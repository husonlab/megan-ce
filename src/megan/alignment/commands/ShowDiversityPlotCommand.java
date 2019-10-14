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
import megan.chart.DiversityPlotViewer;
import megan.core.Director;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * command
 * Daniel Huson, 8.2011
 */
public class ShowDiversityPlotCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("chart wordCount kmer=");
        int kmer = np.getInt(1, Integer.MAX_VALUE);
        np.matchIgnoreCase("step=");
        int step = np.getInt(1, Integer.MAX_VALUE);
        np.matchIgnoreCase("mindepth=");
        int mindepth = np.getInt(1, Integer.MAX_VALUE);
        np.matchIgnoreCase(";");

        AlignmentViewer alignmentViewer = (AlignmentViewer) getViewer();

        DiversityPlotViewer viewer = new DiversityPlotViewer((Director) getDir(), alignmentViewer, kmer, step, mindepth);
        getDir().addViewer(viewer);
        viewer.setVisible(true);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "chart wordCount kmer=<number> step=<number> mindepth=<number>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        AlignmentViewer alignmentViewer = (AlignmentViewer) getViewer();

        int nSequences = alignmentViewer.getAlignment().getNumberOfSequences();
        int step = 1;
        if (nSequences > 10000 && nSequences <= 20000)
            step = 2;
        else if (nSequences > 20000 && nSequences <= 30000)
            step = 4;
        else if (nSequences > 30000)
            step = 6;
        execute("chart wordCount kmer=25 step=" + step + " mindepth=10;");
    }

    private static final String NAME = "Chart Diversity...";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Chart diversity ratio";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("RareFaction16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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
