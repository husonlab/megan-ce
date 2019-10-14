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
package megan.commands;

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.viewer.ViewerBase;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ComputeMannWhitneyUCommand extends CommandBase implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final Document doc = getDir().getDocument();
        int numberSelectedNodes = ((ViewerBase) getViewer()).getNumberSelectedNodes();

        MannWhitneyUTest mannWhitneyUTest = new MannWhitneyUTest();

        double[] x = new double[numberSelectedNodes];
        double[] y = new double[numberSelectedNodes];

        ViewerBase viewer = (ViewerBase) getViewer();
        int count = 0;
        for (Node v : viewer.getSelectedNodes()) {
            if (v.getOutDegree() > 0) {
                x[count] = ((NodeData) v.getData()).getAssigned(0);
                y[count] = ((NodeData) v.getData()).getAssigned(1);
            } else {
                x[count] = ((NodeData) v.getData()).getSummarized(0);
                y[count] = ((NodeData) v.getData()).getSummarized(1);
            }
        }
        double p = mannWhitneyUTest.mannWhitneyUTest(x, y);
        final String message = "Mann Whitney U Test for " + doc.getNumberOfSamples() + " samples based on " + numberSelectedNodes + " selected nodes:\n"
                + "U value=" + mannWhitneyUTest.mannWhitneyU(x, y) + "\n"
                + "p-value=" + (float) p + "\n";
        //System.err.println(message);
        NotificationsInSwing.showInformation(getViewer().getFrame(), message);
    }

    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getNumberSelectedNodes() > 0 && getDir().getDocument().getNumberOfSamples() == 2;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "compute test=MannWhitneyUTest;";
    }

    public void actionPerformed(ActionEvent event) {
        if (getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getSelectedNodes().size() == 0)
            executeImmediately("select nodes=leaves;");
        execute(getSyntax());
    }

    public String getName() {
        return "Mann Whitney U Test...";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Perform the Mann Whitney U Test on comparison document containing exactly two samples";
    }
}


