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
package megan.dialogs.compare.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.dialogs.compare.CompareWindow;
import megan.dialogs.compare.Comparer;
import megan.dialogs.compare.MyListItem;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class ApplyCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        CompareWindow viewer = (CompareWindow) getParent();
        viewer.setCanceled(false);
        viewer.setVisible(false);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "apply;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately(getSyntax());
    }

    public static final String NAME = "Apply";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Create the comparison in a new window";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        if (getParent() != null && getParent() instanceof CompareWindow) {
            final CompareWindow viewer = (CompareWindow) getParent();
            if (viewer.getJList().getSelectedValuesList().size() >= 1) {
                if (viewer.getMode() == Comparer.COMPARISON_MODE.RELATIVE)
                    return true;
                Document.ReadAssignmentMode readAssignmentMode = null;
                for (Object obj : viewer.getJList().getSelectedValuesList()) {
                    final MyListItem myListItem = (MyListItem) obj;
                    if (readAssignmentMode == null)
                        readAssignmentMode = myListItem.getReadAssignmentMode();
                    else if (readAssignmentMode != myListItem.getReadAssignmentMode())
                        return false;
                }
                return true;
            }
        }
        return false;
    }
}
