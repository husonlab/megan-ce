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
package megan.alignment;

import jloda.util.*;
import megan.algorithms.ActiveMatches;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.Lane;
import megan.core.Document;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.main.MeganProperties;
import megan.util.BlastParsingUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * create an alignment from BLAST matches
 * Daniel Huson, 8.2011
 */
public class Blast2Alignment {
    private final Document doc;

    private String classificationName;
    private String className;

    private int totalNumberOfReads = 0;

    private final Map<String, List<byte[][]>> reference2ReadMatchPairs = new HashMap<>();
    // list of triplets, readName, readSequence, match as text

    final public static String BLASTX = "BlastX";
    final public static String BLASTP = "BlastP";
    final public static String BLASTN = "BlastN";
    final public static String UNKNOWN = "Unknown";

    private String blastType = UNKNOWN;

    /**
     * create an aligner for
     *
     * @param doc
     * @throws java.io.IOException
     * @throws jloda.util.CanceledException
     */
    public Blast2Alignment(Document doc) {
        this.doc = doc;
    }

    public String getClassificationName() {
        return classificationName;
    }

    public String getClassName() {
        return className;
    }

    /**
     * loads data for aligning. This also determines the type of blast data
     *
     * @param classificationName
     * @param classIds
     * @throws java.io.IOException
     * @throws jloda.util.CanceledException
     */
    public void loadData(String classificationName, Set<Integer> classIds, String name, ProgressListener progressListener) throws IOException, CanceledException {
        this.classificationName = classificationName;
        this.className = name;

        totalNumberOfReads = 0;
        int totalReadsUsed = 0;

        reference2ReadMatchPairs.clear();
        setBlastType(UNKNOWN);
        boolean blastFormatUnknown = true;
        boolean warnedUnknownBlastFormatEncountered = false;

        final Set<String> matchesSeenForGivenRead = new HashSet<>();
        progressListener.setTasks("Alignment viewer", "Collecting data");

        System.err.println("Collecting data...");
        if (!doc.getMeganFile().hasDataConnector())
            throw new IOException("Alignment requires archive");

        final Map<String, Set<Long>> reference2seen = new HashMap<>(100000);
        int count = 0;
        boolean seenActiveMatch = false;
        try (IReadBlockIterator it = doc.getConnector().getReadsIteratorForListOfClassIds(classificationName, classIds, doc.getMinScore(), doc.getMaxExpected(), true, true)) {
            progressListener.setMaximum(it.getMaximumProgress());
            progressListener.setProgress(0);

            while (it.hasNext()) // iterate over all reads
            {
                final BitSet activeMatches = new BitSet();
                final IReadBlock readBlock = it.next();
                totalNumberOfReads++;
                count++;

                boolean readUsed = false;

                ActiveMatches.compute(doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, classificationName, activeMatches);

                if (activeMatches.cardinality() > 0) {
                    final String readHeader = Basic.swallowLeadingGreaterSign(readBlock.getReadHeader().substring(1)).replaceAll("[\r\n]", "").trim();
                    final String readSequence;
                    {
                        final String sequence = readBlock.getReadSequence();
                        if (sequence == null)
                            throw new IOException("Can't display alignments, reads sequences appear to be missing from RMA file");
                        readSequence = sequence.replaceAll("[\t\r\n ]", "");
                    }

                    for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                        IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                        if (matchBlock.getText() == null) {
                            System.err.println("Error: Match text: null");
                            continue;
                        }
                        if (doc.isUseIdentityFilter() && matchBlock.getPercentIdentity() > 0 && matchBlock.getPercentIdentity() < 97)
                            continue; // keep only high-identity alignments

                        if (activeMatches.get(i)) {
                            final String matchText = BlastParsingUtils.removeReferenceHeaderFromBlastMatch(BlastParsingUtils.truncateBeforeSecondOccurrence(matchBlock.getText(), "Score ="));
                            //  todo: not sure what the following was supposed to do, but it broke the code: .replaceAll("[\t\r\n ]+", " ");

                            String key = Basic.getFirstLine(matchBlock.getText());
                            // if this is an augmented DNA reference, remove everything from |pos| onward

                            //  System.err.println("key:  "+key);
                            //  System.err.println("text: "+matchText);

                            if (blastFormatUnknown) {
                                setBlastType(BlastParsingUtils.guessBlastType(matchText));
                                if (getBlastType().equals(UNKNOWN)) {
                                    if (!warnedUnknownBlastFormatEncountered) {
                                        System.err.println("Error: Unknown BLAST format encountered");
                                        warnedUnknownBlastFormatEncountered = true;
                                    }
                                    continue;
                                } else
                                    blastFormatUnknown = false;
                            }
                            Set<Long> seen = reference2seen.computeIfAbsent(key, k -> new HashSet<>(10000));
                            final long uid = readBlock.getUId();
                            if (!seen.contains(uid)) // this ensures that any given reference only contains one copy of a read
                            {
                                if (uid != 0)
                                    seen.add(uid);
                                if (!matchesSeenForGivenRead.contains(key)) {
                                    matchesSeenForGivenRead.add(key);

                                    List<byte[][]> pairs = reference2ReadMatchPairs.computeIfAbsent(key, k -> new LinkedList<>());
                                    pairs.add(new byte[][]{readHeader.getBytes(), readSequence.getBytes(), matchText.getBytes()});
                                    readUsed = true;
                                }
                            }
                        }
                    }

                    if (readUsed) {
                        totalReadsUsed++;
                    }
                    matchesSeenForGivenRead.clear();
                    if (!seenActiveMatch && activeMatches.cardinality() > 0)
                        seenActiveMatch = true;
                }
                if ((count % 100) == 0) {
                    progressListener.setSubtask("Collecting data (" + count + " reads processed)");
                    progressListener.setProgress(count);
                }
            }
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED EXECUTE, dataset may be incomplete");
        } finally {
            reference2seen.clear();
        }
        if (!seenActiveMatch)
            throw new IOException("No active matches found");


        final int minReads = ProgramProperties.get(MeganProperties.MININUM_READS_IN_ALIGNMENT, 10);
        if (minReads > 1) {

            boolean hasMinReads = false;
            for (List<byte[][]> list : reference2ReadMatchPairs.values()) {
                if (list.size() >= minReads) {
                    hasMinReads = true;
                    break;
                }
            }
            if (hasMinReads) {
                System.err.print("Removing all alignments with less than " + minReads + " reads: ");
                List<String> toDelete = new LinkedList<>();
                for (String reference : reference2ReadMatchPairs.keySet()) {
                    if (reference2ReadMatchPairs.get(reference).size() < minReads)
                        toDelete.add(reference);
                }
                reference2ReadMatchPairs.keySet().removeAll(toDelete);
                System.err.println(toDelete.size());
            }
        }

        System.err.println("Reads total: " + totalNumberOfReads);
        System.err.println("Reads used:  " + totalReadsUsed);
        System.err.println("References:  " + reference2ReadMatchPairs.keySet().size());

        if (getBlastType().equals(UNKNOWN))
            throw new IOException("Couldn't determine BLAST flavor. Aligner requires BLASTX, BLASTP or BLASTN matches");
    }

    /**
     * load some existing data. Used by alignment exporter
     *
     * @param classificationName
     * @param classId
     * @param name
     * @param readMatchPairs
     */
    public void loadData(String classificationName, Integer classId, String name, String key, List<Pair<IReadBlock, IMatchBlock>> readMatchPairs) {
        this.classificationName = classificationName;
        this.className = name;

        reference2ReadMatchPairs.clear();
        final List<byte[][]> newList = new ArrayList<>(readMatchPairs.size());
        for (Pair<IReadBlock, IMatchBlock> pair : readMatchPairs) {
            final IReadBlock readBlock = pair.getFirst();
            final IMatchBlock matchBlock = pair.getSecond();
            final String readSequence = readBlock.getReadSequence().replaceAll("[\t\r\n ]", "");
            final String readName = readBlock.getReadHeader();
            final String matchText = BlastParsingUtils.removeReferenceHeaderFromBlastMatch(BlastParsingUtils.truncateBeforeSecondOccurrence(matchBlock.getText(), "Score ="));
            newList.add(new byte[][]{readName.getBytes(), readSequence.getBytes(), matchText.getBytes()});

        }
        reference2ReadMatchPairs.put(key, newList);

        totalNumberOfReads = readMatchPairs.size();
    }

    private String getBlastType() {
        return blastType;
    }

    private void setBlastType(String blastType) {
        this.blastType = blastType;
    }

    /**
     * gets the number of reads for a given reference string
     *
     * @return number of reads
     */
    public int getReference2Count(String matchRefLine) {
        return reference2ReadMatchPairs.get(matchRefLine).size();
    }

    /**
     * gets the collection of references
     *
     * @return references
     */
    public Collection<String> getReferences() {
        return reference2ReadMatchPairs.keySet();
    }

    /**
     * returns the total number of reads
     *
     * @return
     */
    public int getTotalNumberOfReads() {
        return totalNumberOfReads;
    }

    /**
     * builds an alignment for the given reference string
     *
     * @param matchRefLine
     * @param alignment
     */
    public void makeAlignment(String matchRefLine, Alignment alignment, boolean showInsertions, ProgressListener progressListener) throws IOException, CanceledException {
        alignment.clear();
        alignment.setName(className);

        // set sequence type
        switch (getBlastType()) {
            case BLASTX:
                alignment.setReferenceType(Alignment.PROTEIN);
                alignment.setSequenceType(Alignment.cDNA);
                break;
            case BLASTP:
                alignment.setReferenceType(Alignment.PROTEIN);
                alignment.setSequenceType(Alignment.PROTEIN);
                break;
            case BLASTN:
                alignment.setReferenceType(Alignment.DNA);
                alignment.setSequenceType(Alignment.DNA);
                break;
        }

        int totalReadsIn = 0;
        int totalReadsOut = 0;
        int totalErrors = 0;

        final List<byte[][]> readMatchPairs = reference2ReadMatchPairs.get(matchRefLine);

        progressListener.setMaximum(readMatchPairs.size());
        progressListener.setProgress(0);

        final Single<char[]> referenceSequence = new Single<>();
        final Single<char[]> originalReferenceSequence = new Single<>(); // used in case of BlastX

        final SortedMap<Integer, Collection<Pair<Integer, String>>> pos2Insertions = new TreeMap<>();
        Integer which = 0;

        for (byte[][] readMatchPair : readMatchPairs) {
            String readHeader = Basic.toString(readMatchPair[0]);
            String readSequence = Basic.toString(readMatchPair[1]);
            String matchText = Basic.toString(readMatchPair[2]);
            totalReadsIn++;

            if (getBlastType().equals(UNKNOWN))
                setBlastType(BlastParsingUtils.guessBlastType(matchText));
            // set sequence type
            switch (getBlastType()) {
                case BLASTX:
                    alignment.setReferenceType(Alignment.PROTEIN);
                    alignment.setSequenceType(Alignment.cDNA);
                    break;
                case BLASTP:
                    alignment.setReferenceType(Alignment.PROTEIN);
                    alignment.setSequenceType(Alignment.PROTEIN);
                    break;
                case BLASTN:
                    alignment.setReferenceType(Alignment.DNA);
                    alignment.setSequenceType(Alignment.DNA);
                    break;
            }

            try {
                Collection<Pair<Integer, String>> insertions = new LinkedList<>();

                switch (getBlastType()) {
                    case BLASTX:
                        computeGappedSequenceBlastX(readHeader, readSequence, matchText, insertions, showInsertions, referenceSequence, originalReferenceSequence, alignment);
                        break;
                    case BLASTP:
                        computeGappedSequenceBlastP(readHeader, readSequence, matchText, insertions, showInsertions, referenceSequence, alignment);
                        break;
                    case BLASTN:
                        computeGappedSequenceBlastN(readHeader, readSequence, matchText, insertions, showInsertions, referenceSequence, alignment);
                        break;
                }

                totalReadsOut++;

                for (Pair<Integer, String> insertion : insertions) {
                    Collection<Pair<Integer, String>> list = pos2Insertions.computeIfAbsent(insertion.getFirst(), k -> new LinkedList<>());
                    list.add(new Pair<>(which, insertion.getSecond()));
                }
                which++;
            } catch (Exception ex) {
                Basic.caught(ex);
                System.err.println("Error: " + ex);
                totalErrors++;
            }
            progressListener.incrementProgress();
        }

        if (referenceSequence.get() != null) {
            int originalLength = referenceSequence.get().length;
            int trueLength = referenceSequence.get().length;
            while (trueLength > 0 && referenceSequence.get()[trueLength - 1] == 0) {
                if (alignment.getReferenceType().equals(Alignment.PROTEIN) && trueLength > 2 && referenceSequence.get()[trueLength - 3] != 0)
                    break;
                trueLength--;
            }
            if (trueLength < originalLength) {
                referenceSequence.set(Arrays.copyOf(referenceSequence.get(), trueLength));
                alignment.trimToTrueLength(trueLength);
            }

            for (int i = 0; i < referenceSequence.get().length; i++) {
                if (referenceSequence.get()[i] == 0)
                    referenceSequence.get()[i] = ' ';
            }
            alignment.setReference(matchRefLine, new String(referenceSequence.get()));
            if (originalReferenceSequence.get() != null)
                alignment.setOriginalReference(new String(originalReferenceSequence.get()));

        }

        if (showInsertions && pos2Insertions.size() > 0) {
            addInsertionsToAlignment(pos2Insertions, alignment, progressListener);
        }

        if (totalReadsIn != totalReadsOut) {
            System.err.println("Reads in: " + totalReadsIn);
            System.err.println("Reads out: " + totalReadsOut);
        }
        if (totalErrors > 0)
            System.err.println("Errors: " + totalErrors);
    }

    /**
     * add the given insertions to the alignment
     *
     * @param pos2Insertions
     * @param alignment
     */
    private void addInsertionsToAlignment(SortedMap<Integer, Collection<Pair<Integer, String>>> pos2Insertions, Alignment alignment, ProgressListener progressListener) throws CanceledException {
        // insertions into reference sequence:
        if (alignment.getReference().getLength() > 0) {
            Lane reference = alignment.getReference();
            int offset = 0;
            for (Integer col : pos2Insertions.keySet()) {
                int maxInsertion = 0;
                Collection<Pair<Integer, String>> insertions = pos2Insertions.get(col);
                for (Pair<Integer, String> pair : insertions) {
                    maxInsertion = Math.max(maxInsertion, pair.getSecond().length());
                }
                col += offset;
                if (maxInsertion > 0) {
                    if (col < reference.getLeadingGaps()) {
                        reference.setLeadingGaps(reference.getLeadingGaps() + maxInsertion);
                    } else if (col < reference.getLeadingGaps() + reference.getBlock().length()) {
                        int insertAfter = col - reference.getLeadingGaps();
                        reference.setBlock(reference.getBlock().substring(0, insertAfter + 1) + gaps(maxInsertion) + reference.getBlock().substring(insertAfter + 1));
                    } else if (col > reference.getLeadingGaps() + reference.getBlock().length()) {
                        reference.setTrailingGaps(reference.getTrailingGaps() + maxInsertion);
                    }
                    offset += maxInsertion;
                }
                for (int i = col + 1; i < col + maxInsertion + 1; i++) {
                    alignment.getInsertionsIntoReference().add(i);
                }
                progressListener.checkForCancel();
            }
        }
        // insertions into alignment
        if (alignment.getNumberOfSequences() > 0) {
            for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
                alignment.getLane(row).setBlock(alignment.getLane(row).getBlock().toUpperCase());
            }
            int offset = 0;
            for (Integer col : pos2Insertions.keySet()) {
                int maxInsertion = 0;
                Collection<Pair<Integer, String>> insertions = pos2Insertions.get(col);
                for (Pair<Integer, String> pair : insertions) {
                    maxInsertion = Math.max(maxInsertion, pair.getSecond().length());
                }
                col += offset;
                if (maxInsertion > 0) {
                    Set<Integer> seen = new HashSet<>();
                    for (Pair<Integer, String> pair : insertions) {
                        int row = pair.getFirst();
                        seen.add(row);
                        String insert = pair.getSecond();
                        Lane lane = alignment.getLane(row);
                        if (col < lane.getLeadingGaps()) {
                            lane.setLeadingGaps(lane.getLeadingGaps() + maxInsertion);
                        } else if (col < lane.getLeadingGaps() + lane.getBlock().length()) {
                            int insertAfter = col - lane.getLeadingGaps();
                            lane.setBlock(lane.getBlock().substring(0, insertAfter + 1) + insert.toLowerCase() + gaps(maxInsertion - insert.length()) + lane.getBlock().substring(insertAfter + 1));
                        } else if (col > lane.getLeadingGaps() + lane.getBlock().length()) {
                            lane.setTrailingGaps(lane.getTrailingGaps() + maxInsertion);
                        }
                    }
                    for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
                        if (!seen.contains(row)) {
                            Lane lane = alignment.getLane(row);
                            if (col < lane.getLeadingGaps()) {
                                lane.setLeadingGaps(lane.getLeadingGaps() + maxInsertion);
                            } else if (col < lane.getLeadingGaps() + lane.getBlock().length()) {
                                int insertAfter = col - lane.getLeadingGaps();
                                lane.setBlock(lane.getBlock().substring(0, insertAfter + 1) + gaps(maxInsertion) + lane.getBlock().substring(insertAfter + 1));
                            } else if (col > lane.getLeadingGaps() + lane.getBlock().length()) {
                                lane.setTrailingGaps(lane.getTrailingGaps() + maxInsertion);
                            }
                        }
                    }
                    offset += maxInsertion;
                }
                progressListener.checkForCancel();
            }
        }
    }

    /**
     * get string of n gaps
     *
     * @param n
     * @return string of n gaps
     */
    private String gaps(int n) {
        StringBuilder buf = new StringBuilder();
        for (; n > 0; n--) {
            buf.append('-');
        }
        return buf.toString();
    }

    /**
     * compute the aligned read sequence
     *
     * @param readSequence
     * @param text
     * @param originalReferenceSequence
     * @return aligned read sequence
     * @throws java.io.IOException
     */
    private static void computeGappedSequenceBlastX(String readName, String readSequence, String text, Collection<Pair<Integer, String>> insertions,
                                                    boolean showInsertions, Single<char[]> referenceSequence, Single<char[]> originalReferenceSequence, Alignment alignment) throws IOException {
        int length = Basic.parseInt(BlastParsingUtils.grabNext(text, "Length =", "Length="));
        if (length == 0)
            length = 10000;

        if (referenceSequence.get() == null)
            referenceSequence.set(new char[3 * length]);
        if (originalReferenceSequence.get() == null) {
            originalReferenceSequence.set(new char[length]);
            for (int i = 0; i < length; i++) {
                originalReferenceSequence.get()[i] = '?';
            }
        }

        final int frame = Basic.parseInt(BlastParsingUtils.grabNext(text, "Frame =", "Frame="));

        int startQuery = Basic.parseInt(BlastParsingUtils.grabNext(text, "Query:", "Query"));
        int endQuery = Basic.parseInt(BlastParsingUtils.grabLastInLinePassedScore(text, "Query"));

        if (readSequence == null)
            throw new IOException("Read '" + readName + "': sequence not found");

        if (readSequence.length() < Math.max(startQuery, endQuery)) {
            throw new IOException("Read '" + readName + "': read length too short: " + readSequence.length() + " < " + Math.max(startQuery, endQuery));
        }

        int startSubject = Basic.parseInt(BlastParsingUtils.grabNext(text, "Sbjct:", "Sbjct"));
        int endSubject = Basic.parseInt(BlastParsingUtils.grabLastInLinePassedScore(text, "Sbjct"));

        String queryString = BlastParsingUtils.grabQueryString(text);
        String subjectString = BlastParsingUtils.grabSubjectString(text);

        int p = startSubject;
        for (int i = 0; i < subjectString.length(); i++) {
            if (subjectString.charAt(i) != '-') {
                referenceSequence.get()[3 * (p - 1)] = subjectString.charAt(i);
                originalReferenceSequence.get()[p - 1] = subjectString.charAt(i);
                p++;
            }
        }

        if (frame < 0) {
            readName += " (rev)";
            startQuery = readSequence.length() - startQuery + 1;
            endQuery = readSequence.length() - endQuery + 1;
            readSequence = SequenceUtils.getReverseComplement(readSequence);
        }

        int pos = startQuery - 1; // position in actual sequence

        int alignPos = 3 * (startSubject - 1); // pos in DNA alignment

        //System.err.println("read-length: "+readSequence.length());
        //System.err.println("query-length: "+queryString.length());
        //System.err.println("sbjct-length: "+subjectString.length());

        Pair<Integer, String> insertion = null;

        StringWriter w = new StringWriter();

        for (int mPos = 0; mPos < queryString.length(); mPos++) {
            if (queryString.charAt(mPos) == '-') {
                if (insertion != null) {
                    insertion = null;
                }
                w.write("---");
                alignPos += 3;
            } else if (subjectString.charAt(mPos) == '-') {
                if (showInsertions) {
                    if (insertion == null) {
                        insertion = new Pair<>(alignPos - 1, readSequence.substring(pos, pos + 3));
                        insertions.add(insertion);
                    } else {
                        insertion.setSecond(insertion.getSecond() + readSequence.substring(pos, pos + 3));
                    }
                }
                pos += 3;
                alignPos += 3;
            } else {
                if (insertion != null) {
                    insertion = null;
                }
                if (pos < 0) {
                    throw new IOException("pos too small: " + pos);
                }
                if (pos >= readSequence.length()) {
                    throw new IOException("pos overrun end of read: " + pos + " >= " + readSequence.length());
                } else {
                    w.write(readSequence.charAt(pos++));
                    w.write(readSequence.charAt(pos++));
                    w.write(readSequence.charAt(pos++));
                    alignPos += 3;
                }
            }
        }
        /*
        if (pos != endQuery) {
            System.err.println("Pos not exhausted: "+pos+" != "+endQuery);
        }
        */
        final String block = w.toString();
        final int leadingGaps = 3 * (startSubject - 1);
        final int trailingGaps = 3 * (length - endSubject);
        final String unalignedPrefix = readSequence.substring(0, startQuery);
        final String unalignedSuffix = readSequence.substring(endQuery);
        alignment.addSequence(readName, text, null, unalignedPrefix, leadingGaps, block, trailingGaps, unalignedSuffix);
    }

    /**
     * compute the aligned read sequence
     *
     * @param readSequence
     * @param text
     * @return aligned read sequence
     * @throws java.io.IOException
     */
    private static void computeGappedSequenceBlastP(String readName, String readSequence, String text, Collection<Pair<Integer, String>> insertions, boolean showInsertions, Single<char[]> referenceSequence, Alignment alignment) throws IOException {

        int length = Basic.parseInt(BlastParsingUtils.grabNext(text, "Length =", "Length="));
        if (length == 0)
            length = 10000;

        if (referenceSequence.get() == null)
            referenceSequence.set(new char[length]);

        int startQuery = Basic.parseInt(BlastParsingUtils.grabNext(text, "Query:", "Query"));
        int endQuery = Basic.parseInt(BlastParsingUtils.grabLastInLinePassedScore(text, "Query"));

        if (readSequence == null)
            throw new IOException("Read '" + readName + "': sequence not found");

        if (readSequence.length() < Math.max(startQuery, endQuery)) {
            throw new IOException("Read '" + readName + "': read length too short: " + readSequence.length() + " < " + Math.max(startQuery, endQuery));
        }

        int startSubject = Basic.parseInt(BlastParsingUtils.grabNext(text, "Sbjct:", "Sbjct"));
        int endSubject = Basic.parseInt(BlastParsingUtils.grabLastInLinePassedScore(text, "Sbjct"));

        String queryString = BlastParsingUtils.grabQueryString(text);

        String subjectString = BlastParsingUtils.grabSubjectString(text);

        int p = startSubject;
        for (int i = 0; i < subjectString.length(); i++) {
            if (subjectString.charAt(i) != '-') {
                referenceSequence.get()[p - 1] = subjectString.charAt(i);
                p++;
            }
        }

        int alignPos = (startSubject - 1); // pos in DNA alignment

        //System.err.println("read-length: "+readSequence.length());
        //System.err.println("query-length: "+queryString.length());
        //System.err.println("sbjct-length: "+subjectString.length());

        Pair<Integer, String> insertion = null;

        final StringWriter w = new StringWriter();
        for (int mPos = 0; mPos < queryString.length(); mPos++) {
            char ch = queryString.charAt(mPos);
            if (ch == '-') {
                if (insertion != null) {
                    insertion = null;
                }
                w.write("-");
                alignPos += 1;
            } else if (subjectString.charAt(mPos) == '-') {
                if (showInsertions) {
                    if (insertion == null) {
                        insertion = new Pair<>(alignPos - 1, queryString.substring(mPos, mPos + 1));
                        insertions.add(insertion);
                    } else {
                        insertion.setSecond(insertion.getSecond() + readSequence.substring(mPos, mPos + 1));
                    }
                }
                alignPos += 1;
            } else {
                if (insertion != null) {
                    insertion = null;
                }
                w.write(ch);
                alignPos += 1;
            }
        }
        String block = w.toString();
        int leadingGaps = startSubject - 1;
        int trailingGaps = length - endSubject;
        String unalignedPrefix = readSequence.substring(0, startQuery);
        String unalignedSuffix = readSequence.substring(endQuery);
        alignment.addSequence(readName, text, null, unalignedPrefix, leadingGaps, block, trailingGaps, unalignedSuffix);
    }

    /**
     * compute the aligned read sequence
     *
     * @param readSequence
     * @param text
     * @return aligned read sequence
     * @throws java.io.IOException
     */
    private static void computeGappedSequenceBlastN(String readName, String readSequence, String text, Collection<Pair<Integer, String>> insertions,
                                                    boolean showInsertions, Single<char[]> referenceSequence, Alignment alignment) throws IOException {
        boolean hasExactLength;
        Integer length = Basic.parseInt(BlastParsingUtils.grabNext(text, "Length =", "Length="));
        if (length > 0) {
            hasExactLength = true;
        } else {
            hasExactLength = false;
            length = Basic.parseInt(BlastParsingUtils.grabNext(text, "Length >=", "Length>="));

            if (referenceSequence.get() != null && referenceSequence.get().length < length) { // need to resize the reference sequence
                char[] newRef = new char[length + 1];
                System.arraycopy(referenceSequence.get(), 0, newRef, 0, referenceSequence.get().length);
                referenceSequence.set(newRef);
            }
        }

        String[] strand;
        String tmpString = BlastParsingUtils.grabNext(text, "Strand =", "Strand=");
        if (tmpString != null && tmpString.contains("/")) {
            int pos = tmpString.indexOf("/");
            strand = new String[]{tmpString.substring(0, pos), "/", tmpString.substring(pos + 1)};
        } else
            strand = BlastParsingUtils.grabNext3(text, "Strand =", "Strand=");

        if (referenceSequence.get() == null)
            referenceSequence.set(new char[length + 10000]);

        int startQuery = Basic.parseInt(BlastParsingUtils.grabNext(text, "Query:", "Query"));
        int endQuery = Basic.parseInt(BlastParsingUtils.grabLastInLinePassedScore(text, "Query"));

        if (readSequence == null)
            throw new IOException("Read '" + readName + "': sequence not found");

        if (readSequence.length() < Math.max(startQuery, endQuery)) {
            throw new IOException("Read '" + readName + "': read length too short: " + readSequence.length() + " < " + Math.max(startQuery, endQuery));
        }

        int startSubject = BlastParsingUtils.getStartSubject(text);
        int endSubject = BlastParsingUtils.getEndSubject(text);

        String queryString = BlastParsingUtils.grabQueryString(text);
        String subjectString = BlastParsingUtils.grabSubjectString(text);

        if (strand != null && strand[0].equalsIgnoreCase("Minus") && strand[2].equalsIgnoreCase("Minus")) {
            throw new IOException("Can't parse matches with Strand = Minus / Minus");
        }

        if (strand != null && strand[0].equalsIgnoreCase("Minus")) {
            int tmp = Math.max(startQuery, endQuery);
            startQuery = Math.min(startQuery, endQuery);
            endQuery = tmp;
            //queryString = SequenceUtils.getReverseComplement(queryString);
            // subject string has positive strand
            //subjectString = SequenceUtils.getReverseComplement(subjectString);
            readName += " (-/+)";
        }

        if (strand != null && strand[2].equalsIgnoreCase("Minus")) {
            int tmp = Math.max(startSubject, endSubject);
            startSubject = Math.min(startSubject, endSubject);
            endSubject = tmp;
            queryString = SequenceUtils.getReverseComplement(queryString);
            subjectString = SequenceUtils.getReverseComplement(subjectString);
            if (!strand[0].equalsIgnoreCase("Minus"))
                readName += " (+/-)";
        }

        int p = startSubject;
        for (int i = 0; i < subjectString.length(); i++) {
            if (subjectString.charAt(i) != '-') {
                /*
                char previousCh=referenceSequence.get()[p - 1];
                char newCh=subjectString.charAt(i);
                if(p==792)
                System.err.println("Setting subj["+(p-1)+"]="+newCh+" previous="+previousCh);
                */
                // todo: Just reactivated this, might cause problems:
                referenceSequence.get()[p - 1] = subjectString.charAt(i);
                p++;
            }
        }

        int alignPos = (startSubject - 1); // pos in DNA alignment

        //System.err.println("read-length: "+readSequence.length());
        //System.err.println("query-length: "+queryString.length());
        //System.err.println("sbjct-length: "+subjectString.length());

        StringWriter w = new StringWriter();

        Pair<Integer, String> insertion = null;

        for (int mPos = 0; mPos < queryString.length(); mPos++) {
            char ch = queryString.charAt(mPos);
            if (ch == '-') {
                if (insertion != null) {
                    insertion = null;
                }
                w.write("-");
                alignPos += 1;
            } else if (subjectString.charAt(mPos) == '-') {
                if (showInsertions) {
                    if (insertion == null) {
                        insertion = new Pair<>(alignPos - 1, queryString.substring(mPos, mPos + 1));
                        insertions.add(insertion);
                    } else {
                        insertion.setSecond(insertion.getSecond() + queryString.substring(mPos, mPos + 1));
                    }
                }
                alignPos += 1;
            } else {
                if (insertion != null) {
                    insertion = null;
                }
                {
                    w.write(ch);
                    alignPos += 1;
                }
            }
        }
        String block = w.toString();
        int leadingGaps = startSubject - 1;
        int trailingGaps = length - endSubject;
        String unalignedPrefix = readSequence.substring(0, startQuery - 1);
        String unalignedSuffix = readSequence.substring(endQuery);
        alignment.addSequence(readName, text, null, unalignedPrefix, leadingGaps, block, trailingGaps, unalignedSuffix);

        if (!hasExactLength) {
            boolean hasLengthDifferences = false;
            int longest = 0;
            for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
                int aLength = alignment.getLane(row).getLength();
                if (aLength > longest) {
                    if (longest > 0)
                        hasLengthDifferences = true;
                    longest = aLength;
                }
            }
            if (hasLengthDifferences) {
                for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
                    Lane lane = alignment.getLane(row);
                    if (lane.getLength() < longest) {
                        lane.setTrailingGaps(lane.getTrailingGaps() + (longest - lane.getLength()));
                    }
                }
            }
        }
    }

}
