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

package megan.ms.server;

import com.sun.net.httpserver.BasicAuthenticator;
import jloda.util.Basic;
import jloda.util.FileLineIterator;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * manages Megan server users
 * Daniel Huson, 10.2020
 */
public class UserManager {
    public static final String ADMIN="admin";

    private final String fileName;
    private final Map<String, String> user2passwordMD5 = new TreeMap<>();
    private final Map<String,Set<String>> user2roles =new TreeMap<>();

    private final MyAuthenticator authenticator = createAuthenticator(null);
    private final MyAuthenticator adminAuthenticator = createAuthenticator(ADMIN);

    public UserManager(String fileName) throws IOException {
        this.fileName = fileName;
        readFile();
    }

    public List<String> listAllUsers() {
        return user2passwordMD5.keySet().stream().map(user -> user+" "+ Basic.toString(user2roles.get(user),",")).collect(Collectors.toList());
    }

    public void readFile() throws IOException {
        if (Basic.fileExistsAndIsNonEmpty(fileName)) {
            try (var it = new FileLineIterator(fileName)) {
                StreamSupport.stream(it.lines().spliterator(), false).filter(line -> line.length() > 0 && !line.startsWith("#"))
                        .map(line -> Basic.split(line, '\t'))
                        .filter(tokens -> tokens.length >= 2 && tokens[0].length() > 0 && tokens[1].length() > 0)
                        .forEach(tokens -> {
                            user2passwordMD5.put(tokens[0], tokens[1]);
                            final Set<String> roles=new TreeSet<>();
                            user2roles.put(tokens[0],roles);
                            if (tokens.length == 3 && tokens[2].length()>0)
                                roles.addAll(Arrays.asList(Basic.split(tokens[2],',')));
                        });
            }
        }
    }

    public void writeFile() throws IOException {
        System.err.println("Updating file: " + fileName);
        try (var w = new OutputStreamWriter(Basic.getOutputStreamPossiblyZIPorGZIP(fileName))) {
            w.write("#MeganServer registered users\n");
            w.write("#Name\tpassword-md5\troles\n");
            for (var entry : user2passwordMD5.entrySet())
                w.write(String.format("%s\t%s\t%s\n", entry.getKey(), entry.getValue(), Basic.toString(user2roles.get(entry.getKey()),",")));
        }
    }


    public void addUser(String name, String password, boolean allowReplace, String... roles) throws IOException {
        if (!allowReplace && user2passwordMD5.containsKey(name))
            throw new IOException("User exists: " + name);
        user2passwordMD5.put(name, Basic.computeMD5(password));
        user2roles.put(name,new TreeSet<>(Arrays.asList(roles)));
        writeFile();
    }

    public void removeUser(String name) throws IOException {
        if (!user2passwordMD5.containsKey(name))
            throw new IOException("No such user: " + name);
        user2passwordMD5.remove(name);
        writeFile();
    }

    public void addRoles(String user, String... roles) throws IOException {
        if(roles.length>0) {
            user2roles.get(user).addAll(Arrays.asList(roles));
            writeFile();
        }
    }

    public void removeRoles(String user,  String... roles) throws IOException {
        if(roles.length>0) {
            user2roles.get(user).removeAll(Arrays.asList(roles));
            writeFile();
        }
    }

    public boolean userExists(String name) {
        return user2passwordMD5.containsKey(name);
    }

    public int size() {
        return user2passwordMD5.size();
    }

    public boolean hasAdmin() {
        for(var roles:user2roles.values()) {
            if (roles.contains(ADMIN))
                return true;
        }
        return false;
    }

    public void askForAdminPassword() throws IOException {
        System.err.println();

        final Console console = System.console();
        if (console == null)
            System.err.printf("ATTENTION: No admin defined in list of users, enter password for special user 'admin':%n");
        else
            console.printf("ATTENTION: No admin defined in list of users, enter password for special user 'admin':%n");

        String password;
        while (true) {
            if (console == null) {
                password = (new BufferedReader(new InputStreamReader(System.in)).readLine());
            } else {
                password = new String(console.readPassword());
            }
            if (password == null || password.length() == 0)
                break;
            else if (password.length() < 8)
                System.err.println("Too short, enter a longer password (at least 8 characters):");
            else if (password.contains("\t"))
                System.err.println("Contains a tab, enter a password that doesn't contain a tab:");
            else
                break;

        }
        if (password == null || password.length() == 0)
            throw new IOException("Failed to input admin password");
        addUser(ADMIN, password, true, UserManager.ADMIN);
    }

    public boolean checkCredentials(String role, String name, String passwordMD5) {
        return user2passwordMD5.containsKey(name) && passwordMD5.equals(user2passwordMD5.get(name)) && (role==null || role.equals("admin") || user2roles.get(name).contains(role));
    }

    public MyAuthenticator getAuthenticator() {
        return authenticator;
    }

    public MyAuthenticator getAdminAuthenticator() {
        return adminAuthenticator;
    }

    public MyAuthenticator createAuthenticator(String role) {
        return new MyAuthenticator(role);
    }

    public class MyAuthenticator extends BasicAuthenticator {
        private final String role;

        MyAuthenticator(String role) {
            super("get");
            this.role=role;
        }

        @Override
        public boolean checkCredentials(String username, String password) {
            return UserManager.this.checkCredentials(role, username, password) || UserManager.this.checkCredentials(role, username, Basic.computeMD5(password));
        }
    }
}
