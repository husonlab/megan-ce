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
package megan.groups;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.StatusBar;
import jloda.swing.util.ToolBar;
import jloda.swing.window.MenuBar;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.clusteranalysis.ClusterViewer;
import megan.core.Director;
import megan.core.SelectionSet;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Collection;
import java.util.Set;

/**
 * Viewer for selecting groups
 * Daniel Huson, 8.2014
 */
public class GroupsViewer implements IDirectableViewer, Printable {
    private boolean uptodate = true;
    private boolean locked = false;
    private final JFrame frame;
    private final StatusBar statusBar;
    private final Director dir;

    private final CommandManager commandManager;
    private final MenuBar menuBar;

    private final GroupsPanel groupsPanel;
    private final SelectionSet.SelectionListener selectionListener;

    private boolean ignoreNextUpdateAll = false;


    /**
     * constructor
     *
     * @param dir
     */
    public GroupsViewer(final Director dir, JFrame parent) {
        this.dir = dir;
        commandManager = new CommandManager(dir, this,
                new String[]{"megan.commands", "megan.groups.commands"}, !ProgramProperties.isUseGUI());

        frame = new JFrame();

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        frame.setJMenuBar(menuBar);
        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        setTitle();
        frame.setIconImages(ProgramProperties.getProgramIconImages());

        frame.setJMenuBar(menuBar);

        frame.getContentPane().setLayout(new BorderLayout());
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        frame.add(mainPanel, BorderLayout.CENTER);

        JToolBar toolBar = new ToolBar(this, GUIConfiguration.getToolBarConfiguration(), commandManager);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        groupsPanel = new GroupsPanel(dir.getDocument(), this);
        mainPanel.add(groupsPanel, BorderLayout.CENTER);

        selectionListener = (labels, selected) -> groupsPanel.selectSamples(labels, selected);
        dir.getDocument().getSampleSelection().addSampleSelectionListener(selectionListener);
        groupsPanel.setGroupsChangedListener(() -> {
            for (IDirectableViewer viewer : dir.getViewers()) {
                if (viewer instanceof ClusterViewer) {
                    ClusterViewer cv = (ClusterViewer) viewer;
                    if (cv.getPcoaTab() == cv.getSelectedComponent()) {
                        cv.updateView(IDirector.ALL);
                    }
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                MainViewer.setLastActiveFrame(frame);
                updateView(IDirector.ENABLE_STATE);
            }

            public void windowDeactivated(WindowEvent event) {
                Set<String> selected = groupsPanel.getSelectedSamples();
                if (selected.size() != 0) {
                    ProjectManager.getPreviouslySelectedNodeLabels().clear();
                    ProjectManager.getPreviouslySelectedNodeLabels().addAll(selected);
                }
            }


            public void windowClosing(WindowEvent e) {
                if (dir.getDocument().getProgressListener() != null)
                    dir.getDocument().getProgressListener().setUserCancelled(true);
                if (MainViewer.getLastActiveFrame() == frame)
                    MainViewer.setLastActiveFrame(null);
            }
        });


        statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        Dimension preferredSize = new Dimension(300, 700);
        frame.setSize(preferredSize);
        frame.setLocationRelativeTo(parent);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    public Director getDir() {
        return dir;
    }

    public JFrame getFrame() {
        return this.frame;
    }

    /**
     * gets the title
     *
     * @return title
     */
    public String getTitle() {
        return this.frame.getTitle();
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return this.uptodate;
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        dir.getDocument().getSampleSelection().removeSampleSelectionListener(selectionListener);

        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        dir.removeViewer(this);
        frame.dispose();
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        locked = true;
        commandManager.setEnableCritical(false);
        menuBar.setEnableRecentFileMenuItems(false);
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        this.uptodate = flag;
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        commandManager.setEnableCritical(true);
        menuBar.setEnableRecentFileMenuItems(true);
        locked = false;
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * ask view to rescan itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what what should be updated? Possible values: Director.ALL or Director.TITLE
     */
    public void updateView(String what) {
        this.uptodate = false;

        if (what.equals(IDirector.ALL)) {
            if (ignoreNextUpdateAll)
                ignoreNextUpdateAll = false;
            else
                groupsPanel.syncDocumentToPanel();
        }
        statusBar.setText2(String.format("Samples=%d Groups=%d", groupsPanel.getNumberOfSamples(), groupsPanel.getNumberOfGroups()));

        commandManager.updateEnableState();
        this.setTitle();
        this.uptodate = true;
    }


    /**
     * set the title of the window
     */
    private void setTitle() {
        String newTitle = "Sample Groups - " + this.dir.getDocument().getTitle();

        /*
        if (dir.getDocument().isDirty())
            newTitle += "*";
           */

        if (this.dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + this.dir.getID() + "] - " + ProgramProperties.getProgramVersion();

        if (!this.frame.getTitle().equals(newTitle)) {
            this.frame.setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    /**
     * gets the associated command manager
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }


    /**
     * Print the graph associated with this viewer.
     *
     * @param gc0        the graphics context.
     * @param format     page format
     * @param pagenumber page index
     */
    public int print(Graphics gc0, PageFormat format, int pagenumber) throws PrinterException {
        if (pagenumber == 0) {
            Graphics2D gc = ((Graphics2D) gc0);

            Dimension dim = frame.getContentPane().getSize();

            int image_w = dim.width;
            int image_h = dim.height;

            double paper_x = format.getImageableX() + 1;
            double paper_y = format.getImageableY() + 1;
            double paper_w = format.getImageableWidth() - 2;
            double paper_h = format.getImageableHeight() - 2;

            double scale_x = paper_w / image_w;
            double scale_y = paper_h / image_h;
            double scale = Math.min(scale_x, scale_y);

            double shift_x = paper_x + (paper_w - scale * image_w) / 2.0;
            double shift_y = paper_y + (paper_h - scale * image_h) / 2.0;

            gc.translate(shift_x, shift_y);
            gc.scale(scale, scale);

            gc.setStroke(new BasicStroke(1.0f));
            gc.setColor(Color.BLACK);

            frame.getContentPane().paint(gc);

            return Printable.PAGE_EXISTS;
        } else
            return Printable.NO_SUCH_PAGE;
    }

    public void setIgnoreNextUpdateAll(boolean ignoreNextUpdateAll) {
        this.ignoreNextUpdateAll = ignoreNextUpdateAll;
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "AttributesWindow";
    }

    public GroupsPanel getGroupsPanel() {
        return groupsPanel;
    }

    public void selectFromPrevious(Collection<String> previous) {
        getGroupsPanel().selectSamples(previous, true);
        ignoreNextUpdateAll = true; // update would clobber selection
    }
}
