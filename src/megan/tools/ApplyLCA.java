/*
 *  Copyright (C) 2015 Daniel H. Huson
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

import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.UsageException;
import megan.algorithms.AssignmentUsingLCA;
import megan.classification.Classification;
import megan.main.Megan6;

import java.io.*;

/**
 * applies the LCA to input lines
 */
public class ApplyLCA {
    /**
     * apply the LCA
     *
     * @param args
     * @throws UsageException
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
            ProgramProperties.setProgramName("ApplyLCA");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            long start = System.currentTimeMillis();
            (new ApplyLCA()).run(args);
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
    private void run(String[] args) throws Exception {
        final ArgsOptions options = new ArgsOptions(args, this, "Applies the LCA to taxon-ids");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        final String inputFile = options.getOptionMandatory("-i", "input", "Input  file (stdin ok)", "");
        final String outputFile = options.getOption("-o", "output", "Output file (stdout ok)", "stdout");
        String separator = options.getOption("-s", "Separator", "Separator character (or detect)", "detect");
        final boolean firstLineIsHeader = options.getOption("-H", "hasHeaderLine", "Has header line", true);
        options.done();

        final AssignmentUsingLCA assignmentUsingLCA = new AssignmentUsingLCA(Classification.Taxonomy);

        final Writer w = new BufferedWriter(outputFile.equalsIgnoreCase("stdout") ? new OutputStreamWriter(System.out) : new FileWriter(outputFile));
        try (BufferedReader r = new BufferedReader(inputFile.equals("stdin") ? new InputStreamReader(System.in) : new FileReader(inputFile))) {
            String line;
            boolean first = true;
            int lineNumber = 0;
            while ((line = r.readLine()) != null) {
                lineNumber++;
                if (first) {
                    first = false;
                    if (separator.equals("detect")) {
                        if (line.contains("\t"))
                            separator = "\t";
                        else if (line.contains(","))
                            separator = ",";
                        else if (line.contains(";"))
                            separator = ";";
                        else
                            throw new IOException("Can't detect separator (didn't find tab, comma or semi-colon in first line)");
                        if (firstLineIsHeader) {
                            w.write(line + "\n");
                            continue;
                        }
                    }
                }
                final String[] tokens = line.split("\\s*" + separator + "\\s*");
                if (tokens.length > 0) {
                    int taxonId = -1;
                    for (int i = 1; i < tokens.length; i++) {
                        final String token = tokens[i].trim();
                        if (!Basic.isInteger(token)) {
                            taxonId = 0;
                            break;
                        } else {
                            final int id = Basic.parseInt(token);
                            if (id > 0) {
                                taxonId = (taxonId == -1 ? id : assignmentUsingLCA.getLCA(taxonId, id));
                            }
                        }
                    }
                    w.write(tokens[0] + separator + taxonId + "\n");
                }
            }
            w.flush();
        } finally {
            if (!outputFile.equalsIgnoreCase("stdout"))
                w.close();
        }
    }
}
