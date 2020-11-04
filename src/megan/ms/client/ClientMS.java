/*
 * Copyright (C) 2020. Daniel H. Huson
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

package megan.ms.client;

import jloda.util.Basic;
import megan.ms.Utilities;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        final InetSocketAddress proxyAddress = (Basic.notBlank(proxyName) ? new InetSocketAddress(proxyName, proxyPort) : null);

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
            final HttpRequest request = setupRequest("/list", false);
            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            final List<String> list = response.body().collect(Collectors.toList());
            if (list.size() > 0 && list.get(0).startsWith(Utilities.SERVER_ERROR)) {
                System.err.println(list.get(0));
                throw new IOException(list.get(0));
            } else
                return list;
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
                System.err.println(Basic.getFirstLine(result));
                throw new IOException(Basic.getFirstLine(result));
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
            if (Basic.startsWith(result, Utilities.SERVER_ERROR)) {
                System.err.println(Basic.getFirstLine(result));
                throw new IOException(Basic.getFirstLine(result));
            } else
                return result;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public HttpRequest setupRequest(String command, boolean binary) {
        final URI uri = URI.create(serverAndPrefix + (command.startsWith("/") ? command : "/" + command));
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
        return Basic.parseLong(Basic.getFirstWord(getAsString(command)));
    }

    /**
     * gets as long
     */
    public int getAsInt(String command) throws IOException {
        return Basic.parseInt(Basic.getFirstWord(getAsString(command)));
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
