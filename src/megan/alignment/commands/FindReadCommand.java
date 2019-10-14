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
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.Lane;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * * selection command
 * * Daniel Huson, 4.2015
 */
public class FindReadCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("find read=");
        String regularExpression = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        Pattern pattern = Pattern.compile(regularExpression);

        AlignmentViewer viewer = (AlignmentViewer) getViewer();
        for (int row = 0; row < viewer.getAlignment().getNumberOfSequences(); row++) {
            Lane lane = viewer.getAlignment().getLane(row);
            Matcher matcher = pattern.matcher(lane.getName());
            if (matcher.find()) {
                final Alignment alignment = viewer.getAlignment();
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
        System.err.println("No match");
    }

    public String getSyntax() {
        return "find read=<regular-expression>;";
    }

    public void actionPerformed(ActionEvent event) {

        AlignmentViewer viewer = (AlignmentViewer) getViewer();
        String regularExpression = ProgramProperties.get(MeganProperties.FINDREAD, "");
        regularExpression = JOptionPane.showInputDialog(viewer.getFrame(), "Enter regular expression for read names:", regularExpression);
        if (regularExpression != null && regularExpression.trim().length() != 0) {
            regularExpression = regularExpression.trim();
            ProgramProperties.put(MeganProperties.FINDREAD, regularExpression);
            execute("find read='" + regularExpression + "';");
        }

    }

    public boolean isApplicable() {
        return true;
    }

    public static final String NAME = "Find Read...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Find and select a read using a regular expression";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Find16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
