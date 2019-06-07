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

package megan.commands.algorithms;

import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.samplesviewer.SamplesViewer;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * compute biome
 * Daniel Huson, 2.2013
 */
public class ComputeTotalBiomeCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        final Collection<String> samples;
        if (getViewer() instanceof SamplesViewer)
            samples = ((SamplesViewer) getViewer()).getSamplesTableView().getSelectedSamples();
        else if (getViewer() instanceof ClassificationViewer)
            samples = ((ClassificationViewer) getViewer()).getDocument().getSampleNames();
        else
            return;

        if (samples.size() > 1) {
            execute("compute biome=total samples='" + Basic.toString(samples, "' '") + "';");
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof ClassificationViewer || getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTableView().getCountSelectedSamples() > 1;
    }

    public boolean isCritical() {
        return true;
    }

    public String getName() {
        return "Compute Total Biome...";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Determine total (union) taxonomic and functional content";
    }
}

