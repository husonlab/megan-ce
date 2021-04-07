/*
 * Copyright (C) 2021. Daniel H. Huson
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
 *
 */

package megan.ms.server;

import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import megan.daa.connector.ClassificationBlockDAA;
import megan.data.IClassificationBlock;
import megan.data.IReadBlock;
import megan.ms.Utilities;
import megan.ms.client.connector.ReadBlockMS;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MeganServer request handler for data requests
 * Daniel Huson, 8.2020
 */
public interface RequestHandler {
    byte[] handle(String context, String[] parameters) throws IOException;

    static RequestHandler getDefault() {
        return (c, p) -> ("<html>\n" +
                "<body>\n" +
                "<b> Not implemented: " + c + " " + Basic.toString(p, ", ") +
                "</b>\n" +
                "</body>\n" +
                "</html>\n").getBytes();
    }

    static RequestHandler getAbout(Database database, HttpServerMS server) {
        return (c, p) -> {
            checkKnownParameters(p);
            return server.getAbout().getBytes();
        };
    }


    static RequestHandler getHelp() {
        return (c, p) -> {
            try {
                checkKnownParameters(p);
                try (InputStream ins = ResourceManager.getFileAsStream("ms/help.html")) {
                    if (ins != null) {
                        return ins.readAllBytes();
                    }
                }
                throw new IOException("Resource not found: ms/help.html");
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getVersion() {
        return (c, p) -> {
            try {
                checkKnownParameters(p);
                return MeganServer.Version.getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getListDataset(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "metadata", "readCount","matchCount");

                final boolean includeMetadata = Parameters.getValue(p, "metadata", false);
                final boolean readCount = Parameters.getValue(p, "readCount", false);
                final boolean matchCount = Parameters.getValue(p, "matchCount", false);

                final var list = new ArrayList<>();
                for (String fileName : database.getFileNames()) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append(fileName);
                    if (readCount)
                        buf.append("\t").append(database.getRecord(fileName).getNumberOfReads());
                    if (matchCount) {
                        final long matches=database.getRecord(fileName).getNumberOfMatches();
                        if(matches>0)
                        buf.append("\t").append(matches);
                    }
                    if (includeMetadata)
                        buf.append("\n").append(database.getMetadata(fileName));
                    list.add(buf.toString());
                }
                return Basic.toString(list, "\n").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getNumberOfReads(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "file");
                checkRequiredParameters(p, "file");

                final ArrayList<Long> list = new ArrayList<>();
                for (String fileName : Parameters.getValues(p, "file")) {
                    list.add(database.getRecord(fileName).getNumberOfReads());
                }
                return Basic.toString(list, " ").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getNumberOfMatches(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "file");
                checkRequiredParameters(p, "file");

                final ArrayList<Long> list = new ArrayList<>();
                for (String fileName : Parameters.getValues(p, "file")) {
                    list.add(database.getRecord(fileName).getNumberOfMatches());
                }
                return Basic.toString(list, " ").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getClassifications(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "file");
                checkRequiredParameters(p, "file");

                final ArrayList<String> list = new ArrayList<>();
                for (String fileName : Parameters.getValues(p, "file")) {
                    list.add(Basic.toString(database.getClassifications(fileName), "\n") + "\n");
                }
                return Basic.toString(list, "\n").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getAuxiliaryData(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "file", "binary");
                checkRequiredParameters(p, "file");

                final boolean binary = Parameters.getValue(p, "binary", false);
                final String fileName = Parameters.getValue(p, "file");

                final ArrayList<String> list = new ArrayList<>();
                Database.Record record = database.getRecord(fileName);
                if (binary) {
                    return Utilities.writeAuxiliaryDataToBytes(record.getAuxiliaryData());
                } else {
                    if (record != null && record.getAuxiliaryData() != null) {
                        for (String key : record.getAuxiliaryData().keySet()) {
                            list.add(key + ":\n" + Basic.toString(record.getAuxiliaryData().get(key)));
                        }
                    }
                }
                return Basic.toString(list, "\n").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getFileUid(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "file");
                checkRequiredParameters(p, "file");

                final ArrayList<String> list = new ArrayList<>();
                for (String fileName : Parameters.getValues(p, "file")) {
                    final Integer fileId = database.getFileName2Id().get(fileName);
                    list.add(fileId != null ? String.valueOf(fileId) : "File not found: " + fileName);
                }
                return Basic.toString(list, "\n").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getClassificationBlock(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "file", "classification", "binary");
                checkRequiredParameters(p, "file", "classification");

                final String fileName = Parameters.getValue(p, "file");
                final String classification = Parameters.getValue(p, "classification");
                final boolean binary = Parameters.getValue(p, "binary", false);
                IClassificationBlock classificationBlock = database.getClassificationBlock(fileName, classification);
                if(classificationBlock==null)
                    classificationBlock=new ClassificationBlockDAA(classification);
                if (binary) {
                    return Utilities.writeClassificationBlockToBytes(classificationBlock);
                } else {
                    final ArrayList<String> list = new ArrayList<>(classificationBlock.getKeySet().size());
                    list.add(classification);
                    list.add(String.valueOf(classificationBlock.getKeySet().size()));
                    for (Integer id : classificationBlock.getKeySet()) {
                        list.add(id + "\t" + classificationBlock.getWeightedSum(id) + "\t" + classificationBlock.getSum(id));
                    }
                    return Basic.toString(list, "\n").getBytes();
                }
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getClassSize(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "file", "classification", "classId", "sum");
                checkRequiredParameters(p, "file", "classification", "classId");

                final String fileName = Parameters.getValue(p, "file");
                final String classification = Parameters.getValue(p, "classification");
                final List<Integer> classIds = Parameters.getIntValues(p, "classId");
                final boolean wantSum = Parameters.getValue(p, "sum", false);

                final IClassificationBlock classificationBlock = database.getClassificationBlock(fileName, classification);
                final ArrayList<String> list = new ArrayList<>(classificationBlock.getKeySet().size());
                if (wantSum) {
                    int sum = 0;
                    for (Integer id : classIds) {
                        sum += classificationBlock.getSum(id);
                    }
                    list.add(String.valueOf(sum));
                } else {
                    for (Integer id : classIds) {
                        list.add(id + "\t" + classificationBlock.getSum(id));
                    }
                }
                return Basic.toString(list, "\n").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getRead(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "file", "readId", "header", "sequence", "matches", "binary");
                checkRequiredParameters(p, "file", "readId");

                final String fileName = Parameters.getValue(p, "file");
                final ReadsOutputFormat format = new ReadsOutputFormat(
                        false,
                        Parameters.getValue(p, "header", true),
                        Parameters.getValue(p, "sequence", true),
                        Parameters.getValue(p, "matches", true));

                final long readId = Parameters.getValue(p, "readId", -1L);
                final boolean binary = Parameters.getValue(p, "binary", true);

                if (binary) {
                    final String[] cNames = database.getClassifications(fileName).toArray(new String[0]);
                    return ReadBlockMS.writeToBytes(cNames, database.getRead(fileName, readId, format.isMatches()), format.isMatches());
                } else {
                    return ReadBlockMS.writeToString(database.getRead(fileName, readId, format.isMatches()), format.isReadIds(), format.isHeaders(), format.isSequences(), format.isMatches()).getBytes();
                }
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getReads(Database database,int defaultReadsPerPage) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "pageSize", "file", "readIds", "headers", "sequences", "matches", "binary");
                checkRequiredParameters(p, "file");

                final int pageSize = Parameters.getValue(p, "pageSize", defaultReadsPerPage);
                final String fileName = Parameters.getValue(p, "file");

                final ReadsOutputFormat format = new ReadsOutputFormat(
                        Parameters.getValue(p, "readIds", true),
                        Parameters.getValue(p, "headers", true),
                        Parameters.getValue(p, "sequences", true),
                        Parameters.getValue(p, "matches", true));

                final boolean binary = Parameters.getValue(p, "binary", true);

                final ReadIteratorPagination.Page page = database.getReads(fileName, format, pageSize);
                return getReads(c, binary, page);
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getReadsForMultipleClassIdsIterator(Database database,int defaultReadsPerPage) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "file", "classification", "classId", "readIds", "headers", "sequences", "matches", "binary", "pageSize");
                checkRequiredParameters(p, "file", "classification", "classId");

                final int pageSize = Parameters.getValue(p, "pageSize", defaultReadsPerPage);
                final String fileName = Parameters.getValue(p, "file");
                final String classification = Parameters.getValue(p, "classification");
                final List<Integer> classIds = Parameters.getIntValues(p, "classId");

                final ReadsOutputFormat format = new ReadsOutputFormat(
                        Parameters.getValue(p, "readIds", true),
                        Parameters.getValue(p, "headers", true),
                        Parameters.getValue(p, "sequences", true),
                        Parameters.getValue(p, "matches", true));

                final boolean binary = Parameters.getValue(p, "binary", true);

                final ReadIteratorPagination.Page page = database.getReadsForMultipleClassIds(fileName, classification, classIds, format, pageSize);
                return getReads(c, binary, page);
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getNextPage(Database database) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "pageId", "binary", "pageSize");
                checkRequiredParameters(p, "pageId");

                final int pageId = Parameters.getValue(p, "pageId", 0);
                final int pageSize = Parameters.getValue(p, "pageSize", -1); // -1: use what was initially set
                final boolean binary = Parameters.getValue(p, "binary", false);

                final ReadIteratorPagination.Page page = database.getNextPage(pageId, pageSize);
                return getReads(c, binary, page);
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    private static byte[] getReads(String c, boolean binary, ReadIteratorPagination.Page page) throws IOException {
        if (page != null) {
            final long nextPageId = page.getNextPage();

            if (binary) {
                final ArrayList<byte[]> list = new ArrayList<>();
                list.add(Utilities.getBytesLittleEndian(page.getReads().size()));
                for (IReadBlock readBlock : page.getReads()) {
                    final byte[] bytes = ReadBlockMS.writeToBytes(page.getCNames(), readBlock, page.getFormat().isMatches());
                    list.add(Utilities.getBytesLittleEndian(bytes.length));
                    list.add(bytes);
                }
                list.add(Utilities.getBytesLittleEndian(page.getNextPage()));
                return Basic.concatenate(list);
            } else {
                final ArrayList<String> list = new ArrayList<>();
                for (IReadBlock readBlock : page.getReads()) {
                    addReadToList(readBlock, page.getFormat(), list);
                }
                if (nextPageId != 0)
                    list.add("Next pageId=" + nextPageId);
                else
                    list.add("done");
                return Basic.toString(list, "\n").getBytes();
            }
        } else
            return reportError(c, new String[0], "failed");
    }

    private static void addReadToList(IReadBlock readBlock, ReadsOutputFormat format, ArrayList<String> list) {
        if (format.isReadIds())
            list.add(String.valueOf(readBlock.getUId()));
        if (format.isHeaders())
            list.add(">" + readBlock.getReadHeader());
        if (format.isSequences())
            list.add(readBlock.getReadSequence());
        if (format.isMatches()) {
            for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                list.add(readBlock.getMatchBlock(i).getText());
            }
        }
    }

    static byte[] reportError(String content, String[] parameters, String message) {
        final String error = (Utilities.SERVER_ERROR + content + "?" + Basic.toString(parameters, "&") + ": " + message);
        System.err.println(error);
        return error.getBytes();
    }

    static void checkKnownParameters(String[] parameters, String... known) throws IOException {
        final List<String> parameterNames = Arrays.stream(parameters).sequential().map(parameter ->
        {
            if (parameter.contains("="))
                return parameter.substring(0, parameter.indexOf("="));
            else
                return parameter;
        }).collect(Collectors.toList());
        for (String name : parameterNames) {
            if (!Basic.contains(known, name))
                throw new IOException("Unknown parameter: '" + name + "'");
        }
    }

    static void checkRequiredParameters(String[] parameters, String... required) throws IOException {
        final List<String> parameterNames = Arrays.stream(parameters).sequential().map(parameter -> {
            if (parameter.contains("="))
                return parameter.substring(0, parameter.indexOf("="));
            else
                return parameter;
        }).collect(Collectors.toList());
        for (String name : required) {
            if (!parameterNames.contains(name))
                throw new IOException("Missing parameter: '" + name + "'");
        }
    }
}
