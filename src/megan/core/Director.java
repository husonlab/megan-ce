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
package megan.core;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.*;
import jloda.swing.message.MessageWindow;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ProgressDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.util.WindowUtilities;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * the megan director
 * Daniel Huson, 2005
 */
public class Director implements IDirectableViewer, IDirector {
    private int id;
    private MainViewer viewer;

    private final Document doc;
    private boolean docInUpdate = false;
    private final List<IDirectableViewer> viewers = new LinkedList<>();
    private final List<IDirectorListener> directorListeners = new LinkedList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private Future future;

    private boolean internalDocument = false; // will this remain hidden
    private IProjectsChangedListener projectsChangedListener;

    private boolean locked = false;

    /**
     * create a new director
     *
     * @param doc
     */
    public Director(Document doc) {
        this.doc = doc;
    }

    /**
     * add a viewer to this doc
     *
     * @param viewer
     */
    public IDirectableViewer addViewer(IDirectableViewer viewer) {
        if (viewer instanceof MainViewer)
            this.viewer = (MainViewer) viewer;

        viewers.add(viewer);
        directorListeners.add(viewer);
        ProjectManager.projectWindowChanged(this, viewer, true);
        return viewer;
    }

    /**
     * remove a viewer from this doc
     *
     * @param viewer
     */
    public void removeViewer(IDirectableViewer viewer) {
        viewers.remove(viewer);
        directorListeners.remove(viewer);
        ProjectManager.projectWindowChanged(this, viewer, false);

        if (viewers.isEmpty())
            ProjectManager.removeProject(this);
    }

    /**
     * returns the list of viewers
     *
     * @return viewers
     */
    public List<IDirectableViewer> getViewers() {
        return viewers;
    }

    /**
     * waits until all viewers are uptodate
     */
    private void WaitUntilAllViewersAreUptoDate() {

        while (!isAllViewersUptodate()) {
            try {
                Thread.sleep(10);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * returns true, if all viewers are uptodate
     *
     * @return true, if all viewers uptodate
     */
    private boolean isAllViewersUptodate() {
        for (IDirectableViewer viewer : viewers) {
            if (!viewer.isUptoDate()) {
                //System.err.println("not up-to-date: "+viewer.getTitle()+" "+viewer.getClass().get());
                return false;
            }
        }
        return true;
    }

    /**
     * notify listeners that viewers should be updated
     *
     * @param what what should be updated?
     */
    public void notifyUpdateViewer(final String what) {
        if (what.equals(TITLE) && MessageWindow.getInstance() != null) {
            MessageWindow.getInstance().setTitle("Messages - " + ProgramProperties.getProgramVersion());
        }
        synchronized (directorListeners) {
            for (IDirectorListener directorListener : directorListeners) {
                final IDirectorListener d = directorListener;
                final Runnable runnable = () -> {
                    try {
                        d.setUptoDate(false);
                        d.updateView(what);
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    } finally {
                        d.setUptoDate(true);
                    }
                };

                if (SwingUtilities.isEventDispatchThread())
                    runnable.run();
                else
                    SwingUtilities.invokeLater(runnable);
            }
        }
    }

    /**
     * notify listeners to prevent user input
     */
    public void notifyLockInput() {
        if (!locked) {
            synchronized (directorListeners) {
                IDirectorListener[] listeners = directorListeners.toArray(new IDirectorListener[0]);
                for (IDirectorListener directorListener : listeners) {
                    if (directorListener != this)
                        directorListener.lockUserInput();
                }
            }
            ProjectManager.updateWindowMenus();
        }
        locked = true;
    }

    /**
     * notify listeners to allow user input
     */
    public void notifyUnlockInput() {
        if (locked) {
            synchronized (directorListeners) {
                IDirectorListener[] listeners = directorListeners.toArray(new IDirectorListener[0]);
                for (IDirectorListener directorListener : listeners) {
                    if (directorListener != this)
                        directorListener.unlockUserInput();
                }
            }
            ProjectManager.updateWindowMenus();
        }
        locked = false;
    }

    /**
     * returns true, if currently locked
     *
     * @return locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * notify all director event listeners to destroy themselves
     */
    private void notifyDestroyViewer() throws CanceledException {

        synchronized (directorListeners) {
            while (directorListeners.size() > 0) { // need to do this in this way because  destroyView may modify this list
                IDirectorListener directorListener = directorListeners.get(0);
                if (directorListener != this)
                    directorListener.destroyView();
                if (directorListeners.size() > 0 && directorListeners.get(0) == directorListener)
                    directorListeners.remove(0);
            }
        }

        // now remove all viewers
        while (viewers.size() > 0) {
            removeViewer(viewers.get(0));
        }
        if (projectsChangedListener != null)
            ProjectManager.removeProjectsChangedListener(projectsChangedListener);


        if (future != null && !future.isDone()) {
            try {
                future.cancel(true);
            } catch (Exception ex) {
                // Basic.caught(ex);
            }
            future = null;
        }
    }

    /**
     * execute a command within the swing thread
     *
     * @param command
     */
    public boolean executeImmediately(final String command) {
        throw new RuntimeException("Internal error: OLD executeImmediately()");
    }

    /**
     * execute a command. Lock all viewer input, then request to doc to execute command
     *
     * @param command
     */
    public void execute(final String command) {
        throw new RuntimeException("Internal error: OLD execute()");
    }

    /**
     * execute a command within the swing thread
     *
     * @param command
     */
    public boolean executeImmediately(final String command, CommandManager commandManager) {
        System.err.println("Executing: " + command);
        try {

            if (doc.getProgressListener() == null) {
                ProgressListener progressListener = new ProgressPercentage();
                doc.setProgressListener(progressListener);
            }
            if (commandManager != null)
                commandManager.execute(command);
            else
                throw new Exception("Internal error: commandManager==null");
            if (viewer == null || !viewer.isLocked()) {
                notifyUpdateViewer(Director.ENABLE_STATE);
                WaitUntilAllViewersAreUptoDate();
                notifyUnlockInput();
            }
            return true;
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED EXECUTE");
            NotificationsInSwing.showInformation("USER CANCELED EXECUTE");

            return false;
        } catch (Exception ex) {
            NotificationsInSwing.showError("Command failed: " + ex.getMessage());
            return false;
        }
    }

    /**
     * execute a command. Lock all viewer input, then request to doc to execute command
     *
     * @param command
     */
    public void execute(final String command, final CommandManager commandManager) {
        if (ProgramProperties.isUseGUI()) {
            Component parentComponent;
            Object parent = commandManager.getParent();
            if (parent instanceof IDirectableViewer)
                parentComponent = ((IDirectableViewer) parent).getFrame();
            else if (parent instanceof JDialog)
                parentComponent = (JDialog) parent;
            else
                parentComponent = getParent();
            execute(command, commandManager, parentComponent);
        } else
            executeImmediately(command, commandManager);
    }

    /**
     * execute a command. Lock all viewer input, then request to doc to execute command
     *
     * @param command
     */
    public void execute(final String command, final CommandManager commandManager, final Component parent) {
        if (ProgramProperties.isUseGUI()) {
            if (command.length() > 1)
                System.err.println("Executing: " + command);

            if (docInUpdate) // shouldn't happen!
                System.err.println("Warning: execute(" + command + "): concurrent execution");
            notifyLockInput();

            future = executorService.submit(() -> {
                docInUpdate = true;
                // final ProgressListener progressDialog=new ProgressPercentage();
                final ProgressListener progressDialog = ProgramProperties.isUseGUI() ? new ProgressDialog("", "", parent) : new ProgressPercentage();
                progressDialog.setDebug(Basic.getDebugMode());
                doc.setProgressListener(progressDialog);
                long start = System.currentTimeMillis();
                boolean ok = false;
                try {
                    if (commandManager != null) {
                        commandManager.execute(command);
                        ok = true;
                    } else
                        throw new Exception("Internal error: commandManager==null");
                } catch (CanceledException ex) {
                    System.err.println("USER CANCELED EXECUTE");
                } catch (Exception ex) {
                    Basic.caught(ex);
                    NotificationsInSwing.showError("Execute failed: " + ex);
                }

                notifyUpdateViewer(Director.ALL);
                WaitUntilAllViewersAreUptoDate();
                notifyUnlockInput();

                doc.getProgressListener().close();
                doc.setProgressListener(null);

                docInUpdate = false;
                final int timeInSeconds = (int) ((System.currentTimeMillis() - start) / 1000);
                if (ok && timeInSeconds > 8) // if it took more than 8 seconds to complete, notify
                {
                    NotificationsInSwing.showInformation("Command completed (" + timeInSeconds + "s): " + command);
                }
            });
        } else
            executeImmediately(command, commandManager);
    }

    /**
     * returns the parent viewer
     *
     * @return viewer
     */
    private Component getParent() {
        if (viewer != null)
            return viewer.getFrame();
        else
            return null;
    }

    /**
     * returns a viewer of the given class
     *
     * @param aClass
     * @return viewer of the given class, or null
     */
    public IDirectableViewer getViewerByClass(Class aClass) {
        for (Object o : getViewers()) {
            IDirectableViewer viewer = (IDirectableViewer) o;
            if (viewer.getClass().equals(aClass))
                return viewer;
        }
        return null;
    }

    /**
     * returns a viewer of the given class
     *
     * @param aClass
     * @return viewer of the given class, or null
     */
    public <T> IDirectableViewer getViewerByAssignableFrom(Class<T> aClass) {
        for (Object o : getViewers()) {
            IDirectableViewer viewer = (IDirectableViewer) o;
            if (aClass.isAssignableFrom(viewer.getClass()))
                return viewer;
        }
        return null;
    }

    /**
     * returns a viewer by class name
     *
     * @param className
     * @return viewer that has the given className
     */
    public IDirectableViewer getViewerByClassName(String className) {
        for (IDirectableViewer viewer : getViewers()) {
            try {
                final Method method = viewer.getClass().getMethod("getClassName", (Class<?>[]) null);
                if (method != null) {
                    final String name = (String) method.invoke(viewer, (Object[]) null);
                    if (name.equals(className))
                        return viewer;
                }
            } catch (Exception ignored) {
            }
            if (viewer.getClass().getName().equals(className))
                return viewer;
        }
        return null;
    }


    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public Document getDocument() {
        return doc;
    }

    /**
     * close everything directed by this director
     */
    public void close() throws CanceledException {
        notifyDestroyViewer();
    }

    public void updateView(String what) {

    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return true;
    }

    /**
     * return the frame associated with the viewer
     *
     * @return frame
     */
    public JFrame getFrame() {
        return null;
    }

    /**
     * gets the title
     *
     * @return title
     */
    public String getTitle() {
        return doc.getTitle();
    }


    /**
     * get the main viewer
     *
     * @return main viewer
     */
    public MainViewer getMainViewer() {
        return viewer;
    }

    /**
     * are we currently updating the document?
     *
     * @return true, if are in rescan
     */
    public boolean isInUpdate() {
        return docInUpdate;
    }

    /**
     * open all necessary files at startup
     *
     * @param taxonomyFileName
     * @param meganFiles
     * @param initCommand
     */
    public void executeOpen(final String taxonomyFileName, final String[] meganFiles, final String initCommand) {
        System.err.println("Opening startup files");

        final CommandManager commandManager = getMainViewer().getCommandManager();

        if (docInUpdate) // shouldn't happen!
            System.err.println("Warning: executeOpen: concurrent execution");
        notifyLockInput();

        Runnable runnable = () -> {
            docInUpdate = true;
            ProgressListener progressDialog = ProgramProperties.isUseGUI() ? new ProgressDialog("Open startup files", "", getParent()) : new ProgressCmdLine("Open startup files", "");
            progressDialog.setDebug(Basic.getDebugMode());
            doc.setProgressListener(progressDialog);
            try {
                if (taxonomyFileName != null && taxonomyFileName.length() > 0) {
                    if (ResourceManager.fileExists(taxonomyFileName))
                        commandManager.execute("load taxonomyFile='" + taxonomyFileName + "';");
                    else
                        throw new IOException("Can't read taxonomy file: <" + taxonomyFileName + ">");
                }
                if (meganFiles != null && meganFiles.length > 0) {
                    StringBuilder buf = new StringBuilder();
                    for (String fileName : meganFiles) {
                        fileName = fileName.trim();
                        if (fileName.length() > 0) {
                            File file = new File(fileName);

                            if (file.canRead()) {
                                buf.append("open file='").append(fileName).append("';");
                            } else {
                                System.err.println("Warning: Can't read MEGAN file: '" + fileName + "'");
                            }
                        }
                    }
                    if (buf.toString().length() > 0)
                        commandManager.execute(buf.toString());
                }
                getMainViewer().setDoReInduce(true);
                getMainViewer().setDoReset(true);
                commandManager.execute(";");

                if (initCommand != null && initCommand.length() > 0) {
                    String[] tokens = initCommand.split(";");
                    for (String command : tokens) {
                        if (command.equalsIgnoreCase("quit"))
                            System.exit(0);
                        else
                            executeImmediately(command + ";", commandManager);
                    }
                }
            } catch (CanceledException ex) {
                System.err.println("USER CANCELED EXECUTE");
            } catch (Exception ex) {
                Basic.caught(ex);
                NotificationsInSwing.showError("Execute failed: " + ex);
            }

            if (TaxonomyData.getTree().getRoot() == null) {
                NotificationsInSwing.showError(viewer.getFrame(), "Initialization files not found. Please reinstall the program.");
                executeImmediately("quit;", commandManager);
            }
            notifyUpdateViewer(Director.ALL);
            WaitUntilAllViewersAreUptoDate();
            notifyUnlockInput();

            progressDialog.close();
            doc.setProgressListener(null);
            docInUpdate = false;
        };

        if (ProgramProperties.isUseGUI())
            future = executorService.submit(runnable);
        else
            runnable.run();
    }


    /**
     * set the dirty flag
     *
     * @param dirty
     */
    public void setDirty(boolean dirty) {
        getDocument().setDirty(dirty);
    }

    /**
     * get the dirty flag
     *
     * @return dirty
     */
    public boolean getDirty() {
        return getDocument().isDirty();
    }

    /**
     * gets a new director and makes the main viewer visible
     *
     * @return new director
     */
    public static Director newProject() {
        return newProject(true);
    }

    /**
     * gets a new director
     *
     * @param visible show main viewer be visible?
     * @return new director
     */
    public static Director newProject(boolean visible) {
        return newProject(visible, false);
    }

    /**
     * gets a new director
     *
     * @param visible show main viewer be visible?
     * @return new director
     */
    public static Director newProject(boolean visible, boolean internalDocument) {
        Document doc = new Document();
        Director dir = new Director(doc);
        dir.setInternalDocument(internalDocument);
        doc.setDir(dir);
        dir.setID(ProjectManager.getNextID());
        final MainViewer viewer;
        try {
            viewer = new MainViewer(dir, visible);
        } catch (Exception e) {
            Basic.caught(e);
            return null;
        }
        if (!dir.isInternalDocument()) {
            dir.projectsChangedListener = () -> viewer.getCommandManager().updateEnableState("Compare...");
            ProjectManager.addProjectsChangedListener(dir.projectsChangedListener);
        }
        ProjectManager.addProject(dir, viewer);
        return dir;
    }

    /**
     * show the message window
     */
    public static void showMessageWindow() {
        if (ProgramProperties.isUseGUI() && MessageWindow.getInstance() != null) {
            MessageWindow.getInstance().startCapturingOutput();
            WindowUtilities.toFront(MessageWindow.getInstance().getFrame());
        }
    }

    /**
     * gets the command manager associated with the main viewer
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return getMainViewer().getCommandManager();
    }

    @Override
    public boolean isInternalDocument() {
        return internalDocument;
    }

    @Override
    public void setInternalDocument(boolean internalDocument) {
        this.internalDocument = internalDocument;
    }

    /**
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "Director";
    }
}
