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
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.Lane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

/**
 * select from previous window
 * Daniel Huson, 5.2015
 */
public class SelectFromPreviousWindowCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        AlignmentViewer viewer = (AlignmentViewer) getViewer();
        final Set<String> previousSelection = ProjectManager.getPreviouslySelectedNodeLabels();
        viewer.getSelectedBlock().clear();
        if (previousSelection.size() > 0) {
            final Alignment alignment = viewer.getAlignment();
            for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
                Lane lane = alignment.getLane(row);
                if (previousSelection.contains(Basic.getFirstWord(lane.getName()))) {
                    int firstJump = alignment.getGapColumnContractor().getTotalJumpBeforeLayoutColumn(lane.getFirstNonGapPosition());
                    int firstCol = lane.getFirstNonGapPosition() - firstJump;
                    int lastCol = lane.getLastNonGapPosition() - firstJump - 1;
                    row = alignment.getRowCompressor().getRow(row);
                    viewer.getSelectedBlock().select(row, firstCol, row, lastCol, alignment.isTranslate());
                    System.err.println("Found: " + lane.getName());
                    executeImmediately("zoom axis=both what=selection;");
                    return;
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
        return "select what=previous;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("select what=previous;");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getAltName() {
        return "From Previous Alignment";
    }

    public String getName() {
        return "From Previous Window";
    }

    public String getDescription() {
        return "Select from previous window";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Empty16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
