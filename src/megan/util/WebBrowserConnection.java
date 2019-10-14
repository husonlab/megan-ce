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
package megan.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jloda.util.Basic;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Objects;

/**
 * listens for commands from webbrowser
 * Daniel Huson, 3.2012
 */
public class WebBrowserConnection implements Runnable {


    /**
     * run
     */
    public void run() {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(16358), 0);
        } catch (IOException e) {
            Basic.caught(e);
        }
        Objects.requireNonNull(server).createContext("/start", httpExchange -> {
            URI uri = httpExchange.getRequestURI();
            System.err.println("URI: " + uri);

            String command = uri.toString().substring(uri.toString().indexOf('?') + 1);
            System.err.println("Command: " + command);

            String response = "ok";
            httpExchange.sendResponseHeaders(200, 2);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        });
        server.setExecutor(null);
        server.start();
    }

    /**
     * test program. Enter the following in browser: http://localhost:16358/start?hello_world4
     *
     * @param args
     */
    public static void main(String[] args) {
        Thread thread = new Thread(new WebBrowserConnection());
        thread.start();
    }

}
