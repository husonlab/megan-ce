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
package megan.samplesviewer.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * * duplicate command
 * * Daniel Huson, 9.2012
 */
public class ExtractSamplesCommand extends CommandBase implements ICommand {
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
        SamplesViewer viewer = (SamplesViewer) getViewer();
        List<String> samples = viewer.getSamplesTableView().getSelectedSamples();
        if (samples.size() > 0) {
            execute("extract samples='" + Basic.toString(samples, "' '") + "';");
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTableView().getCountSelectedSamples() > 0;
    }

    public String getName() {
        return "Extract Samples...";
    }


    public String getDescription() {
        return "Extract selected samples to a new document";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
