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
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;

/**
 * * reorder samples in viewer
 * * Daniel Huson, 9.2012
 */
public class ApplyOrderToViewersCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "reorder samples[=<name>...];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("reorder samples");
        LinkedList<String> newOrder = new LinkedList<>();
        if (np.peekMatchIgnoreCase("=")) {
            np.matchIgnoreCase("=");
            while (!np.peekMatchIgnoreCase(";")) {
                newOrder.add(np.getWordRespectCase());
            }
        } else {
            final SamplesViewer viewer = (SamplesViewer) getViewer();
            newOrder.addAll(viewer.getSamplesTableView().getSamples());
        }
        np.matchIgnoreCase(";");

        final SamplesViewer viewer = (SamplesViewer) getViewer();
        Document doc = viewer.getDocument();
        doc.reorderSamples(newOrder);
    }

    public void actionPerformed(ActionEvent event) {
    }

    public boolean isApplicable() {
        final SamplesViewer viewer = (SamplesViewer) getViewer();
        return viewer != null && viewer.getDocument().getNumberOfSamples() > 0
                && !viewer.getSampleAttributeTable().getSampleOrder().equals(viewer.getDocument().getSampleNames());
    }

    public String getName() {
        return null;
    }

    public String getDescription() {
        return "Reorder samples in all viewers as specified";
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
