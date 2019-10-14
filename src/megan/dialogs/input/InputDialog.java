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
package megan.dialogs.input;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.RememberingComboBox;
import jloda.swing.window.MenuBar;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Command input window
 * Daniel Huson, 5.2011
 */
public class InputDialog extends JFrame implements IDirectableViewer {
    private boolean uptoDate = true;
    private IDirectableViewer viewer;
    private IDirector dir;
    private CommandManager commandManager;
    private final MenuBar menuBar;
    private final RememberingComboBox inputCBox;
    private final AbstractAction applyAction;
    private final JLabel commandContextLabel;

    private static InputDialog instance = null;

    private boolean isLocked = false;

    /**
     * constructor
     *
     * @param viewer
     */
    public InputDialog(IDirector dir0, IDirectableViewer viewer) {
        this.viewer = viewer;
        this.dir = dir0;
        commandManager = viewer.getCommandManager();

        setTitle();

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        setJMenuBar(menuBar);
        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(null, menuBar.getWindowMenu());

        setSize(600, 120);
        setMaximumSize(new Dimension(2000, 120));
        setLocationRelativeTo(viewer.getFrame());

        inputCBox = new RememberingComboBox();

        inputCBox.addItemsFromString(ProgramProperties.get(ProgramProperties.LASTCOMMAND, ""), "%%%");

        final JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        topPanel.add(new JLabel("Enter command to execute:"), BorderLayout.NORTH);

        inputCBox.setAction(new AbstractAction("Command input") {
            public void actionPerformed(ActionEvent event) {
                updateView(IDirector.ALL);
            }
        });
        //JScrollPane scrollPane=new JScrollPane(inputCBox);
        //scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) ;
        //scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER) ;
        topPanel.add(inputCBox, BorderLayout.CENTER);

        getContentPane().add(topPanel);

        final JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        commandContextLabel = new JLabel();
        commandContextLabel.setForeground(Color.LIGHT_GRAY);
        bottomPanel.add(commandContextLabel);
        bottomPanel.add(Box.createHorizontalGlue());

        applyAction = new AbstractAction("Apply") {
            public void actionPerformed(ActionEvent actionEvent) {
                inputCBox.getCurrentText(true);
                String command = getCommand();
                if (command != null && command.length() > 0) {
                    if (!command.startsWith("!"))
                        dir.executeImmediately("show window=message;", getCommandManager());
                    else
                        command = command.substring(1);
                    if (!command.endsWith(";"))
                        command += ";";
                    command = command.trim();
                    if (command.length() > 0) {
                        ProgramProperties.put(ProgramProperties.LASTCOMMAND, inputCBox.getItemsAsString(20, "%%%"));
                        dir.execute(command, getCommandManager());
                    }
                }
            }
        };

        bottomPanel.add(new JButton(new AbstractAction("Close") {
            public void actionPerformed(ActionEvent actionEvent) {
                InputDialog.this.setVisible(false);
            }
        }));

        final JButton applyButton = new JButton(applyAction);
        bottomPanel.add(applyButton);
        bottomPanel.add(Box.createHorizontalStrut(20));
        getRootPane().setDefaultButton(applyButton);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        getFrame().addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                if (dir.getMainViewer().isLocked())
                    lockUserInput();
                else {
                    updateView(IDirector.ALL);
                    unlockUserInput();
                }
                getFrame().requestFocusInWindow();
            }
        });
        setViewer(dir, viewer);
        updateView(IDirector.ALL);
        setVisible(true);
    }

    public boolean isUptoDate() {
        return uptoDate;
    }

    public JFrame getFrame() {
        return this;
    }

    public void updateView(String what) {
        setTitle();
        commandManager.updateEnableState();
    }

    public void lockUserInput() {
        isLocked = true;
        applyAction.setEnabled(false);
        commandManager.setEnableCritical(false);
    }

    public void unlockUserInput() {
        applyAction.setEnabled(true);
        commandManager.setEnableCritical(true);
        isLocked = false;
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return isLocked;
    }

    public void destroyView() throws CanceledException {
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        setVisible(false);
        dispose();
    }

    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * set the title of the window
     */
    private void setTitle() {
        String newTitle = "Command input  - " + viewer.getTitle();

        if (!getFrame().getTitle().equals(newTitle)) {
            getFrame().setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * get the current command
     *
     * @return command
     */
    private String getCommand() {
        return inputCBox.getCurrentText(false).trim();
    }


    public static InputDialog getInstance() {
        return instance;
    }

    public static void setInstance(InputDialog instance) {
        InputDialog.instance = instance;
    }

    public void setViewer(IDirector dir, IDirectableViewer viewer) {
        this.dir = dir;
        this.viewer = viewer;
        this.commandManager = viewer.getCommandManager();
        setTitle();
        commandContextLabel.setText("Context=" + viewer.getClassName());
        if (this.viewer.isLocked())
            lockUserInput();
    }

    /**
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "InputDialog";
    }
}
