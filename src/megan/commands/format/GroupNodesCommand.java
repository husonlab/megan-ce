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
package megan.commands.format;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.gui.PCoATab;
import megan.core.Director;
import megan.core.Document;
import megan.groups.GroupsViewer;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * group nodes in PCoA plot
 * Daniel Huson, 7.2014
 */
public class GroupNodesCommand extends CommandBase implements ICommand {
    /**
     * apply
     *
     * @param np
     * @throws Exception
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set groupNodes=");
        final String choice = np.getWordMatchesIgnoringCase("none selected");
        np.matchIgnoreCase(";");

        final Document doc = ((Director) getDir()).getDocument();

        if (choice.equalsIgnoreCase("none")) {
            for (String sample : doc.getSampleNames()) {
                doc.getSampleAttributeTable().putGroupId(sample, null);
            }
        } else if (choice.equalsIgnoreCase("selected")) {
            Collection<String> selectedSamples;

            if (getViewer() instanceof SamplesViewer) {

                SamplesViewer samplesViewer = (SamplesViewer) getViewer();
                selectedSamples = samplesViewer.getSamplesTableView().getSelectedSamples();
            } else {
                selectedSamples = doc.getSampleSelection().getAll();
            }

            int nextId = 1;
            for (String sample : doc.getSampleNames()) {
                if (Basic.isInteger(doc.getSampleAttributeTable().getGroupId(sample))) {
                    int joinId = Basic.parseInt(doc.getSampleAttributeTable().getGroupId(sample));
                    if (joinId != 0 && nextId <= joinId)
                        nextId = joinId + 1;
                }
            }
            for (String sample : selectedSamples) {
                doc.getSampleAttributeTable().putGroupId(sample, "" + nextId);
            }
        }
        final GroupsViewer groupViewer = (GroupsViewer) getDir().getViewerByClass(GroupsViewer.class);
        if (groupViewer != null)
            groupViewer.updateView(IDirector.ALL);

        for (IDirectableViewer viewer : ((Director) getDir()).getViewers()) {
            if (viewer instanceof ClusterViewer) {
                if (choice.equalsIgnoreCase("selected")) {
                    final PCoATab pcoaTab = ((ClusterViewer) viewer).getPcoaTab();
                    if (!pcoaTab.isShowGroupsAsEllipses() && !pcoaTab.isShowGroupsAsConvexHulls()) {
                        pcoaTab.setShowGroupsAsEllipses(true);
                    }
                }
                viewer.updateView(Director.ALL);
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
        return "set groupNodes={none|selected};";
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
        if (getViewer() instanceof ClusterViewer) {
            final ClusterViewer clusterViewer = (ClusterViewer) getViewer();
            return clusterViewer.isPCoATab() && clusterViewer.getGraphView().getSelectedNodes().size() >= 1;
        } else if (getViewer() instanceof SamplesViewer) {
            final SamplesViewer samplesViewer = (SamplesViewer) getViewer();
            return samplesViewer.getSamplesTableView().getCountSelectedSamples() > 0;
        } else
            return ((Director) getDir()).getDocument().getSampleSelection().size() >= 1;
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Group Nodes";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Group selected nodes in PCoA plot";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("JoinNodes16.gif");
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
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        execute("show window=groups;set groupNodes=selected;");
    }
}
