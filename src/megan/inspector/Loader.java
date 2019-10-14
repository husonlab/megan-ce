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
package megan.inspector;

import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ProgressDialog;
import jloda.util.Basic;
import jloda.util.CanceledException;
import megan.core.Director;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * load data into node and then a node into the inspector
 * Daniel Huson, 9.2009
 */
public class Loader {
    private final InspectorWindow inspectorWindow;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private Future future;

    private final Queue<LoaderTask> tasks = new LinkedList<>();

    /**
     * constructor
     *
     * @param inspectorWindow
     */
    public Loader(InspectorWindow inspectorWindow) {
        this.inspectorWindow = inspectorWindow;
    }

    /**
     * execute a loader task. Either immediately, if nothing is running, or later
     *
     * @param task
     */
    public void execute(final LoaderTask task) {
        tasks.add(task);
        processQueue();
    }

    /**
     * runs all loader tasks in queue
     */
    private void processQueue() {
        final Director dir = inspectorWindow.dir;
        if (!dir.isLocked() && (future == null || future.isDone())) {
            future = executorService.submit(() -> {
                ProgressDialog progressDialog = new ProgressDialog("", "", inspectorWindow.getFrame());
                dir.getDocument().setProgressListener(progressDialog);
                try {
                    dir.notifyLockInput();
                    progressDialog.setTasks("Loading data from file", inspectorWindow.dir.getDocument().getMeganFile().getName());
                    progressDialog.setDebug(Basic.getDebugMode());
                    while (tasks.size() > 0) {
                        final LoaderTask task = tasks.remove();
                        // System.err.println("Task: " + task);
                        try {
                            task.run(progressDialog);
                        } catch (CanceledException ex) {
                            // System.err.println("USER CANCELED EXECUTE");
                        } catch (Exception ex) {
                            Basic.caught(ex);
                            NotificationsInSwing.showError(inspectorWindow.getFrame(), "Load data failed: " + ex.getMessage());
                        }
                    }
                } finally {
                    dir.getDocument().getProgressListener().close();
                    dir.notifyUnlockInput();
                    inspectorWindow.updateView(Director.ALL);
                }
            });
        }
    }
}
