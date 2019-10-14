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
package megan.inspector.commands;

import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.commands.clipboard.ClipboardBase;
import megan.inspector.InspectorWindow;
import megan.inspector.MatchHeadLineNode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.TreeSet;

/**
 * disable taxon associated with selected match
 * Daniel Huson, Nov 2017
 */
public class DisableTaxonCommand extends ClipboardBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        final Set<Integer> set = new TreeSet<>();
        final InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        for (MatchHeadLineNode node : inspectorWindow.getAllSelectedMatchHeadLineNodes()) {
            if (node.getTaxId() > 0)
                set.add(node.getTaxId());
        }
        if (set.size() > 10 && JOptionPane.showConfirmDialog(getViewer().getFrame(), "Do you really want to disable " + set.size() +
                " taxa?", "Confirmation - MEGAN", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
            return;

        if (set.size() > 0)
            execute("disable taxa=" + Basic.toString(set, " ") + ";");
    }

    public boolean isApplicable() {
        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        return inspectorWindow != null && inspectorWindow.hasSelectedMatchHeadLineNodes();
    }

    private static final String NAME = "Disable Taxon";

    public String getName() {
        return NAME;
    }

    public String getAltName() {
        return "Disable Taxon For Match";
    }

    public String getDescription() {
        return "Disable the taxon associated with this match";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}

