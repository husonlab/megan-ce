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
package megan.dialogs.extractor;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.StatusBar;
import jloda.swing.window.MenuBar;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.commands.CloseCommand;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.dialogs.extractor.commands.ApplyCommand;
import megan.dialogs.extractor.commands.ChooseDirectoryCommand;
import megan.dialogs.extractor.commands.SetIncludeSummarizedCommand;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.*;

/**
 * extract reads dialog
 * Daniel Huson, 6.2007 (infight from Dubai to Christchurch)
 */
public class ExtractReadsViewer extends JFrame implements IDirectableViewer {
    private final Director dir;
    private boolean uptoDate;
    private boolean locked = false;
    private String mode = ClassificationType.Taxonomy.toString();

    private final AbstractButton closeButton;

    private boolean includeSummarized = false;

    private final JTextField outDirectory;
    private final JTextField outFileTemplate;

    private final MenuBar menuBar;

    private final CommandManager commandManager;

    /**
     * constructor
     *
     * @param dir
     */
    public ExtractReadsViewer(JFrame parent, final Director dir) {
        this.dir = dir;

        commandManager = new CommandManager(dir, this,
                new String[]{"megan.commands", "megan.dialogs.extractor.commands"}, !ProgramProperties.isUseGUI());

        setTitle();

        setSize(500, 165);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setIconImages(ProgramProperties.getProgramIconImages());

        StatusBar statusBar = new StatusBar(false);

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        setJMenuBar(menuBar);
        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        outDirectory = new JTextField();
        outDirectory.setText(ProgramProperties.get(MeganProperties.EXTRACT_OUTFILE_DIR, System.getProperty("user.home")));
        outFileTemplate = new JTextField();
        outFileTemplate.setText(ProgramProperties.get(MeganProperties.EXTRACT_OUTFILE_TEMPLATE, "reads-%t.fasta"));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel topMiddlePanel = new JPanel();
        topMiddlePanel.setLayout(new BoxLayout(topMiddlePanel, BoxLayout.Y_AXIS));

        JPanel middlePanel = new JPanel();
        middlePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Output files"));

        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));
        JPanel m1 = new JPanel();
        m1.setLayout(new BorderLayout());
        m1.add(new JLabel("Directory:"), BorderLayout.WEST);
        m1.add(outDirectory, BorderLayout.CENTER);
        outDirectory.setToolTipText("Set directory for files to be written to");
        m1.add(commandManager.getButton(ChooseDirectoryCommand.ALTNAME), BorderLayout.EAST);
        middlePanel.add(m1);

        JPanel m2 = new JPanel();
        m2.setLayout(new BorderLayout());
        m2.add(new JLabel("File name:"), BorderLayout.WEST);
        m2.add(outFileTemplate, BorderLayout.CENTER);
        outFileTemplate.setToolTipText("Set name of file to save to. Any occurrence of %t, or %i, will be replaced by the name or id of the node, respectively");
        middlePanel.add(m2);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(commandManager.getButton(SetIncludeSummarizedCommand.NAME));

        middlePanel.add(buttons);

        topMiddlePanel.add(middlePanel);

        mainPanel.add(topMiddlePanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        bottomPanel.setLayout(new BorderLayout());

        bottomPanel.add(statusBar, BorderLayout.CENTER);

        JPanel b1 = new JPanel();
        b1.setLayout(new BoxLayout(b1, BoxLayout.X_AXIS));
        closeButton = commandManager.getButton(CloseCommand.NAME);
        b1.add(closeButton);
        b1.add(commandManager.getButton(ApplyCommand.NAME));

        bottomPanel.add(b1, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().validate();

        commandManager.updateEnableState();
        getFrame().setLocationRelativeTo(parent);
    }

    public boolean isUptoDate() {
        return uptoDate;
    }

    public JFrame getFrame() {
        return this;
    }

    public void updateView(String what) {
        uptoDate = false;
        setTitle();
        commandManager.updateEnableState();
        uptoDate = true;
    }

    public void lockUserInput() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        locked = true;
        commandManager.setEnableCritical(false);
        closeButton.setEnabled(false);
        menuBar.setEnableRecentFileMenuItems(false);
    }

    public void unlockUserInput() {
        setCursor(Cursor.getDefaultCursor());
        closeButton.setEnabled(true);
        commandManager.setEnableCritical(true);
        locked = false;
        menuBar.setEnableRecentFileMenuItems(true);
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return locked;
    }

    public void destroyView() throws CanceledException {
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        dir.removeViewer(this);
        dispose();
    }

    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * set the title of the window
     */
    private void setTitle() {
        String newTitle = "Extract by " + getMode() + " - " + dir.getDocument().getTitle();

        /*
        if (dir.getDocument().isDirty())
            newTitle += "*";
          */

        if (dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + dir.getID() + "] - " + ProgramProperties.getProgramVersion();

        if (!getFrame().getTitle().equals(newTitle)) {
            getFrame().setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
        updateView(IDirector.ALL);
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public boolean isIncludeSummarized() {

        return !mode.equals("Taxonomy") || includeSummarized;
    }

    public void setIncludeSummarized(boolean includeSummarized) {
        this.includeSummarized = includeSummarized;
    }


    public JTextField getOutDirectory() {
        return outDirectory;
    }

    public JTextField getOutFileTemplate() {
        return outFileTemplate;
    }

    public MenuBar getTheMenuBar() {
        return menuBar;
    }


    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "ExtractorViewer";
    }

}
