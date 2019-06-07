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

package megan.timeseriesviewer.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.timeseriesviewer.TimeSeriesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

/**
 * * open samples command
 * * Daniel Huson, 6.2015
 */
public class CompareSamplesCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        TimeSeriesViewer viewer = (TimeSeriesViewer) getViewer();
        Collection<String> samples = viewer.getDataJTable().getSelectedSamples();
        if (samples.size() > 0) {
            execute("extract samples='" + Basic.toString(samples, "' '") + "';");
        }
    }

    public boolean isApplicable() {
        return ((TimeSeriesViewer) getViewer()).getDataJTable().getSelectedSamples().size() > 0;
    }

    public final static String NAME = "Compare Samples...";

    public String getName() {
        return NAME;
    }


    public String getDescription() {
        return "Compare all selected samples in a new document";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

    }
}
