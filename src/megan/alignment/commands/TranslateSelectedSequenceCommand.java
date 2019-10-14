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
import jloda.swing.window.NotificationsInSwing;
import jloda.util.SequenceUtils;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.gui.Alignment;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * translate selected sequence command
 * Daniel Huson, 11.2011
 */
public class TranslateSelectedSequenceCommand extends CommandBase implements ICommand {
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

        String fastA = viewer.getAlignmentViewerPanel().getSelectedAlignment();
        if (fastA != null) {
            StringWriter w = new StringWriter();
            BufferedReader r = new BufferedReader(new StringReader(fastA));
            String aLine;
            while ((aLine = r.readLine()) != null) {
                aLine = aLine.trim();
                if (aLine.startsWith(">"))
                    w.write(aLine + "\n");
                else {
                    for (int i = 0; i < aLine.length() - 2; i += 3) {
                        w.write(SequenceUtils.getAminoAcid(aLine.charAt(i), aLine.charAt(i + 1), aLine.charAt(i + 2)));
                    }
                    w.write("\n");
                }
            }
            System.out.println(w.toString());
            NotificationsInSwing.showInformation(viewer.getFrame(), w.toString());
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "translate sequence=selected;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    private static final String NAME = "Translate...";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Translate sequenced DNA or cDNA sequence";
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
        AlignmentViewer viewer = (AlignmentViewer) getViewer();
        return viewer.getSelectedBlock().isSelected() &&
                (viewer.getAlignment().getSequenceType().equalsIgnoreCase(Alignment.DNA) || viewer.getAlignment().getSequenceType().equalsIgnoreCase(Alignment.cDNA))
                && !viewer.isShowAminoAcids();
    }

}
