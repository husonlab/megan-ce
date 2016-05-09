/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.commands;


import jloda.graphview.GraphView;
import jloda.gui.commands.ICommand;
import jloda.gui.director.ProjectManager;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * close the window
 * Daniel Huson, 6.2010
 */
public class CloseCommand extends CommandBase implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return NAME;
    }

    public static final String NAME = "Close";

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Close the window";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Close16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        if (getViewer() instanceof MainViewer) {
            if (getViewer().isLocked()) {
                if (ProgramProperties.isUseGUI()) {
                    int result = JOptionPane.showConfirmDialog(getViewer().getFrame(), "Process running, close anyway?", "Close?", JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION)
                        return;
                } else {
                    System.err.println("Internal error: process running, close request ignored");
                    return; // todo: could cause problems
                }
            }
            if (ProjectManager.getNumberOfProjects() == 1)
                executeImmediately("quit;");
            else {
                try {
                    if (getDir().getDocument().getProgressListener() != null)
                        getDir().getDocument().getProgressListener().setUserCancelled(true);
                    getDir().close();
                } catch (CanceledException ex) {
                    //Basic.caught(ex);
                }
            }
        } else if (getViewer() != null) {
            getViewer().destroyView();

        } else if (getParent() instanceof GraphView) {
            ((GraphView) getParent()).getFrame().setVisible(false);
        }


    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately(getSyntax());
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return false;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "close;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
