/*
 * RequestHandlerAdditional.java Copyright (C) 2020. Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *  No usage, copying or distribution without explicit permission.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package megan.ms.server;

import com.sun.net.httpserver.HttpExchange;
import jloda.util.Basic;

import java.io.*;

/**
 * addition request handlers
 * Daniel Huson, 10.2020
 */
public class RequestHandlerAdditional {
    private final static String DOWNLOAD_FILE_PREFIX = "DOWNLOAD-FILE:";

    static RequestHandler getDownloadPage(Database database) {
        return (c, p) -> {
            try {
                RequestHandler.checkKnownParameters(p, "name");
                RequestHandler.checkRequiredParameters(p, "name");
                final String name = Parameters.getValue(p, "name");
                final File file = database.getRecord(name).getFile();
                return (DOWNLOAD_FILE_PREFIX + file).getBytes();
            } catch (IOException ex) {
                return RequestHandler.reportError(c, p, ex.getMessage());
            }
        };
    }

    public static HttpHandlerMS getDownloadPageHandler(Database database) {
        final RequestHandler requestHandler = getDownloadPage(database);
        final HttpHandlerMS handlerMS = new HttpHandlerMS(requestHandler) {
            public void respond(HttpExchange httpExchange, String[] parameters) throws IOException {
                final byte[] bytes = requestHandler.handle(httpExchange.getHttpContext().getPath(), parameters);

                if (!Basic.startsWith(bytes, DOWNLOAD_FILE_PREFIX))
                    throw new IOException("invalid");
                final String fileName = Basic.getTextAfter(DOWNLOAD_FILE_PREFIX, Basic.toString(bytes));
                if (fileName == null)
                    throw new IOException("No file name");
                final File file = new File(fileName);
                if (!file.exists())
                    throw new IOException("No such file: " + file);

                httpExchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                String headerKey = "Content-Disposition";
                String headerValue = String.format("attachment; filename=\"%s\"", Basic.getFileNameWithoutPath(fileName));
                httpExchange.getResponseHeaders().set(headerKey, headerValue);

                httpExchange.sendResponseHeaders(200, file.length());
                try (OutputStream outs = httpExchange.getResponseBody();
                     BufferedInputStream ins = new BufferedInputStream(new FileInputStream(fileName))) {
                    byte[] buf = new byte[8192];
                    int length;
                    while ((length = ins.read(buf)) > 0) {
                        outs.write(buf, 0, length);
                    }
                }
            }
        };
        return handlerMS;
    }
}
