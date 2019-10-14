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
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.gui.colors.ColorSchemeNucleotides;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 4.2012
 */
public class SetNucleotideColorSchemeCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set nucleotideColors=");

        String value = np.getWordMatchesIgnoringCase(Basic.toString(ColorSchemeNucleotides.getNames(), " "));
        np.matchIgnoreCase(";");

        AlignmentViewer viewer = (AlignmentViewer) getViewer();
        viewer.setNucleotideColoringScheme(value);
        // the following forces re-coloring:
        viewer.setShowAminoAcids(viewer.isShowAminoAcids());
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set nucleotideColors=[" + Basic.toString(ColorSchemeNucleotides.getNames(), "|") + "];";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        String choice = ProgramProperties.get("NucleotideColorScheme", ColorSchemeNucleotides.NAMES.Default.toString());
        String result = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Choose nucleotide color scheme", "Choose colors", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(),
                ColorSchemeNucleotides.getNames(), choice);
        if (result != null) {
            result = result.trim();
            if (result.length() > 0) {
                ProgramProperties.put("NucleotideColorScheme", result);
                execute("set nucleotideColors='" + result + "';");
            }
        }
    }

    private static final String NAME = "Set Nucleotide Colors...";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the coloring scheme for nucleotides";
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
        return true;
    }
}
