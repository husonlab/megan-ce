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
import jloda.util.Pair;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.ComputeAlignmentProperties;
import megan.core.Director;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 8.2011
 */
public class ComputeDiversityCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("compute diversityRatio kmer=");
        int kmer = np.getInt(1, Integer.MAX_VALUE);
        np.matchIgnoreCase("step=");
        int step = np.getInt(1, Integer.MAX_VALUE);
        np.matchIgnoreCase("mindepth=");
        int mindepth = np.getInt(1, Integer.MAX_VALUE);
        np.matchIgnoreCase(";");

        AlignmentViewer viewer = (AlignmentViewer) getViewer();

        Pair<Double, Double> kn = ComputeAlignmentProperties.computeSequenceDiversityRatio(viewer.getAlignment(), step, kmer, mindepth, ((Director) getDir()).getDocument().getProgressListener());
        ((Director) getDir()).getDocument().getProgressListener().close();
        NotificationsInSwing.showInformation(viewer.getFrame(), "Average diversity ratio:\n" + (float) (0 + kn.getFirst()) + " / " + (float) (0 + kn.getSecond()) + " = " + (float) (kn.getFirst() / kn.getSecond()));
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "compute diversityRatio kmer=<number> step=<number> mindepth=<number>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("compute diversityRatio kmer=25 step=25 mindepth=10;");
    }

    private static final String NAME = "Compute Diversity...";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Compute diversity ratio";
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
        return viewer.getAlignment().getLength() > 0;
    }
}
