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
package megan.commands.show;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.TaxonomyClusterViewer;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ShowClusterWindowCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=clusterViewer;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        final Director dir = getDir();

        if (getViewer() instanceof MainViewer) {
            ClusterViewer viewer = (ClusterViewer) dir.getViewerByClass(TaxonomyClusterViewer.class);
            if (viewer == null) {
                viewer = new TaxonomyClusterViewer((MainViewer) getViewer());
                if (ClusterViewer.clusterViewerAddOn != null)
                    ClusterViewer.clusterViewerAddOn.accept(viewer);
                dir.addViewer(viewer);
            }
            viewer.getFrame().toFront();

        } else if (getViewer() instanceof ClassificationViewer) {
            final String name = getViewer().getClassName().toUpperCase() + "ClusterViewer";
            ClusterViewer viewer = (ClusterViewer) dir.getViewerByClassName(name);
            if (viewer == null) {
                viewer = new ClusterViewer(dir, (ClassificationViewer) getViewer(), getViewer().getClassName());
                if (ClusterViewer.clusterViewerAddOn != null)
                    ClusterViewer.clusterViewerAddOn.accept(viewer);
                dir.addViewer(viewer);
            }
            viewer.toFront();
        }
    }

    public void actionPerformed(ActionEvent event) {
        if (getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getSelectedNodes().size() == 0)
            executeImmediately("select nodes=leaves;");
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfSamples() >= 4 && getViewer() instanceof ClassificationViewer;

    }

    public String getName() {
        return "Cluster Analysis...";
    }

    public String getDescription() {
        return "Open a cluster analysis window";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Network16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}
