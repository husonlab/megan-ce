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
package megan.viewer.commands.collapse;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.viewer.ClassificationViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Set;

public class CollapseAtLevelCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "collapse level=<num>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("collapse level=");
        final int level = np.getInt(0, 100);
        np.matchIgnoreCase(";");

        final ClassificationViewer classificationViewer = (ClassificationViewer) getViewer();

        final Set<Integer> ids2collapse = ClassificationManager.get(classificationViewer.getClassName(), true).getFullTree().getAllAtLevel(level);
        switch (ProgramProperties.get("KeepOthersCollapsed", " none")) {
            case "prokaryotes":
                ids2collapse.addAll(TaxonomyData.getNonProkaryotesToCollapse());
                break;
            case "eukaryotes":
                ids2collapse.addAll(TaxonomyData.getNonEukaryotesToCollapse());
                break;
            case "viruses":
                ids2collapse.addAll(TaxonomyData.getNonVirusesToCollapse());
                break;
        }
        classificationViewer.setCollapsedIds(ids2collapse);

        getDoc().setDirty(true);
        classificationViewer.updateTree();
    }

    public void actionPerformed(ActionEvent event) {
        int level = 2;
        String input = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter max level", level);
        if (input != null) {
            try {
                level = Integer.parseInt(input);

            } catch (NumberFormatException ex) {
                NotificationsInSwing.showError(getViewer().getFrame(), "Failed to parse input: " + input);

            }
            if (level < 0)
                level = 0;
            execute("collapse level=" + level + ";");
        }
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Collapse at Level...";
    }

    public String getDescription() {
        return "Collapse all nodes at given depth in tree";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("CollapseTreeLevel16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

