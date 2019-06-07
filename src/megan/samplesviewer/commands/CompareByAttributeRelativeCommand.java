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
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.dialogs.compare.Comparer;
import megan.samplesviewer.SamplesTableView;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * * compare by command
 * * Daniel Huson, 9.2015
 */
public class CompareByAttributeRelativeCommand extends CommandBase implements ICommand {
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
        final SamplesViewer viewer = ((SamplesViewer) getViewer());
        final String attribute = viewer.getSamplesTableView().getASelectedAttribute();
        final SamplesTableView samplesTable = ((SamplesViewer) getViewer()).getSamplesTableView();

        if (attribute != null && samplesTable.getCountSelectedSamples() > 0) {
            execute("compareBy attribute='" + attribute + "' mode=" + Comparer.COMPARISON_MODE.RELATIVE.toString().toLowerCase() +
                    " samples='" + Basic.toString(samplesTable.getSelectedSamples(), "' '") + "';");
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTableView().getCountSelectedAttributes() == 1;
    }

    public String getName() {
        return "Compare Relative...";
    }

    public String getDescription() {
        return "Aggregate samples by this attribute and show comparison in new document";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Compare16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}
