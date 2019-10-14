/*
 *  Copyright (C) 2019 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.rma6;

import jloda.util.*;
import megan.accessiondb.AccessAccessionMappingDatabase;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdParser;
import megan.core.Document;
import megan.core.MeganFile;
import megan.core.SyncArchiveAndDataTable;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.io.InputOutputReaderWriter;
import megan.parsers.blast.BlastFileFormat;
import megan.parsers.blast.ISAMIterator;
import megan.parsers.blast.IteratorManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a new RMA6 file by parsing a blast file
 * <p>
 * Daniel Huson, 6.2015
 */
public class RMA6FromBlastCreator {
    private final BlastFileFormat format;
    private final BlastMode blastMode;
    private final String[] blastFiles;
    private final String[] readsFiles;
    private final String rma6File;
    private final Document doc;

    private final int maxMatchesPerRead;
    private final int taxonMapperIndex;
    private final IdParser[] parsers;
    private final String[] cNames;
    private final boolean pairedReads;
    private final boolean longReads;
    private final int pairedReadSuffixLength;

    private final RMA6FileCreator rma6FileCreator;

    /**
     * construct a new creator to create an RMA6 file from a set of BLAST files
     *
     * @param format
     * @param blastMode
     * @param blastFiles
     * @param readsFiles
     * @param maxMatchesPerRead
     * @param doc
     * @throws IOException
     */
    public RMA6FromBlastCreator(String creator, BlastFileFormat format, BlastMode blastMode, String[] blastFiles, String[] readsFiles, String rma6File, boolean useCompression,
                                Document doc, int maxMatchesPerRead) throws IOException {
        this.format = format;
        this.blastMode = blastMode;
        this.blastFiles = blastFiles;
        this.readsFiles = readsFiles;
        this.rma6File = rma6File;
        this.maxMatchesPerRead = maxMatchesPerRead;
        this.doc = doc;
        doc.getMeganFile().setFile(rma6File, MeganFile.Type.RMA6_FILE);

        if (doc.getActiveViewers().size() > 0)
            cNames = doc.getActiveViewers().toArray(new String[0]);
        else
            cNames = new String[]{Classification.Taxonomy};

        this.parsers = new IdParser[cNames.length];
        int taxonMapperIndex = -1;

        System.err.println("Classifications: " + Basic.toString(cNames, ", "));

        for (int i = 0; i < cNames.length; i++) {
            parsers[i] = ClassificationManager.get(cNames[i], true).getIdMapper().createIdParser();
            if (cNames[i].equals(Classification.Taxonomy)) {
                taxonMapperIndex = i;
            }
        }
        if (taxonMapperIndex == -1)
            throw new IOException("Internal error: taxonMapperIndex=-1");
        else
            this.taxonMapperIndex = taxonMapperIndex;

        this.longReads = doc.isLongReads();
        this.pairedReads = doc.isPairedReads();
        this.pairedReadSuffixLength = doc.getPairedReadSuffixLength();

        // setup the file creator and write the header:
        rma6FileCreator = new RMA6FileCreator(rma6File, useCompression);

        final String[] matchClassificationNames = new String[parsers.length];
        for (int i = 0; i < parsers.length; i++)
            matchClassificationNames[i] = parsers[i].getCName();

        rma6FileCreator.writeHeader(creator, blastMode, matchClassificationNames, doc.isPairedReads());
    }

    /**
     * parse the files
     *
     * @param progress
     * @throws IOException
     * @throws CanceledException
     */
    public void parseFiles(final ProgressListener progress) throws IOException, CanceledException, SQLException {
        progress.setTasks("Generating RMA6 file", "Parsing matches");

        final HashMap<String, Long> read2PairedReadLocation;
        if (pairedReads)
            read2PairedReadLocation = new HashMap<>(1000000);
        else
            read2PairedReadLocation = null;

        final byte[] queryName = new byte[100000];
        final Single<byte[]> fastAText = new Single<>(new byte[1000]);
        MatchLineRMA6[] matchLineRMA6s = new MatchLineRMA6[maxMatchesPerRead];
        for (int i = 0; i < matchLineRMA6s.length; i++) {
            matchLineRMA6s[i] = new MatchLineRMA6(cNames.length, taxonMapperIndex);
        }

        int[][] match2classification2id = new int[maxMatchesPerRead][cNames.length];

        rma6FileCreator.startAddingQueries();

        long totalNumberOfReads = 0;
        long totalNumberOfMatches = 0;

        // setup use of accession mapping database, if provided
        final AccessAccessionMappingDatabase accessAccessionMappingDatabase;
        final int[] mapClassificationId2DatabaseRank;
        if (ClassificationManager.canUseMeganMapDBFile()) {
            System.err.println("Annotating RMA6 file using FAST mode (accession database and first accession per line)");
            accessAccessionMappingDatabase = new AccessAccessionMappingDatabase(ClassificationManager.getMeganMapDBFile());
            mapClassificationId2DatabaseRank = accessAccessionMappingDatabase.setupMapClassificationId2DatabaseRank(cNames);
        } else {
            System.err.println("Annotating RMA6 file using EXTENDED mode");
            accessAccessionMappingDatabase = null;
            mapClassificationId2DatabaseRank = null;
        }

        try {
            for (int fileNumber = 0; fileNumber < blastFiles.length; fileNumber++) {
            int missingReadWarnings = 0;
            final String blastFile = blastFiles[fileNumber];
            progress.setTasks("Parsing file", Basic.getFileNameWithoutPath(blastFile));
            System.err.println("Parsing file: " + blastFile);

            final ISAMIterator iterator = IteratorManager.getIterator(blastFile, format, blastMode, maxMatchesPerRead, longReads);

            progress.setProgress(0);
            progress.setMaximum(iterator.getMaximumProgress());

            final FileLineBytesIterator fastaIterator;
            final boolean isFasta;
            if (readsFiles != null && readsFiles.length > fileNumber && Basic.fileExistsAndIsNonEmpty(readsFiles[fileNumber])) {
                fastaIterator = new FileLineBytesIterator(readsFiles[fileNumber]);
                isFasta = (fastaIterator.peekNextByte() == '>');
                if (!isFasta && (fastaIterator.peekNextByte() != '@'))
                    throw new IOException("Cannot determine type of reads file (doesn't start with '>' or '@': " + readsFiles[fileNumber]);
            } else {
                fastaIterator = null;
                isFasta = false; // don't care, won't use
            }

                // MAIN LOOP:
                while (iterator.hasNext()) {
                    totalNumberOfReads++;
                    final int numberOfMatches = iterator.next();
                    totalNumberOfMatches += numberOfMatches;
                    final byte[] matchesText = iterator.getMatchesText(); // get matches as '\n' separated strings
                    final int matchesTextLength = iterator.getMatchesTextLength();
                    final int queryNameLength = Basic.getFirstWord(matchesText, queryName);

                    //System.err.println("Got: "+Basic.toString(matchesText,Math.min(100,matchesTextLength)));

                    Long mateLocation = null;

                    if (pairedReads) {
                        final String strippedName = Basic.toString(queryName, 0, queryNameLength - pairedReadSuffixLength);
                        mateLocation = read2PairedReadLocation.get(strippedName);
                        if (mateLocation == null) {
                            read2PairedReadLocation.put(strippedName, rma6FileCreator.getPosition());
                        } else {
                            read2PairedReadLocation.remove(strippedName);
                        }
                    }

                    byte[] queryText = null;
                    int queryTextLength = 0;

                    if (fastaIterator != null) {
                        if (Utilities.findQuery(queryName, queryNameLength, fastaIterator, isFasta)) {
                            queryTextLength = Utilities.getFastAText(fastaIterator, isFasta, fastAText);
                            queryText = fastAText.get();

                        } else {
                            if (missingReadWarnings++ < 50)
                                System.err.println("WARNING: Failed to find read '" + Basic.toString(queryName, 0, queryNameLength) + "' in file: " + readsFiles[fileNumber]);
                            if (missingReadWarnings == 50)
                                System.err.println("No further 'failed to find read' warnings...");
                        }
                    }
                    if (iterator.getQueryText() != null) {
                        queryText = iterator.getQueryText();
                        queryTextLength = iterator.getQueryText().length;

                    }
                    if (queryText == null) {
                        queryText = queryName;
                        queryTextLength = queryNameLength;
                    }

                    // for each match, write its taxonId and all its functional ids:

                    if (mapClassificationId2DatabaseRank != null) { // use mapping database
                        int offset = 0;
                        final String[] queries = new String[numberOfMatches];
                        for (int matchCount = 0; matchCount < numberOfMatches; matchCount++) {
                            queries[matchCount] = getFirstWord(Utilities.getToken(2, matchesText, offset));

                            if (matchCount == matchLineRMA6s.length) { // double the array...
                                MatchLineRMA6[] tmp = new MatchLineRMA6[2 * numberOfMatches];
                                System.arraycopy(matchLineRMA6s, 0, tmp, 0, matchCount);
                                matchLineRMA6s = tmp;
                                for (int i = matchCount; i < matchLineRMA6s.length; i++)
                                    matchLineRMA6s[i] = new MatchLineRMA6(cNames.length, taxonMapperIndex);
                            }
                            if (matchCount == match2classification2id.length) {
                                int[][] tmp = new int[2 * numberOfMatches][cNames.length];
                                System.arraycopy(match2classification2id, 0, tmp, 0, matchCount);
                                match2classification2id = tmp;
                            }

                            final MatchLineRMA6 matchLineRMA6 = matchLineRMA6s[matchCount];
                            matchLineRMA6.parse(matchesText, offset);
                            offset = Utilities.nextNewLine(matchesText, offset) + 1;
                        }
                        final Map<String, int[]> query2ids = accessAccessionMappingDatabase.getValues(queries, queries.length);
                        for (int matchCount = 0; matchCount < queries.length; matchCount++) {
                            final int[] ids = query2ids.get(queries[matchCount]);
                            if (ids != null) {
                                for (int c = 0; c < cNames.length; c++) {
                                    final int dbRank = mapClassificationId2DatabaseRank[c];
                                    if (dbRank < ids.length) {
                                        final int id = ids[dbRank];
                                        match2classification2id[matchCount][c] = id;
                                        matchLineRMA6s[matchCount].setFId(c, id);
                                    }
                                }
                            }
                        }
                    } else { // use mapping files
                        int offset = 0;
                        for (int matchCount = 0; matchCount < numberOfMatches; matchCount++) {
                            final String refName = Utilities.getToken(2, matchesText, offset);

                            if (matchCount == matchLineRMA6s.length) { // double the array...
                                MatchLineRMA6[] tmp = new MatchLineRMA6[2 * numberOfMatches];
                                System.arraycopy(matchLineRMA6s, 0, tmp, 0, matchCount);
                                matchLineRMA6s = tmp;
                                for (int i = matchCount; i < matchLineRMA6s.length; i++)
                                    matchLineRMA6s[i] = new MatchLineRMA6(cNames.length, taxonMapperIndex);
                            }
                            if (matchCount == match2classification2id.length) {
                                int[][] tmp = new int[2 * numberOfMatches][cNames.length];
                                System.arraycopy(match2classification2id, 0, tmp, 0, matchCount);
                                match2classification2id = tmp;
                            }

                            final MatchLineRMA6 matchLineRMA6 = matchLineRMA6s[matchCount];
                            matchLineRMA6.parse(matchesText, offset);
                            for (int i = 0; i < parsers.length; i++) {
                                final int id = parsers[i].getIdFromHeaderLine(refName);

                                match2classification2id[matchCount][i] = id;
                                matchLineRMA6.setFId(i, id);
                            }
                            offset = Utilities.nextNewLine(matchesText, offset) + 1;
                        }
                    }

                    rma6FileCreator.addQuery(queryText, queryTextLength, numberOfMatches, matchesText, matchesTextLength, match2classification2id, mateLocation != null ? mateLocation : 0);
                    progress.setProgress(iterator.getProgress());
                } // end of iterator
            } // end of files
        }
        finally{
            if(accessAccessionMappingDatabase!=null)
                accessAccessionMappingDatabase.close();
        }

        rma6FileCreator.endAddingQueries();

        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();

        System.err.println(String.format("Total reads:  %,16d", totalNumberOfReads));
        System.err.println(String.format("Alignments:    %,15d", totalNumberOfMatches));

        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();

        // nothing to write
        rma6FileCreator.writeClassifications(null, null, null);

        rma6FileCreator.writeAuxBlocks(null); // zero aux blocks

        rma6FileCreator.close();

        if (pairedReads) { // update paired reads info
            long count = 0;
            if (progress instanceof ProgressPercentage)
                ((ProgressPercentage) progress).reportTaskCompleted();
            try (InputOutputReaderWriter raf = new InputOutputReaderWriter(rma6File, "rw");
                 IReadBlockIterator it = (new RMA6Connector(rma6File)).getAllReadsIterator(0, 10, false, false)) {
                progress.setSubtask("Linking paired reads");
                progress.setProgress(0);
                progress.setProgress(it.getMaximumProgress());

                while (it.hasNext()) {
                    final IReadBlock readBlock = it.next();
                    if (readBlock.getMateUId() > 0) {
                        if (readBlock.getMateUId() > readBlock.getUId())
                            throw new IOException("Mate uid=" + readBlock.getMateUId() + ": too big");
                        raf.seek(readBlock.getMateUId()); // if defined, mate UID is first number in record, so this is ok
                        raf.writeLong(readBlock.getUId());
                        count++;
                    }
                    progress.setProgress(it.getProgress());
                }
                System.err.println(String.format("Number of pairs:%,14d", count));
            }
        }

        // we need to run data processor to perform classification
        doc.processReadHits();

        // update and then save auxiliary data:
        final String sampleName = Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(rma6File), "");
        SyncArchiveAndDataTable.syncRecomputedArchive2Summary(doc.getReadAssignmentMode(), sampleName, "LCA", doc.getBlastMode(), doc.getParameterString(), new RMA6Connector(rma6File), doc.getDataTable(), 0);
        doc.saveAuxiliaryData();
    }

    /**
     * set contaminants
     *
     * @param contaminantTaxonIdsString
     */
    public void setContaminants(String contaminantTaxonIdsString) {
        doc.getDataTable().setContaminants(contaminantTaxonIdsString);
    }

    private static String getFirstWord(String string) {
        int a=0;
        while(a<string.length() && (string.charAt(a)=='>' || Character.isWhitespace(string.charAt(a)))) {
            a++;
        }
        int b=a;
        while(b<string.length() && (string.charAt(b)=='_' || Character.isLetterOrDigit(string.charAt(b)))) {
            b++;
        }
        return   string.substring(a,b);
    }
}
