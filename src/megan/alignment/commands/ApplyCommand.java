/*
 * ApplyCommand.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.alignment.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.NumberUtils;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.gui.Alignment;
import megan.core.Director;
import megan.core.Document;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * apply command
 * Daniel Huson, 1.2012
 */
public class ApplyCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final AlignmentViewer viewer = (AlignmentViewer) getViewer();
        final Alignment alignment = viewer.getAlignment();
        final Document doc = ((Director) getDir()).getDocument();
        viewer.getSelectedBlock().clear();
        String reference = viewer.getSelectedReference();

        if (reference != null) {
            final int posDoubleColon = reference.lastIndexOf("::");
			if (posDoubleColon > 0 && NumberUtils.isInteger(reference.substring(posDoubleColon + 2)))
				reference = reference.substring(0, posDoubleColon);

            if (reference.length() > 0) {
                doc.getProgressListener().setTasks("Alignment viewer", "Calculating alignment");
                viewer.getBlast2Alignment().makeAlignment(reference, alignment, viewer.isShowInsertions(), doc.getProgressListener());
                viewer.setShowAminoAcids(alignment.getSequenceType().equals(Alignment.PROTEIN));
                doc.getProgressListener().setTasks("Alignment viewer", "Drawing alignment");
                doc.getProgressListener().setMaximum(100);
                doc.getProgressListener().setProgress(-1);
                viewer.setAlignment(alignment, true);
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
        return "apply;";
    }

    /**
     * action to be performed
     *
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
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
        return "Compute an alignment for all sequences that match the given reference sequence";
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
        return viewer.getSelectedReference() != null;
    }
}
