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
import megan.alignment.gui.Alignment;
import megan.alignment.gui.Lane;
import megan.alignment.gui.SelectedBlock;
import megan.core.Director;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class ShowMatchCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return null;
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        Director.showMessageWindow();
        final AlignmentViewer viewer = (AlignmentViewer) getViewer();
        final Alignment alignment = viewer.getAlignment();
        final SelectedBlock block = viewer.getSelectedBlock();

        for (int row = 0; row < alignment.getRowCompressor().getNumberRows(); row++) {
            if (block.isSelectedRow(row)) {
                for (Integer read : alignment.getRowCompressor().getCompressedRow2Reads(row)) {
                    Lane lane = alignment.getLane(read);
                    int firstJump = alignment.getGapColumnContractor().getTotalJumpBeforeLayoutColumn(block.getFirstCol());
                    if ((block.isSelectedCol(lane.getFirstNonGapPosition() - firstJump + 1) /* && block.isSelectedCol(lane.getLastNonGapPosition() - firstJump) */)) {
                        System.out.println();
                        System.out.println(viewer.getSelectedReference());
                        System.out.println(lane.getText());
                    }
                }
            }
        }
    }

    final public static String NAME = "Show Match...";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Show the match for a given read";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/History16.gif");
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
        return ((AlignmentViewer) getViewer()).getSelectedBlock().isSelected();
    }
}
