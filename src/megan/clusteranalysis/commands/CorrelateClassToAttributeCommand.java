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
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.commands.CommandBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

public class CorrelateClassToAttributeCommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        Collection<String> list = getDoc().getSampleAttributeTable().getNumericalAttributes();
        ClusterViewer viewer = (ClusterViewer) getViewer();
        Collection<Integer> ids = viewer.getSelectedClassIds();
        if (ids.size() > 0 && list.size() > 0) {
            final String[] choices = list.toArray(new String[0]);
            String choice = ProgramProperties.get("CorrelateToAttribute", choices[0]);
            if (!list.contains(choice))
                choice = choices[0];

            choice = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Choose attribute to correlate to:",
                    "Compute Correlation Coefficient", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), choices, choice);

            if (choice != null) {
                ProgramProperties.put("CorrelateToAttribute", choice);

                StringBuilder buf = new StringBuilder();

                buf.append("correlate class=");
                for (Integer id : ids) {
                    buf.append(" ").append(id);
                }
                buf.append("' classification='").append(((ClusterViewer) getViewer()).getParentViewer().getClassName())
                        .append("' attribute='").append(choice).append("';");
                executeImmediately("show window=message;");
                execute(buf.toString());
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof ClusterViewer && ((ClusterViewer) getViewer()).getSelectedClassIds().size() > 0 && getDoc().getSampleAttributeTable().getNumberOfAttributes() > 0;
    }

    public String getName() {
        return "Correlate To Attributes...";
    }

    public String getDescription() {
        return "Correlate assigned (or summarized) counts for nodes with a selected attribute";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}
