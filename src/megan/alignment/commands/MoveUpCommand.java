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
package megan.alignment.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.gui.AlignmentSorter;
import megan.alignment.gui.SelectedBlock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class MoveUpCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("move dir=");
        String dir = np.getWordMatchesIgnoringCase("up down");
        AlignmentViewer viewer = (AlignmentViewer) getViewer();
        SelectedBlock selectedBlock = viewer.getSelectedBlock();

        if (selectedBlock.isSelected()) {
            if (dir.equals("up")) {
                if ((viewer.isShowAsMapping() && viewer.getAlignment().getRowCompressor().moveUp(selectedBlock.getFirstRow(), selectedBlock.getLastRow()))
                        || (!viewer.isShowAsMapping() && AlignmentSorter.moveUp(viewer.getAlignment(), selectedBlock.getFirstRow(), selectedBlock.getLastRow()))) {
                    selectedBlock.setFirstRow(selectedBlock.getFirstRow() - 1);
                    selectedBlock.setLastRow(selectedBlock.getLastRow() - 1);
                    selectedBlock.fireSelectionChanged();
                }
            } else if (dir.equals("down")) {
                if ((viewer.isShowAsMapping() && viewer.getAlignment().getRowCompressor().moveDown(selectedBlock.getFirstRow(), selectedBlock.getLastRow()))
                        || (!viewer.isShowAsMapping() && AlignmentSorter.moveDown(viewer.getAlignment(), selectedBlock.getFirstRow(), selectedBlock.getLastRow()))) {
                    selectedBlock.setFirstRow(selectedBlock.getFirstRow() + 1);
                    selectedBlock.setLastRow(selectedBlock.getLastRow() + 1);
                    selectedBlock.fireSelectionChanged();
                }
            }
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "move dir=<up|down>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("move dir=up;");
    }

    public static final String NAME = "Move Up";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Move selected sequences up";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Up16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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
        AlignmentViewer viewer = (AlignmentViewer) getViewer();
        SelectedBlock selectedBlock = viewer.getSelectedBlock();

        return viewer.getAlignment().getNumberOfSequences() > 0 && selectedBlock.isSelected() && selectedBlock.getFirstRow() > 0;
    }
}
