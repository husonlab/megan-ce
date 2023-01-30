/*
 * RequestHandlerAdditional.java Copyright (C) 2023 Daniel H. Huson
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

package megan.ms.server;

import com.sun.net.httpserver.HttpExchange;
import jloda.util.FileUtils;
import jloda.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * addition request handlers
 * Daniel Huson, 10.2020
 */
public class RequestHandlerAdditional {
    private final static String DOWNLOAD_FILE_PREFIX = "DOWNLOAD-FILE:";

    static RequestHandler getDownloadPage(Database database) {
        return (c, p) -> {
            try {
                RequestHandler.checkKnownParameters(p, "file");
                RequestHandler.checkRequiredParameters(p, "file");
                final var name = Parameters.getValue(p, "file");
                final var file = database.getRecord(name).getFile();
                return (DOWNLOAD_FILE_PREFIX + file).getBytes();
            } catch (IOException ex) {
                return RequestHandler.reportError(c, p, ex.getMessage());
            }
        };
    }

    public static HttpHandlerMS getDownloadPageHandler(Database database) {
        final RequestHandler requestHandler = getDownloadPage(database);
        return new HttpHandlerMS(requestHandler) {
            public void respond(HttpExchange httpExchange, String[] parameters) throws IOException {
                final var bytes = requestHandler.handle(httpExchange.getHttpContext().getPath(), parameters);

				if (!StringUtils.startsWith(bytes, DOWNLOAD_FILE_PREFIX))
					throw new IOException("invalid");
				final var fileName = StringUtils.getTextAfter(DOWNLOAD_FILE_PREFIX, StringUtils.toString(bytes));
                if (fileName == null)
                    throw new IOException("No file name");
                final var file = new File(fileName);
                if (!file.exists())
                    throw new IOException("No such file: " + file);

                httpExchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
				var headerKey = "Content-Disposition";
				var headerValue = String.format("attachment; filename=\"%s\"", FileUtils.getFileNameWithoutPath(fileName));
                httpExchange.getResponseHeaders().set(headerKey, headerValue);

                httpExchange.sendResponseHeaders(200, file.length());
                try (var outs = httpExchange.getResponseBody();
                     var ins = new BufferedInputStream(new FileInputStream(fileName))) {
                    var buf = new byte[8192];
                    int length;
                    while ((length = ins.read(buf)) > 0) {
                        outs.write(buf, 0, length);
                    }
                }
            }
        };
    }
}
