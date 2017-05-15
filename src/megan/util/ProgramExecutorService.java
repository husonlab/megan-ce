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

package megan.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * program executor service
 * all concurrent services should use this executor service
 * Created by huson on 12/11/16.
 */
public class ProgramExecutorService {
    private static ExecutorService executorService;

    /**
     * setup a program wide executor service
     *
     * @param numberOfThreads
     */
    public static void setup(int numberOfThreads) {
        executorService = Executors.newFixedThreadPool(numberOfThreads);
    }

    /**
     * get the program wide executor service
     *
     * @return executor service
     */
    public static ExecutorService getExecutorService() {
        if (executorService == null)
            setup(4);
        return executorService;
    }
}