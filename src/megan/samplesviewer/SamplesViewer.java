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
package megan.samplesviewer;

import javafx.application.Platform;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IViewerWithFindToolBar;
import jloda.swing.director.ProjectManager;
import jloda.swing.find.FindToolBar;
import jloda.swing.find.SearchManager;
import jloda.swing.util.StatusBar;
import jloda.swing.util.ToolBar;
import jloda.swing.window.MenuBar;
import jloda.swing.window.MenuConfiguration;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.core.Director;
import megan.core.Document;
import megan.core.SampleAttributeTable;
import megan.core.SelectionSet;
import megan.dialogs.input.InputDialog;
import megan.fx.CommandManagerFX;
import megan.main.MeganProperties;
import megan.samplesviewer.commands.PasteCommand;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Viewer for working with metadata
 * Daniel Huson, 9.2012
 */
public class SamplesViewer implements IDirectableViewer, IViewerWithFindToolBar {
    private boolean uptodate = true;
    private boolean locked = false;

    private final JFrame frame;
    private final JPanel mainPanel;
    private final StatusBar statusbar;

    private final Director dir;
    private final Document doc;

    private final SelectionSet.SelectionListener selectionListener;

    private boolean showFindToolBar = false;
    private boolean showReplaceToolBar = false;

    private final SearchManager searchManager;

    private final CommandManagerFX commandManager;
    private final MenuBar menuBar;

    private final Set<String> needToReselectSamples = new HashSet<>();

    private final SamplesTableView sampleTableView;

    /**
     * constructor
     *
     * @param dir
     */
    public SamplesViewer(final Director dir) {
        this.dir = dir;
        this.doc = dir.getDocument();

        frame = new JFrame("Sample viewer");
        frame.getContentPane().setLayout(new BorderLayout());

        frame.setLocationRelativeTo(dir.getMainViewer().getFrame());
        final int[] geometry = ProgramProperties.get("SampleViewerGeometry", new int[]{100, 100, 800, 600});
        frame.setSize(geometry[2], geometry[3]);

        statusbar = new StatusBar();

        this.commandManager = new CommandManagerFX(dir, this, new String[]{"megan.commands", "megan.samplesviewer.commands"}, !ProgramProperties.isUseGUI());

        String toolBarConfig = megan.samplesviewer.GUIConfiguration.getToolBarConfiguration();
        JToolBar toolBar = new ToolBar(this, toolBarConfig, commandManager);
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);

        sampleTableView = new SamplesTableView(this);
        mainPanel.add(sampleTableView.getPanel());

        frame.getContentPane().add(statusbar, BorderLayout.SOUTH);

        MenuConfiguration menuConfig = GUIConfiguration.getMenuConfiguration();

        TableViewSearcher tableViewSearcher = new TableViewSearcher(sampleTableView.getTableView());
        searchManager = new SearchManager(dir, this, tableViewSearcher, false, true);

        this.menuBar = new MenuBar(this, menuConfig, getCommandManager());
        frame.setJMenuBar(menuBar);

        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        frame.setIconImages(ProgramProperties.getProgramIconImages());

        setWindowTitle();
        // add window listeners
        frame.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                componentResized(e);
            }

            public void componentResized(ComponentEvent event) {
                if ((event.getID() == ComponentEvent.COMPONENT_RESIZED || event.getID() == ComponentEvent.COMPONENT_MOVED)
                        && (getFrame().getExtendedState() & JFrame.MAXIMIZED_HORIZ) == 0
                        && (getFrame().getExtendedState() & JFrame.MAXIMIZED_VERT) == 0) {
                    ProgramProperties.put("SampleViewerGeometry", new int[]{frame.getLocation().x, frame.getLocation().y, frame.getSize().width,
                            frame.getSize().height});
                }
            }
        });

        getFrame().addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                MainViewer.setLastActiveFrame(frame);
                commandManager.updateEnableState(PasteCommand.ALT_NAME);
                final InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null) {
                    inputDialog.setViewer(dir, SamplesViewer.this);
                }
            }

            public void windowDeactivated(WindowEvent event) {
                List<String> list = new ArrayList<>();
                list.addAll(sampleTableView.getSelectedAttributes());
                list.addAll(sampleTableView.getSelectedSamples());
                if (list.size() != 0) {
                    ProjectManager.getPreviouslySelectedNodeLabels().clear();
                    ProjectManager.getPreviouslySelectedNodeLabels().addAll(list);
                }
            }

            public void windowClosing(WindowEvent e) {
                if (dir.getDocument().getProgressListener() != null)
                    dir.getDocument().getProgressListener().setUserCancelled(true);
                if (MainViewer.getLastActiveFrame() == frame)
                    MainViewer.setLastActiveFrame(null);
            }
        });

        selectionListener = (labels, selected) -> Platform.runLater(() -> sampleTableView.selectSamples(labels, selected));
        doc.getSampleSelection().addSampleSelectionListener(selectionListener);

        frame.setVisible(true);
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return uptodate;
    }

    /**
     * return the frame associated with the viewer
     *
     * @return frame
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * gets the title
     *
     * @return title
     */
    public String getTitle() {
        return getFrame().getTitle();
    }

    /**
     * gets the associated command manager
     *
     * @return command manager
     */
    public CommandManagerFX getCommandManager() {
        return commandManager;
    }

    public Director getDir() {
        return dir;
    }

    /**
     * ask view to rescan itself
     *
     * @param what what should be updated? Possible values: Director.ALL or Director.TITLE
     */
    public void updateView(final String what) {
        // if (!SwingUtilities.isEventDispatchThread() && !Platform.isFxApplicationThread())
        //     System.err.println("updateView(): not in Swing or FX thread!");

        if (what.equals(Director.ALL)) {
            sampleTableView.syncFromDocumentToView();
        }

        final FindToolBar findToolBar = searchManager.getFindDialogAsToolBar();
        if (findToolBar.isClosing()) {
            showFindToolBar = false;
            showReplaceToolBar = false;
            findToolBar.setClosing(false);
        }
        if (!findToolBar.isEnabled() && showFindToolBar) {
            mainPanel.add(findToolBar, BorderLayout.NORTH);
            findToolBar.setEnabled(true);
            if (showReplaceToolBar)
                findToolBar.setShowReplaceBar(true);
            frame.getContentPane().validate();
        } else if (findToolBar.isEnabled() && !showFindToolBar) {
            mainPanel.remove(findToolBar);
            findToolBar.setEnabled(false);
            frame.getContentPane().validate();
        }
        if (findToolBar.isEnabled()) {
            findToolBar.clearMessage();
            searchManager.updateView(Director.ENABLE_STATE);
            if (findToolBar.isShowReplaceBar() != showReplaceToolBar)
                findToolBar.setShowReplaceBar(showReplaceToolBar);
        }

        if (!doc.isDirty() && sampleTableView.isDirty())
            doc.setDirty(true);

        getCommandManager().updateEnableStateSwingItems();
        javafx.application.Platform.runLater(() -> getCommandManager().updateEnableStateFXItems());

        setWindowTitle();
        frame.setCursor(Cursor.getDefaultCursor());
        updateStatusBar();
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        locked = true;
        statusbar.setText1("");
        statusbar.setText2("Busy...");
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getCommandManager().setEnableCritical(false);
        searchManager.getFindDialogAsToolBar().setEnableCritical(false);
        sampleTableView.lockUserInput();
        menuBar.setEnableRecentFileMenuItems(false);
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        sampleTableView.unlockUserInput();
        getCommandManager().setEnableCritical(true);
        frame.setCursor(Cursor.getDefaultCursor());
        searchManager.getFindDialogAsToolBar().setEnableCritical(true);
        frame.setCursor(Cursor.getDefaultCursor());
        updateStatusBar();
        menuBar.setEnableRecentFileMenuItems(true);
        locked = false;
    }

    /**
     * rescan the status bar
     */
    private void updateStatusBar() {
        SampleAttributeTable sampleAttributeTable = doc.getSampleAttributeTable();
        String message = "Samples=" + sampleAttributeTable.getNumberOfSamples();
        message += " Attributes=" + sampleAttributeTable.getNumberOfUnhiddenAttributes();
        if (getSamplesTableView().getCountSelectedSamples() > 0 || getSamplesTableView().getCountSelectedAttributes() > 0) {
            message += " (Selection: " + getSamplesTableView().getCountSelectedSamples() + " samples, " + getSamplesTableView().getCountSelectedAttributes() + " attributes)";
        }
        statusbar.setText2(message);
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
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        locked = true;
        ProgramProperties.put("SampleViewerGeometry", new int[]{frame.getLocation().x, frame.getLocation().y, frame.getSize().width, frame.getSize().height});
        frame.setVisible(false);

        searchManager.getFindDialogAsToolBar().close();

        doc.getSampleSelection().removeSampleSelectionListener(selectionListener);

        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        frame.setVisible(false);

        dir.removeViewer(this);
        //frame.dispose();
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptodate = flag;
    }

    public boolean isShowFindToolBar() {
        return showFindToolBar;
    }

    public void setShowFindToolBar(boolean show) {
        showFindToolBar = show;
    }

    public boolean isShowReplaceToolBar() {
        return showReplaceToolBar;
    }

    public void setShowReplaceToolBar(boolean showReplaceToolBar) {
        this.showReplaceToolBar = showReplaceToolBar;
        if (showReplaceToolBar)
            showFindToolBar = true;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "SamplesViewer";
    }

    /**
     * sets the title of the window
     */
    public void setWindowTitle() {
        String newTitle = "SamplesViewer - " + dir.getDocument().getTitle();

        if (doc.getMeganFile().isMeganServerFile())
            newTitle += " (remote file)";
        if (doc.getMeganFile().isReadOnly())
            newTitle += " (read-only)";
        else if (doc.isDirty())
            newTitle += "*";

        if (dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + dir.getID() + "] - " + ProgramProperties.getProgramVersion();

        if (!getTitle().equals(newTitle)) {
            frame.setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }


    public SampleAttributeTable getSampleAttributeTable() {
        return dir.getDocument().getSampleAttributeTable();
    }

    public Document getDocument() {
        return dir.getDocument();
    }

    public Set<String> getNeedToReselectSamples() {
        return needToReselectSamples;
    }

    public SamplesTableView getSamplesTableView() {
        return sampleTableView;
    }

    /**
     * execute a command
     *
     * @param command
     */
    public void execute(String command) {
        dir.execute(command, getCommandManager());
    }

    /**
     * execute a command
     *
     * @param command
     */
    public void executeImmediately(String command) {
        dir.executeImmediately(command, getCommandManager());
    }
}
