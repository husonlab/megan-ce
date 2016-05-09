/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.tools;

import jloda.util.*;
import megan.parsers.fasta.FastAFileIterator;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * merge read to reference alignments based on a multiple alignment of the references
 */
public class MergeAlignments {
    public enum OutputFormat {MSA, BlastNText, BlastXText}

    /**
     * converts the file
     *
     * @param args
     * @throws UsageException
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0 && System.getProperty("user.name").equals("huson")) {
            args = new String[]{
                    "-ref", "/Users/huson/data/michael/test/dna/references-aligned.fasta",
                    "-rea", "/Users/huson/data/michael/test/dna/reads-aligned.fasta"
            };
            args = new String[]{
                    "-ref", "/Users/huson/data/michael/test/references-aligned-sg.fasta",
                    "-rea", "/Users/huson/data/michael/test/reads-aligned-sg.fasta", "-d2p", "-f", "BlastNText"
            };
            args = new String[]{
                    "-ref", "/Users/huson/data/michael/adam/try-alignments/880_rpoB_sequences_min_1000_AAs.mafft",
                    "-rea", "/Users/huson/data/michael/adam/try-alignments/use", "-d2p",
                    "-o", "/Users/huson/data/michael/adam/try-alignments/out.blastn", "-f", "BlastNText", "-n", "880_rpoB_sequences_min_1000_AAs|kegg|3043"
            };
            args = new String[]{
                    "-ref", "/Users/huson/data/michael/test/references-aligned.fasta",
                    "-rea", "/Users/huson/data/michael/test/reads-aligned.fasta", "-d2p", "-f", "BlastNText"
            };
            args = new String[]{
                    "-ref", "/Users/huson/data/michael/adam/try-alignments/references-aligned.fasta",
                    "-rea", "/Users/huson/data/michael/adam/try-alignments/use", "-d2p",
                    "-o", "/Users/huson/data/michael/adam/try-alignments/SRR172902-rpoB.blastn", "-f", "BlastNText", "-n", "880_rpoB_sequences_min_1000_AAs|kegg|3043",
                    "-r", "/Users/huson/data/michael/adam/try-alignments/SRR172902-rpoB.fasta"
            };
            args = new String[]{
                    "-ref", "/Users/huson/data/michael/adam/try-alignments/references-top-aligned.fasta",
                    "-rea", "/Users/huson/data/michael/adam/try-alignments/use", "-d2p",
                    "-o", "/Users/huson/data/michael/adam/try-alignments/SRR172902-rpoB.blastn", "-f", "BlastNText", "-n", "references-top|kegg|3043",
                    "-r", "/Users/huson/data/michael/adam/try-alignments/SRR172902-rpoB.fasta"
            };
            args = new String[]{
                    "-ref", "/Users/huson/data/michael/adam/try-alignments/one-aligned.fasta",
                    "-rea", "/Users/huson/data/michael/adam/try-alignments/two-reads-aligned.fasta", "-d2p",
                    "-o", "/Users/huson/data/michael/adam/try-alignments/two.blastn", "-f", "BlastNText", "-n", "references-top|kegg|3043",
            };
            args = new String[]{
                    "-ref", "/Users/huson/data/michael/adam/try-alignments/references-82-aligned.fasta",
                    "-rea", "/Users/huson/data/michael/adam/try-alignments/use82", "-d2p",
                    "-o", "/Users/huson/data/michael/adam/try-alignments/SRR172902-rpoB-82.blastn", "-f", "BlastNText", "-n", "references-top|kegg|3043",
                    "-r", "/Users/huson/data/michael/adam/try-alignments/SRR172902-rpoB.fasta"
            };
        }

        try {
            long start = System.currentTimeMillis();
            (new MergeAlignments()).run(args);
            System.err.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s");
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run the program
     *
     * @param args
     */
    public void run(String[] args) throws Exception {
        final ArgsOptions options = new ArgsOptions(args, this, "Merges reads-to-reference alignments using reference alignment");

        final String referencesAlignmentFile = options.getOptionMandatory("-ref", "refInput", "file containing multiple alignment of references", "");
        String[] readsAlignmentFiles = options.getOption("-rea", "readAlignmentsFile", "Files containing alignments of reads to references", new String[0]);
        String readsFile = options.getOption("-r", "readsFile", "File containing all reads", "");

        final boolean dna2ProteinMode = options.getOption("-d2p", "dna2prot", "Aligned reads are DNA, references are protein", false);
        final String outputFile = options.getOption("-o", "output", "Output file", "");
        final OutputFormat outputFormat = OutputFormat.valueOf(options.getOption("-f", "format", "Output format", OutputFormat.values(), OutputFormat.MSA.toString()));
        final String referenceName = options.getOption("-n", "name", "Name of references", Basic.getFileBaseName(Basic.getFileNameWithoutPath(referencesAlignmentFile)));
        options.done();

        if (outputFormat == OutputFormat.BlastXText)
            throw new IOException("Option '--format BlastXText': not implemented");

        if (outputFormat == OutputFormat.BlastXText && !dna2ProteinMode)
            throw new IOException("Option '--format BlastXText' requires option '--dna2prot'");
        if (outputFormat == OutputFormat.BlastXText && readsFile.length() == 0)
            throw new IOException("Option '--format BlastXText' requires option '--readsFile'");


        if (readsAlignmentFiles.length == 1 && (new File(readsAlignmentFiles[0]).isDirectory())) {
            File[] files = (new File(readsAlignmentFiles[0])).listFiles();
            if (files != null) {
                List<String> names = new LinkedList<>();
                for (File file : files) {
                    if (file.isFile() && file.canRead() && !file.getName().startsWith("."))
                        names.add(file.getPath());
                }
                readsAlignmentFiles = names.toArray(new String[names.size()]);
            }
        }

        final Map<String, String> referencesGlobalAlignment = new HashMap<>();
        final List<String> referenceNames = new LinkedList<>();

        int refGlobalAlignmentLength = -1;

        try (FileInputIterator it = new FileInputIterator(referencesAlignmentFile)) {
            final ProgressPercentage progress = new ProgressPercentage("Processing file: " + referencesAlignmentFile, it.getMaximumProgress());

            String name = null;
            final List<String> sequences = new LinkedList<>();

            while (it.hasNext()) {
                final String aLine = it.next();
                if (aLine.startsWith(">")) {
                    if (name != null) {
                        final String alignedSequence = Basic.toString(sequences, "");
                        if (refGlobalAlignmentLength == -1)
                            refGlobalAlignmentLength = alignedSequence.length();
                        else if (refGlobalAlignmentLength != alignedSequence.length())
                            throw new IOException("Aligned reference sequences have different lengths: " + refGlobalAlignmentLength + " vs " + alignedSequence.length());
                        referencesGlobalAlignment.put(name, alignedSequence);
                    }
                    name = Basic.getWordAfter(">", aLine);
                    if (referencesGlobalAlignment.keySet().contains(name))
                        throw new IOException("Reference name occurs more than once: " + name);
                    referenceNames.add(name);
                    sequences.clear();
                    progress.setProgress(it.getProgress());
                } else
                    sequences.add(aLine.replaceAll("\\s+", ""));
            }
            if (name != null) {
                final String alignedSequence = Basic.toString(sequences, "");
                referencesGlobalAlignment.put(name, alignedSequence);
                if (refGlobalAlignmentLength == -1)
                    refGlobalAlignmentLength = alignedSequence.length();
                else if (refGlobalAlignmentLength != alignedSequence.length())
                    throw new IOException("Aligned reference sequences have different lengths: " + refGlobalAlignmentLength + " vs " + alignedSequence.length());
            }
            progress.close();
            System.err.println("Input: " + referencesGlobalAlignment.size());
        }
        if (referencesGlobalAlignment.size() <= 5) {
            System.err.println("Reference alignment (" + referencesGlobalAlignment.size() + " x " + refGlobalAlignmentLength + "):");
            for (String name : referenceNames) {
                System.err.println(referencesGlobalAlignment.get(name));
            }
        }

        final Map<String, String> readsGlobalAlignment = new HashMap<>();
        final List<String> readNames = new LinkedList<>();

        final int readGlobalAlignmentLength = (dna2ProteinMode ? 3 * refGlobalAlignmentLength : refGlobalAlignmentLength);

        for (String readsAlignmentFile : readsAlignmentFiles) {
            try (FileInputIterator it = new FileInputIterator(readsAlignmentFile)) {
                final ProgressPercentage progress = new ProgressPercentage("Processing file: " + readsAlignmentFile, it.getMaximumProgress());

                String refName = null;
                String refSequence = null;

                String readName = null;
                String readSequence;

                final List<String> sequences = new LinkedList<>();

                while (it.hasNext()) {
                    final String aLine = it.next();

                    if (aLine.startsWith(">")) {
                        final String name = Basic.getWordAfter(">", aLine);
                        final boolean isRef = referencesGlobalAlignment.keySet().contains(name);

                        if (refName == null) {
                            refName = name;
                        } else if (readName == null) {
                            if (refSequence == null)
                                refSequence = Basic.toString(sequences, "");
                            readName = name;
                            readNames.add(readName);
                        } else {
                            readSequence = placeBracketsAroundInsertionsInReadSequence(dna2ProteinMode, refSequence, Basic.toString(sequences, ""));

                            String globallyAlignedReference = referencesGlobalAlignment.get(refName);
                            if (globallyAlignedReference == null)
                                throw new IOException("Reference sequence not found: " + refName);
                            String globallyAligned = computeGlobalReadAlignment(dna2ProteinMode, globallyAlignedReference, readSequence, readGlobalAlignmentLength);
                            if (globallyAligned.length() != readGlobalAlignmentLength)
                                throw new IOException("Read global alignment has wrong length: " + globallyAligned.length() + ", should be: " + readGlobalAlignmentLength);
                            readsGlobalAlignment.put(readName, globallyAligned);

                            if (isRef) {
                                refName = name;
                                refSequence = null;
                                readName = null;
                            } else {
                                readName = name;
                                readNames.add(readName);
                            }
                        }
                        sequences.clear();
                    } else // is sequence
                    {
                        sequences.add(parseAlignedSequence(aLine));
                    }
                    progress.setProgress(it.getProgress());
                }
                // do the last:
                if (readName != null) {
                    readSequence = placeBracketsAroundInsertionsInReadSequence(dna2ProteinMode, refSequence, Basic.toString(sequences, ""));
                    String globallyAligned = computeGlobalReadAlignment(dna2ProteinMode, referencesGlobalAlignment.get(refName), readSequence, readGlobalAlignmentLength);
                    readsGlobalAlignment.put(readName, globallyAligned);
                }
                progress.close();
            }
        }

        if (readsGlobalAlignment.size() <= 5) {
            System.err.println("Reads alignment (" + readsGlobalAlignment.size() + " x " + readGlobalAlignmentLength + "):");
            for (String name : readNames) {
                System.err.println(readsGlobalAlignment.get(name));
            }
        }

        Map<String, String> readName2ReadSequence = new HashMap<>();

        if (readsFile.length() > 0) {
            readNames.clear();
            try (FastAFileIterator it = new FastAFileIterator(readsFile)) {
                while (it.hasNext()) {
                    Pair<String, String> pair = it.next();
                    String name = Basic.getFirstWord(Basic.swallowLeadingGreaterSign(pair.getFirst()));
                    readNames.add(name);
                    if (outputFormat == OutputFormat.BlastXText)
                        readName2ReadSequence.put(name, pair.get2());
                }
            }
        }

        try (final Writer w = (outputFile.length() == 0 ? new OutputStreamWriter(System.out) : new FileWriter(outputFile))) {
            ProgressPercentage progress = new ProgressPercentage("Writing file: " + outputFile, -1);
            switch (outputFormat) {
                case MSA: {
                    for (String name : readNames) {
                        if (readsGlobalAlignment.get(name) != null)
                            w.write(String.format(">%s\n%s\n", name, readsGlobalAlignment.get(name)));
                    }
                    break;
                }
                case BlastNText: {
                    w.write("BLASTN computed by MergeAlignments\n\n");
                    String referenceSequence = computeConsensus(readsGlobalAlignment, 'N');

                    for (String name : readNames) {
                        if (readsGlobalAlignment.get(name) != null) {
                            w.write("Query= " + name + "\n\n");
                            w.write(getPairwiseBlastNAlignment(referenceName, referenceSequence, readsGlobalAlignment.get(name)));
                        }
                    }
                    break;
                }
                case BlastXText: {
                    w.write("BLASTX computed by MergeAlignments\n\n");
                    String referenceSequence = computeConsensus(referencesGlobalAlignment, 'X');

                    for (String name : readNames) {
                        if (readsGlobalAlignment.get(name) != null) {
                            w.write("Query= " + name + "\n\n");
                            w.write(getPairwiseBlastXAlignment(referenceName, referenceSequence, readsGlobalAlignment.get(name), readName2ReadSequence.get(name)));
                        }
                    }
                    break;
                }
            }
            progress.close();
        }
    }

    /**
     * put brackets around all inserts in the read sequence
     *
     * @param dna2ProteinMode
     * @param refSequence
     * @param readSequence
     * @return shorted reads
     */
    private String placeBracketsAroundInsertionsInReadSequence(boolean dna2ProteinMode, String refSequence, String readSequence) {
        StringBuilder buf = new StringBuilder();

        int readPos = 0;
        boolean inInsertion = false;
        for (int i = 0; i < refSequence.length() && readPos < readSequence.length(); i++) {
            final boolean isInsertion = (refSequence.charAt(i) == '-' && readSequence.charAt(readPos) != '-');

            if (!inInsertion && isInsertion) {
                buf.append("[");
                inInsertion = true;
            }
            if (inInsertion && !isInsertion) {
                buf.append("]");
                inInsertion = false;
            }

            if (dna2ProteinMode) {
                buf.append(readSequence.charAt(readPos++));
                buf.append(readSequence.charAt(readPos++));
                buf.append(readSequence.charAt(readPos++));
            } else
                buf.append(readSequence.charAt(readPos++));

        }
        if (inInsertion) {
            buf.append("]");
        }
        return buf.toString();
    }


    /**
     * get the algined sequence in the line
     * Possible formats:
     * XXXXX
     * 5 XXXX  (5 leading gaps)
     * 5 XXXX 3 (3 trailing gaps)
     *
     * @param aLine
     * @return
     */
    private String parseAlignedSequence(String aLine) {
        String[] tokens = aLine.split("\\s+");
        switch (tokens.length) {
            case 3: {
                final StringBuilder buf = new StringBuilder();
                int gaps = Integer.parseInt(tokens[0]);
                while (--gaps >= 0)
                    buf.append("-");
                buf.append(tokens[1]);
                gaps = Integer.parseInt(tokens[2]);
                while (--gaps >= 0)
                    buf.append("-");
                return buf.toString();
            }
            case 2: {
                final StringBuilder buf = new StringBuilder();
                int gaps = Integer.parseInt(tokens[0]);
                while (--gaps >= 0)
                    buf.append("-");
                buf.append(tokens[1]);
                return buf.toString();
            }
            case 1:
                return tokens[0];
            default:
                return aLine.replaceAll("\\s+", "");
        }
    }

    /**
     * compute the global alignment of a locally aligned read
     *
     * @param globallyAlignedReference
     * @param locallyAlignedRead
     * @return globally aligned read
     */
    private String computeGlobalReadAlignment(boolean dna2ProteinMode, String globallyAlignedReference, String locallyAlignedRead, int readGlobalAlignmentLength) throws IOException {
        if (readGlobalAlignmentLength < 20) {
            System.err.print("\nreference:  ");
            if (!dna2ProteinMode) {
                for (int i = 0; i < globallyAlignedReference.length(); i++) {
                    System.err.print(" " + globallyAlignedReference.charAt(i) + " ");
                }
            } else {
                for (int i = 0; i < globallyAlignedReference.length(); i++) {
                    System.err.print(" <" + globallyAlignedReference.charAt(i) + "> ");
                }
            }
            System.err.println("\t\ttotal: " + globallyAlignedReference.length() +
                    " letters: " + (globallyAlignedReference.length() - Basic.countOccurrences(globallyAlignedReference, '-'))
                    + " gaps: " + Basic.countOccurrences(globallyAlignedReference, '-'));


            System.err.print("local read: ");
            if (!dna2ProteinMode) {
                for (int i = 0; i < locallyAlignedRead.length(); i++) {
                    System.err.print(" " + locallyAlignedRead.charAt(i) + " ");
                }
            } else {
                int i = 0;
                while (i < locallyAlignedRead.length()) {
                    if (locallyAlignedRead.charAt(i) == '[') {
                        System.err.print("[");
                        i++;
                    } else
                        System.err.print(" ");
                    System.err.print(String.format("%c%c%c", locallyAlignedRead.charAt(i++), locallyAlignedRead.charAt(i++), locallyAlignedRead.charAt(i++)));
                    if (i < locallyAlignedRead.length() && locallyAlignedRead.charAt(i) == ']') {
                        System.err.print("]");
                        i++;
                    } else
                        System.err.print(" ");
                }
            }
            System.err.println("\t\ttotal: " + locallyAlignedRead.length() +
                    " letters: " + (locallyAlignedRead.length() - Basic.countOccurrences(locallyAlignedRead, '-'))
                    + " gaps: " + Basic.countOccurrences(locallyAlignedRead, '-'));
        }


        final StringBuilder buf = new StringBuilder();
        if (dna2ProteinMode) {
            int localPos = 0;
            boolean inInsertion = false;
            for (int globalPos = 0; globalPos < globallyAlignedReference.length() && localPos < locallyAlignedRead.length(); globalPos++) {
                if (locallyAlignedRead.charAt(localPos) == '[') {
                    if (inInsertion)
                        throw new IOException("inInsertion=true: Already in insertion");
                    inInsertion = true;
                    localPos++;
                } else if (locallyAlignedRead.charAt(localPos) == ']') {
                    if (!inInsertion)
                        throw new IOException("inInsertion=false: Not in insertion");
                    inInsertion = false;
                    localPos++;
                }

                if (inInsertion) {
                    if (globallyAlignedReference.charAt(globalPos) == '-') {
                        buf.append(locallyAlignedRead.charAt(localPos++));
                        buf.append(locallyAlignedRead.charAt(localPos++));
                        buf.append(locallyAlignedRead.charAt(localPos++));
                    } else
                        localPos += 3; // swallow nucleotides todo: need to insert
                } else {
                    if (globallyAlignedReference.charAt(globalPos) == '-') {
                        buf.append("---");
                    } else {
                        buf.append(locallyAlignedRead.charAt(localPos++));
                        buf.append(locallyAlignedRead.charAt(localPos++));
                        buf.append(locallyAlignedRead.charAt(localPos++));
                    }
                }
            }
        } else {
            int localPos = 0;
            boolean inInsertion = false;
            for (int globalPos = 0; globalPos < globallyAlignedReference.length() && localPos < locallyAlignedRead.length(); globalPos++) {
                if (locallyAlignedRead.charAt(localPos) == '[') {
                    if (inInsertion)
                        throw new IOException("inInsertion=true: Already in insertion");
                    inInsertion = true;
                    localPos++;
                } else if (locallyAlignedRead.charAt(localPos) == ']') {
                    if (!inInsertion)
                        throw new IOException("inInsertion=false: Not in insertion");
                    inInsertion = false;
                    localPos++;
                }
                if (inInsertion) {
                    if (globallyAlignedReference.charAt(globalPos) == '-') {
                        buf.append(locallyAlignedRead.charAt(localPos++));
                    } else
                        localPos++; // swallow nucleotides todo: need to insert
                } else {
                    if (globallyAlignedReference.charAt(globalPos) == '-') {
                        buf.append("-");
                    } else {
                        buf.append(locallyAlignedRead.charAt(localPos++));
                    }
                }
            }
        }
        String result = buf.toString();
        if (result.length() == readGlobalAlignmentLength)
            return result;
        else { // add missing gaps to end of alignment
            int missing = readGlobalAlignmentLength - result.length();
            for (int i = 0; i < missing; i++) {
                buf.append("-");
            }
            return buf.toString();
        }
    }

    /**
     * computes the consensus for a set of reads or references
     *
     * @param globalAlignment
     * @return consensus
     */
    private String computeConsensus(Map<String, String> globalAlignment, char unknownChar) {
        if (globalAlignment.size() > 0) {
            String firstRow = globalAlignment.values().iterator().next();
            StringBuilder buf = new StringBuilder();
            Map<Character, Integer> char2count = new HashMap<>();
            for (int i = 0; i < firstRow.length(); i++) {
                for (String row : globalAlignment.values()) {

                    char ch = row.charAt(i);
                    if (Character.isLetter(ch)) {
                        Integer count = char2count.get(ch);
                        if (count == null)
                            char2count.put(ch, 1);
                        else
                            char2count.put(ch, count + 1);
                    }
                }
                char bestChar = unknownChar;
                int bestCount = 0;
                for (Character ch : char2count.keySet()) {
                    if (char2count.get(ch) > bestCount) {
                        bestChar = ch;
                        bestCount = char2count.get(ch);
                    }
                }
                buf.append(bestChar);
                char2count.clear();
            }
            return buf.toString();
        } else
            return "";
    }

    /**
     * get the paired alignment string
     *
     * @param referenceName
     * @param referenceSequence
     * @param alignedRead
     * @return paired alignment
     */
    private String getPairwiseBlastNAlignment(String referenceName, String referenceSequence, String alignedRead) {
        StringBuilder buf = new StringBuilder();

        int leadingGaps = countLeadingGaps(alignedRead);
        int trailingGaps = countTrailingGaps(alignedRead);

        buf.append(String.format(">%s\n  Length = %d\n\n", referenceName, referenceSequence.replaceAll("-", "").length()));

        buf.append(" Score = 50 bits (0), Expect = 0\n");
        int aLength = referenceSequence.length() - leadingGaps - trailingGaps;
        int identities = computeIdentities(referenceSequence, alignedRead, leadingGaps, trailingGaps);
        int gaps = computeGaps(referenceSequence, alignedRead, leadingGaps, trailingGaps);

        buf.append(String.format(" Identities = %d/%d (%d%%), Gaps = %d/%d (%d%%)\n", identities, aLength, Math.round((100.0 * identities) / aLength), gaps, aLength, Math.round((100.0 * gaps) / aLength)));
        buf.append("  Strand = Plus / Plus\n\n");

        buf.append(String.format("Query:%8d %s %d\n", leadingGaps + 1, alignedRead.substring(leadingGaps, alignedRead.length() - trailingGaps), alignedRead.length() - trailingGaps));
        buf.append(String.format("               %s\n", computeMid(referenceSequence, alignedRead, leadingGaps, trailingGaps)));
        int refStart = 1;
        for (int i = 0; i < leadingGaps; i++) {
            if (Character.isLetter(referenceSequence.charAt(i)) || referenceSequence.charAt(i) == '?')
                refStart++;
        }
        int refEnd = referenceSequence.length();
        for (int i = 0; i < trailingGaps; i++) {
            if (Character.isLetter(referenceSequence.charAt(referenceSequence.length() - i - 1)))
                refEnd--;
        }
        buf.append(String.format("Sbjct:%8d %s %d\n\n", refStart, referenceSequence.substring(leadingGaps, referenceSequence.length() - trailingGaps), refEnd));
        return buf.toString();
    }

    /**
     * get the paired alignment string
     *
     * @param referenceName
     * @param referenceSequence
     * @param alignedRead
     * @return paired alignment
     */
    private String getPairwiseBlastXAlignment(String referenceName, String referenceSequence, String alignedRead, String originalRead) {
        // todo: implement this
        StringBuilder buf = new StringBuilder();

        int leadingGaps = countLeadingGaps(alignedRead);
        int trailingGaps = countTrailingGaps(alignedRead);

        buf.append(String.format(">%s\n  Length = %d\n\n", referenceName, referenceSequence.replaceAll("-", "").length()));

        buf.append(" Score = 50 bits (0), Expect = 0\n");
        int aLength = referenceSequence.length() - leadingGaps - trailingGaps;
        int identities = computeIdentities(referenceSequence, alignedRead, leadingGaps, trailingGaps);
        int gaps = computeGaps(referenceSequence, alignedRead, leadingGaps, trailingGaps);

        buf.append(String.format(" Identities = %d/%d (%d%%), Gaps = %d/%d (%d%%)\n", identities, aLength, Math.round((100.0 * identities) / aLength), gaps, aLength, Math.round((100.0 * gaps) / aLength)));
        buf.append("  Strand = Plus / Plus\n\n");

        buf.append(String.format("Query:%8d %s %d\n", leadingGaps + 1, alignedRead.substring(leadingGaps, alignedRead.length() - trailingGaps), alignedRead.length() - trailingGaps));
        buf.append(String.format("               %s\n", computeMid(referenceSequence, alignedRead, leadingGaps, trailingGaps)));
        int refStart = 1;
        for (int i = 0; i < leadingGaps; i++) {
            if (Character.isLetter(referenceSequence.charAt(i)) || referenceSequence.charAt(i) == '?')
                refStart++;
        }
        int refEnd = referenceSequence.length();
        for (int i = 0; i < trailingGaps; i++) {
            if (Character.isLetter(referenceSequence.charAt(referenceSequence.length() - i - 1)))
                refEnd--;
        }
        buf.append(String.format("Sbjct:%8d %s %d\n\n", refStart, referenceSequence.substring(leadingGaps, referenceSequence.length() - trailingGaps), refEnd));


        return buf.toString();
    }

    private int countLeadingGaps(String alignedRead) {
        for (int i = 0; i < alignedRead.length(); i++) {
            if (Character.isLetter(alignedRead.charAt(i)))
                return i;
        }
        return 0;
    }

    private int countTrailingGaps(String alignedRead) {
        for (int i = 0; i < alignedRead.length(); i++) {
            if (Character.isLetter(alignedRead.charAt(alignedRead.length() - i - 1)))
                return i;
        }
        return 0;
    }

    private int computeIdentities(String referenceSequence, String alignedRead, int leadingGaps, int trailingGaps) {
        int count = 0;
        for (int i = leadingGaps; i < referenceSequence.length() - trailingGaps; i++) {
            if (Character.isLetter(referenceSequence.charAt(i)) && referenceSequence.charAt(i) == alignedRead.charAt(i))
                count++;
        }
        return count;
    }

    private int computeGaps(String referenceSequence, String alignedRead, int leadingGaps, int trailingGaps) {
        int count = 0;
        for (int i = leadingGaps; i < referenceSequence.length() - trailingGaps; i++) {
            if (referenceSequence.charAt(i) == '-' || alignedRead.charAt(i) == '-')
                count++;
        }
        return count;
    }

    private String computeMid(String referenceSequence, String alignedRead, int leadingGaps, int trailingGaps) {
        StringBuilder buf = new StringBuilder();
        for (int i = leadingGaps; i < referenceSequence.length() - trailingGaps; i++) {
            if (Character.isLetter(referenceSequence.charAt(i)) && referenceSequence.charAt(i) == alignedRead.charAt(i))
                buf.append("|");
            else
                buf.append(" ");
        }
        return buf.toString();
    }

}
