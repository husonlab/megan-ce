/*
 * DecontamDialog.java Copyright (C) 2022 Daniel H. Huson
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

package megan.fx.dialogs.decontam;

import javafx.application.Platform;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IViewerWithFindToolBar;
import jloda.swing.director.ProjectManager;
import jloda.swing.find.CompositeObjectSearcher;
import jloda.swing.find.FindToolBar;
import jloda.swing.find.SearchManager;
import jloda.swing.util.IViewerWithJComponent;
import jloda.swing.util.ProgramProperties;
import jloda.swing.util.ToolBar;
import jloda.swing.window.MenuBar;
import megan.core.Director;
import megan.dialogs.input.InputDialog;
import megan.fx.SwingPanel4FX;
import megan.main.MeganProperties;
import megan.samplesviewer.commands.PasteCommand;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DecontamDialog extends JFrame implements IDirectableViewer, IViewerWithJComponent, IViewerWithFindToolBar {
    private final Director dir;
    private boolean uptoDate = true;
    private boolean locked = false;

    private final MenuBar menuBar;
    private final CommandManager commandManager;
    private final JPanel mainPanel;
    private final ToolBar toolBar;

    private final SwingPanel4FX<DecontamDialogController> swingPanel4FX;

    private boolean showFindToolBar = false;
    private final SearchManager searchManager;
    private final CompositeObjectSearcher searcher;

    private final JFrame frame;

    private Runnable runOnDestroy;

    /**
     * constructor
     *
	 */
    public DecontamDialog(JFrame parent, final Director dir) {
        this.dir = dir;
        this.frame = this;
        commandManager = new CommandManager(dir, this, new String[]{"megan.commands", "megan6u.dialogs.parallelcoords.commands"}, !ProgramProperties.isUseGUI());

        setTitle();

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setIconImages(ProgramProperties.getProgramIconImages());

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        setJMenuBar(menuBar);

        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(mainPanel);
        toolBar = new ToolBar(this, GUIConfiguration.getToolBarConfiguration(), commandManager);
        frame.add(toolBar, BorderLayout.NORTH);

        searcher = new CompositeObjectSearcher("Columns", frame);
        searchManager = new SearchManager(dir, this, searcher, false, true);
        searchManager.getFindDialogAsToolBar().setEnabled(false);

        commandManager.updateEnableState();

        getFrame().addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                MainViewer.setLastActiveFrame(frame);
                commandManager.updateEnableState(PasteCommand.ALT_NAME);
                final InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null) {
                    inputDialog.setViewer(dir, DecontamDialog.this);
                }
            }
        });


        frame.setLocationRelativeTo(parent);

        setSize(650, 450);
        frame.setVisible(true);

        commandManager.updateEnableState();

        swingPanel4FX = new SwingPanel4FX<>(this.getClass());

        swingPanel4FX.runLaterInSwing(() -> {
            mainPanel.add(swingPanel4FX.getPanel(), BorderLayout.CENTER); // add panel once initialization complete
            mainPanel.validate();
            Platform.runLater(() -> ControlBindings.setup(swingPanel4FX.getController(), DecontamDialog.this, toolBar));
            Platform.runLater(() -> ControlBindings.updateScene(swingPanel4FX.getController(), DecontamDialog.this));
        });

    }

    public boolean isUptoDate() {
        return uptoDate;
    }

    public JFrame getFrame() {
        return frame;
    }

    public void updateView(String what) {
        uptoDate = false;
        setTitle();

        FindToolBar findToolBar = searchManager.getFindDialogAsToolBar();
        if (findToolBar.isClosing()) {
            showFindToolBar = false;
            findToolBar.setClosing(false);
        }
        if (!findToolBar.isEnabled() && showFindToolBar) {
            mainPanel.add(findToolBar, BorderLayout.NORTH);
            findToolBar.setEnabled(true);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        } else if (findToolBar.isEnabled() && !showFindToolBar) {
            mainPanel.remove(findToolBar);
            findToolBar.setEnabled(false);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        }
        if (findToolBar.isEnabled())
            findToolBar.clearMessage();
        uptoDate = true;
    }

    public void lockUserInput() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        searchManager.getFindDialogAsToolBar().setEnableCritical(false);
        locked = true;
        commandManager.setEnableCritical(false);
        menuBar.setEnableRecentFileMenuItems(false);
    }

    public void unlockUserInput() {
        setCursor(Cursor.getDefaultCursor());
        searchManager.getFindDialogAsToolBar().setEnableCritical(true);
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

    public void destroyView() {
        if (runOnDestroy != null)
            runOnDestroy.run();
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        dir.removeViewer(this);
        searchManager.getFindDialogAsToolBar().close();
        dispose();
    }

    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * set the title of the window
     */
    private void setTitle() {
        String newTitle = "Decontam - " + dir.getDocument().getTitle();

        /*
        if (dir.getDocument().isDirty())
            newTitle += "*";
          */

        if (dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + dir.getID() + "] - " + ProgramProperties.getProgramVersion();

        if (!frame.getTitle().equals(newTitle)) {
            frame.setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "DecontamDialog";
    }

    public Director getDir() {
        return dir;
    }

    @Override
    public boolean isShowFindToolBar() {
        return showFindToolBar;
    }

    @Override
    public void setShowFindToolBar(boolean show) {
        this.showFindToolBar = show;
    }

    @Override
    public SearchManager getSearchManager() {
        return searchManager;
    }

    @Override
    public JComponent getComponent() {
        return swingPanel4FX.getPanel();
    }

    public SwingPanel4FX<DecontamDialogController> getSwingPanel4FX() {
        return swingPanel4FX;
    }
}
