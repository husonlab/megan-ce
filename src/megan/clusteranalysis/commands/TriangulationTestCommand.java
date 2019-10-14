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
package megan.clusteranalysis.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.TriangulationTest;
import megan.commands.CommandBase;
import megan.core.Document;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * apply the triangulation test
 * Daniel Huson, 11.2015
 */
public class TriangulationTestCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "triangulationTest attribute=<name>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("triangulationTest attribute=");
        final String attribute = np.getLabelRespectCase();
        np.matchIgnoreCase(";");

        final ClusterViewer viewer = (ClusterViewer) getViewer();
        final Document doc = viewer.getDocument();

        if (TriangulationTest.isSuitableAttribute(doc.getSampleAttributeTable(), attribute)) {
            TriangulationTest triangulationTest = new TriangulationTest();
            boolean rejectedH0 = triangulationTest.apply(viewer, attribute);
            if (rejectedH0) {
                NotificationsInSwing.showInformation(viewer.getFrame(), "Triangulation test REJECTS null hypothesis that samples come from same distribution (alpha=0.05, attribute='" + attribute + "')");
            } else {
                NotificationsInSwing.showInformation(viewer.getFrame(), "Triangulation test FAILS to reject null hypothesis that samples come from same distribution (attribute='" + attribute + "')");
            }
        } else
            NotificationsInSwing.showWarning(viewer.getFrame(), "Triangulation test not applicable to attribute '" + attribute + "': doesn't have at least two samples per value");
    }

    public void actionPerformed(ActionEvent event) {
        final ClusterViewer viewer = (ClusterViewer) getViewer();
        final Document doc = viewer.getDocument();
        List<String> attributes = doc.getSampleAttributeTable().getAttributeOrder();
        ArrayList<String> choices = new ArrayList<>(attributes.size());
        for (String attribute : attributes) {
            if (TriangulationTest.isSuitableAttribute(doc.getSampleAttributeTable(), attribute))
                choices.add(attribute);
        }
        if (choices.size() > 0) {
            String choice = ProgramProperties.get("TriangulationTestChoice", choices.get(0));
            if (!choices.contains(choice))
                choice = choices.get(0);
            final String[] array = choices.toArray(new String[0]);
            choice = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Attribute that defines biological replicates:",
                    "Setup triangulation test", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), array, choice);
            if (choice != null) {
                ProgramProperties.put("TriangulationTestChoice", choice);
                execute("triangulationTest attribute='" + choice + "';");
            }
        } else {
            NotificationsInSwing.showWarning("Triangulation test not applicable: no attribute that defines biological replicates, with at least two samples per value");
        }
    }

    public boolean isCritical() {
        return true;
    }


    public boolean isApplicable() {
        return getViewer() instanceof ClusterViewer && ((ClusterViewer) getViewer()).getDocument().getSampleAttributeTable().getNumberOfAttributes() > 0;
    }


    public String getName() {
        return "Apply Triangulation Test...";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Applies the triangulation test to determine whether biological samples have same distribution, based on multiple technical replicates per sample";
    }
}

