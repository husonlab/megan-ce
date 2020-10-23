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

import jloda.util.Basic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static megan.ms.server.RequestHandler.*;

/**
 * handles admin requests
 * Daniel Huson, 10.2020
 */
public class RequestHandlerAdmin {

    static RequestHandler listUsers(UserManager userManager) {
        return (c, p) -> (Basic.toString(userManager.listAllUsers(), "\n")).getBytes();
    }

    static RequestHandler addUser(UserManager userManager) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "name", "password", "role","isAdmin", "replace");
                checkRequiredParameters(p, "name", "password");

                final String user = Parameters.getValue(p, "name");
                final String password = Parameters.getValue(p, "password");
                final boolean isAdmin=Parameters.getValue(p,"isAdmin",false);
                final String role = isAdmin?"admin":Parameters.getValue(p, "role");
                final boolean allowReplace = Parameters.getValue(p, "replace", false);

                userManager.addUser(user, password, allowReplace, role);
                return ("User '" + user + "' added").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler addRole(UserManager userManager) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "user",  "role");
                checkRequiredParameters(p, "user", "role");

                final String user = Parameters.getValue(p, "user");
                final String[] roles = Basic.split(Parameters.getValue(p, "role"),',');

                if(!userManager.userExists(user))
                    throw new IOException("No such user: "+user);

                userManager.addRoles(user,roles);
                return ("User " + user + ": role "+Basic.toString(roles,",")+" added").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler removeRole(UserManager userManager) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "user",  "role");
                checkRequiredParameters(p, "user", "role");

                final String user = Parameters.getValue(p, "name");
                final String[] roles = Basic.split(Parameters.getValue(p, "role"),',');

                if(!userManager.userExists(user))
                    throw new IOException("No such user: "+user);

                userManager.removeRoles(user,roles);
                return ("User " + user + ": role "+Basic.toString(roles,",")+" removed").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler removeUser(UserManager userManager) {
        return (c, p) -> {
            try {
                checkKnownParameters(p, "name");
                checkRequiredParameters(p, "name");

                final String user = Parameters.getValue(p, "name");

                userManager.removeUser(user);
                return ("User '" + user + "' removed").getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler recompute(Collection<Database> databases) {
        return (c, p) -> {
            try {
                checkKnownParameters(p);
                final ArrayList<byte[]> list=new ArrayList<>();
                for(var database:databases)
                    list.add(database.rebuild().getBytes());
                return Basic.concatenate(list);
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler getLog() {
        return (c, p) -> {
            try {
                checkKnownParameters(p);
                return Basic.getCollected().getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }

    static RequestHandler clearLog() {
        return (c, p) -> {
            try {
                checkKnownParameters(p);
                Basic.stopCollectingStdErr();
                Basic.startCollectionStdErr();
                return "Log cleared".getBytes();
            } catch (IOException ex) {
                return reportError(c, p, ex.getMessage());
            }
        };
    }
}
