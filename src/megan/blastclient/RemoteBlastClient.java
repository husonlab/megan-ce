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

package megan.blastclient;

import jloda.util.Basic;
import jloda.util.Pair;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;


/**
 * Remotely blast sequences using the NCBI web service
 * Author: Daniel Huson 1/11/17.
 */
public class RemoteBlastClient {
    public enum BlastProgram {
        blastn, blastx, blastp;
        // blastn, blastx,  blastp, megablast, discomegablast, rpsblast, tblastn, tblastx; // todo: these don't appear to work

        public static BlastProgram valueOfIgnoreCase(String name) {
            for (BlastProgram blastProgram : values()) {
                if (blastProgram.toString().equalsIgnoreCase(name))
                    return blastProgram;
            }
            return null;
        }
    }

    public enum Status {stopped, searching, hitsFound, noHitsFound, failed, unknown}

    private static final boolean verbose = false;
    private final static String baseURL = "https://blast.ncbi.nlm.nih.gov/blast/Blast.cgi";

    private BlastProgram program;
    private String database; // make sure you use a correct database name. If the database does not exist, then this will
    // not cause an error, but no alignments will be found
    private String requestId;
    private int estimatedTime;
    private int actualTime;
    private long startTime;

    /**
     * constructor
     */
    public RemoteBlastClient() {
        program = BlastProgram.blastx;
        database = "nr";
        requestId = null;
        estimatedTime = -1;
        actualTime = -1;
        startTime = -1;
    }

    /**
     * launch the search
     *
     * @param queries
     * @return request Id
     */
    public String startRemoteSearch(Collection<Pair<String, String>> queries) throws IOException {
        if (queries.size() == 0)
            return null;
        final StringBuilder query = new StringBuilder();
        for (Pair<String, String> pair : queries) {
            query.append(">").append(Basic.swallowLeadingGreaterSign(pair.get1().trim())).append("\n").append(pair.get2().trim()).append("\n");
        }
        requestId = null;
        estimatedTime = -1;
        actualTime = -1;
        startTime = System.currentTimeMillis();

        final Map<String, Object> params = new HashMap<>();
        params.put("CMD", "Put");
        params.put("PROGRAM", program.toString());
        params.put("DATABASE", database);
        params.put("QUERY", query.toString());
        final String response = postRequest(baseURL, params);
        requestId = parseRequestId(response);
        if (requestId == null)
            throw new IOException("Failed to obtain valid requestId");
        estimatedTime = parseEstimatedTime(response);

        return requestId;
    }

    /**
     * request the current status
     *
     * @return status
     */
    public Status getRemoteStatus() {
        if (requestId == null)
            return Status.failed;
        try {
            final Map<String, Object> params = new HashMap<>();
            params.put("CMD", "Get");
            params.put("FORMAT_OBJECT", "SearchInfo");
            params.put("RID", requestId);
            final String response = getRequest(baseURL, params);

            Status status = Status.unknown;
            boolean thereAreNoHits = false;
            boolean thereAreHits = false;
            for (String aLine : getLinesBetween(response, "QBlastInfoBegin", "QBlastInfoEnd")) {
                if (aLine.contains("Status=")) {
                    switch (aLine.replaceAll("Status=", "").trim()) {
                        case "WAITING":
                            status = Status.searching;
                            break;
                        case "FAILED":
                            status = Status.failed;
                            break;
                        case "READY":
                            actualTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
                            status = Status.hitsFound; // will check whether hits really found
                            break;
                        default:
                        case "UNKNOWN":
                            status = Status.unknown;
                    }
                } else if (aLine.contains("ThereAreHits=no"))
                    thereAreNoHits = true;
                else if (aLine.contains("ThereAreHits=yes"))
                    thereAreHits = true;
            }
            if (status == Status.hitsFound && thereAreNoHits)
                status = Status.noHitsFound;
            if (status == Status.hitsFound && !thereAreHits)
                status = Status.failed;
            return status;
        } catch (IOException e) {
            e.printStackTrace();
            return Status.unknown;
        }
    }

    /**
     * request the alignments
     *
     * @return gets the alignments
     */
    public String[] getRemoteAlignments() {
        try {
            final Map<String, Object> params = new HashMap<>();
            params.put("CMD", "Get");
            params.put("FORMAT_TYPE", "Text");
            params.put("RID", requestId);
            return getLinesBetween(getRequest(baseURL, params), "<PRE>", null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get the blast program
     *
     * @return blast program
     */
    public BlastProgram getProgram() {
        return program;
    }

    /**
     * set the blast program
     *
     * @param program
     * @return
     */
    public RemoteBlastClient setProgram(BlastProgram program) {
        this.program = program;
        return this;
    }

    /**
     * get the database
     *
     * @return database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * set the database
     *
     * @param database
     * @return
     */
    public RemoteBlastClient setDatabase(String database) {
        this.database = database;
        return this;
    }

    /**
     * get estimated time in seconds
     *
     * @return time or -1
     */
    public int getEstimatedTime() {
        return estimatedTime;
    }

    public int getActualTime() {
        return actualTime;
    }

    public String getRequestId() {
        return requestId;
    }

    /**
     * get the request id from a response text
     *
     * @param response
     * @return request id or null
     */
    private static String parseRequestId(String response) {
        for (String aLine : getLinesBetween(response, "QBlastInfoBegin", "QBlastInfoEnd")) {
            if (aLine.contains("    RID = ")) {
                return aLine.replaceAll("    RID = ", "").trim();

            }
        }
        return null;
    }

    /**
     * get the estimated time
     *
     * @param response
     * @return time or -1
     */
    private static Integer parseEstimatedTime(String response) {
        for (String aLine : getLinesBetween(response, "QBlastInfoBegin", "QBlastInfoEnd")) {
            if (aLine.contains("    RTOE = ")) {
                return Integer.parseInt(aLine.replaceAll("    RTOE = ", "").trim());

            }
        }
        return -1;
    }

    /**
     * get a delimited set of lines
     *
     * @param text
     * @param afterLineEndingOnThis      start reporting lines after seeing a line ending on this, or from beginning, if null
     * @param beforeLineStartingWithThis stop reporting lines upon seeing a line starting on this, or a the end, if null
     * @return delimited text
     */
    private static String[] getLinesBetween(String text, String afterLineEndingOnThis, String beforeLineStartingWithThis) {
        final ArrayList<String> lines = new ArrayList<>();
        boolean reporting = (afterLineEndingOnThis == null);
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            String aLine;
            while ((aLine = reader.readLine()) != null) {
                if (reporting) {
                    if (beforeLineStartingWithThis != null && aLine.startsWith(beforeLineStartingWithThis))
                        reporting = false;
                    else
                        lines.add(aLine);
                } else if (afterLineEndingOnThis != null && aLine.endsWith(afterLineEndingOnThis)) {
                    reporting = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lines.toArray(new String[0]);
    }

    /**
     * get request
     *
     * @param parameters
     * @return response
     * @throws IOException
     */
    private static String getRequest(String baseURL, Map<String, Object> parameters) throws IOException {
        final StringBuilder urlString = new StringBuilder();
        urlString.append(baseURL);
        boolean first = true;
        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            if (first) {
                urlString.append("?");
                first = false;
            } else
                urlString.append("&");
            urlString.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            urlString.append('=');
            urlString.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }

        if (verbose) {
            System.err.println("GET " + baseURL);
        }

        final URL url = new URL(urlString.toString());
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "text/plain");
        connection.setRequestProperty("charset", "utf-8");
        connection.setDoOutput(true);
        connection.connect();

        final StringBuilder response = new StringBuilder();
        try (Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            for (int c; (c = in.read()) >= 0; ) {
                response.append((char) c);
            }
        }
        if (verbose)
            System.err.println("Response " + response.toString());
        return response.toString();
    }

    /**
     * post request
     *
     * @param parameters
     * @return response
     * @throws IOException
     */
    private static String postRequest(String baseURL, Map<String, Object> parameters) throws IOException {
        final StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");

        if (verbose) {
            System.err.println("POST " + baseURL);
            System.err.println(postData.toString());
        }

        final URL url = new URL(baseURL);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        connection.setDoOutput(true);
        connection.getOutputStream().write(postDataBytes);

        final StringBuilder response = new StringBuilder();
        try (Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            for (int c; (c = in.read()) >= 0; ) {
                response.append((char) c);
            }
        }
        if (verbose)
            System.err.println("Response " + response.toString());
        return response.toString();
    }

    public static String[] getDatabaseNames(String blastProgram) {
        return getDatabaseNames(Objects.requireNonNull(BlastProgram.valueOfIgnoreCase(blastProgram)));
    }

    public static String[] getDatabaseNames(BlastProgram blastProgram) {
        switch (blastProgram) {
            case blastp:
            case blastx:
                return new String[]{"nr", "refseq_protein", "SMARTBLAST/landmark", "swissprot", "pat", "pdb", "env_nr", "tsa_nr"};
            default:
                return new String[]{"nr", "refseq_rna", "refseq_genomic", "refseq_representative_genomes",
                        "genomic/9606/RefSeqGene", "est", "gss", "htgs", "pat", "pdb", "alu", "dbsts",
                        "chromosome", "Whole_Genome_Shotgun_contigs", "tsa_nt", "rRNA_typestrains/prokaryotic_16S_ribosomal_RNA",
                        "sra"};
        }
    }
}
