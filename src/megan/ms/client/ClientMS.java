/*
 * ClientMS.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.ms.client;

import jloda.util.Basic;
import jloda.util.NumberUtils;
import jloda.util.StringUtils;
import megan.ms.Utilities;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MeganServer client
 * Daniel Huson, 8.2020
 */
public class ClientMS {
    private final String serverAndPrefix;
    private final int timeoutSeconds;
    private final HttpClient httpClient;
    private int pageSize = 100;

    /**
     * constructor
     */
    public ClientMS(String serverAndPrefix, String proxyName, int proxyPort, String user, String passwdMD5, int timeoutSeconds) {
        this.serverAndPrefix = serverAndPrefix.replaceAll("/$", "");
        this.timeoutSeconds = timeoutSeconds;

        final var proxyAddress = (proxyName.isBlank() ? null:new InetSocketAddress(proxyName, proxyPort));

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .authenticator(new Authenticator() {
                                   protected PasswordAuthentication getPasswordAuthentication() {
                                       return new PasswordAuthentication(user, passwdMD5.toCharArray());
                                   }
                               }
                )
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.of(proxyAddress))
                .build();
    }

    public List<String> getFiles() throws IOException {
        try {
            final var request = setupRequest("/list", false);
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            final var list = response.body().collect(Collectors.toList());
            if (list.size() > 0 && list.get(0).startsWith(Utilities.SERVER_ERROR)) {
                System.err.println(list.get(0));
                throw new IOException(list.get(0));
            } else
                return list;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public List<FileRecord> getFileRecords() throws IOException {
        try {
            final var request = setupRequest("/list?readCount=true&matchCount=true", false);
            final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            final var list = response.body().toList();
            if (list.size() > 0 && list.get(0).startsWith(Utilities.SERVER_ERROR)) {
                System.err.println(list.get(0));
                throw new IOException(list.get(0));
            } else {
				return list.stream().map(line -> StringUtils.split(line, '\t'))
						.filter(tokens -> tokens.length > 0)
						.map(tokens -> {
							var name = tokens[0];
							var reads = (tokens.length > 1 ? NumberUtils.parseLong(tokens[1]) : 0);
							var matches = (tokens.length > 2 ? NumberUtils.parseLong(tokens[2]) : 0);
							return new FileRecord(name, reads, matches);
						}).collect(Collectors.toList());
            }

        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    /**
     * gets as string
     */
    public String getAsString(String command) throws IOException {
        try {
            final HttpRequest request = setupRequest(command, false);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            final String result = response.body();
            if (result.startsWith(Utilities.SERVER_ERROR)) {
				System.err.println(StringUtils.getFirstLine(result));
				throw new IOException(StringUtils.getFirstLine(result));
			} else
                return result;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public byte[] getAsBytes(String command) throws IOException {
        try {
			final HttpRequest request = setupRequest(command, true);
			final HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			final byte[] result = response.body();
			if (StringUtils.startsWith(result, Utilities.SERVER_ERROR)) {
				System.err.println(StringUtils.getFirstLine(result));
				throw new IOException(StringUtils.getFirstLine(result));
			} else
				return result;
		} catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public HttpRequest setupRequest(String command, boolean binary) {
        final var uri = URI.create(serverAndPrefix + (command.startsWith("/") ? command : "/" + command));
        if(Basic.getDebugMode())
            System.err.println("Remote request: " +uri);
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", binary ? "application/octet-stream" : "application/text")
                .build();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * gets as long
     */
    public long getAsLong(String command) throws IOException {
		return NumberUtils.parseLong(StringUtils.getFirstWord(getAsString(command)));
    }

    /**
     * gets as long
     */
    public int getAsInt(String command) throws IOException {
		return NumberUtils.parseInt(StringUtils.getFirstWord(getAsString(command)));
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public static class FileRecord {
        private final String name;
        private final long reads;
        private final long matches;
        private String description;

        public FileRecord(String name, long reads, long matches) {
            this.name = name;
            this.reads = reads;
            this.matches = matches;
            if(reads>0) {
                var description=String.format("reads=%,d",reads);
                if(matches>0)
                    description+=String.format(", matches=%,d",matches);
                this.description = description;
            }
            else
                description=null;
        }

        public String getName() {
            return name;
        }

        public long getReads() {
            return reads;
        }

        public long getMatches() {
            return matches;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
