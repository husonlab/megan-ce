/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.remote;

import java.util.List;

/**
 * Represents a (remote) MEGAN server
 * <p/>
 * Daniel Huson on 12/2014
 */
public interface IRemoteService {

    /**
     * get the remote server URL and directory path, e.g. www.megan.de/data/files
     *
     * @return file specification
     */
    String getServerURL();

    /**
     * get short server name, e.g. megan.de:files
     *
     * @return short name
     */
    String getShortName();


    /**
     * is service available
     *
     * @return
     */
    boolean isAvailable();

    /**
     * get a list of available files (relative names)
     *
     * @return list of available files
     */
    List<String> getAvailableFiles();

    /**
     * gets the server and file name
     *
     * @param file
     * @return server and file
     */
    String getServerAndFileName(String file);

    /**
     * gets the info string for a server
     *
     * @return info in html
     */
    String getInfo();

    /**
     * get the description associated with a given file name
     *
     * @param fileName
     * @return description
     */
    String getDescription(String fileName);
}
