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

import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.gui.PCoATab;
import megan.clusteranalysis.pcoa.PCoA;
import megan.clusteranalysis.tree.Taxa;
import megan.core.Director;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

/**
 * PC2 vs PC3
 * Daniel Huson, 9.2012
 */
public class SetPCIvsPCJCommand extends CommandBase implements ICheckBoxCommand {
    /**
     * this is currently selected?
     *
     * @return selected
     */
    public boolean isSelected() {
        ClusterViewer viewer = getViewer();
        return viewer.getPcoaTab() != null && viewer.getPcoaTab().getPCoA() != null && viewer.getPcoaTab().getPCoA().getNumberOfPositiveEigenValues() > 3
                && !(viewer.getPcoaTab().getFirstPC() == 0 && viewer.getPcoaTab().getSecondPC() == 1)
                && !(viewer.getPcoaTab().getFirstPC() == 0 && viewer.getPcoaTab().getSecondPC() == 2)
                && !(viewer.getPcoaTab().getFirstPC() == 1 && viewer.getPcoaTab().getSecondPC() == 2) && !viewer.getPcoaTab().isIs3dMode();
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        final ClusterViewer viewer = getViewer();
        int maxPC = viewer.getPcoaTab().getPCoA().getNumberOfPositiveEigenValues();
        np.matchIgnoreCase("set pc1=");

        int pc1 = np.getInt(1, maxPC);
        np.matchIgnoreCase("pc2=");
        int pc2 = np.getInt(1, maxPC);
        int pc3 = Math.max(pc1, pc2) + 1;
        if (np.peekMatchIgnoreCase(";")) {
            viewer.getPcoaTab().set3dMode(false);
        } else {
            np.matchIgnoreCase("pc3=");
            pc3 = np.getInt(1, maxPC);
            viewer.getPcoaTab().set3dMode(true);
        }
        np.matchIgnoreCase(";");
        if (pc1 == pc2)
            throw new IOException("pc1==pc2");
        if (pc1 == pc3)
            throw new IOException("pc1==pc3");
        if (pc2 == pc3)
            throw new IOException("pc2==pc3");

        {
            viewer.getPcoaTab().setFirstPC(pc1 - 1);
            viewer.getPcoaTab().setSecondPC(pc2 - 1);
            viewer.getPcoaTab().setThirdPC(pc3 - 1);

            final Taxa taxa = new Taxa();
            java.util.List<String> pids = ((Director) getDir()).getDocument().getSampleNames();
            for (String name : pids) {
                taxa.add(name);
            }
            viewer.getPcoaTab().setData(taxa, null);
            viewer.updateConvexHulls = true;
            viewer.addFormatting(viewer.getPcoaTab().getGraphView());
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set pc1=<number> pc2=<number> [pc3=<number>];";
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
        return getViewer().getTabbedIndex() == ClusterViewer.PCoA_TAB_INDEX;
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "PCi vs PCj...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set principle components to use";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("PCIvPCJ_16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_4, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        PCoATab tab = getViewer().getPcoaTab();
        PCoA PCoA = tab.getPCoA();

        int numberOfPCs = PCoA.getNumberOfPositiveEigenValues();

        String value = (tab.getFirstPC() + 1) + " x " + (tab.getSecondPC() + 1);
        value = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter PCs (range 1-" + numberOfPCs + "):", value);
        if (value != null) {
            try {
                String[] tokens = value.split("x");
                int pc1 = Integer.parseInt(tokens[0].trim());
                int pc2 = Integer.parseInt(tokens[1].trim());
                execute("set pc1=" + pc1 + " pc2=" + pc2 + ";");
            } catch (Exception ex) {
                NotificationsInSwing.showError(getViewer().getFrame(), "Expected 'pc1 x pc2', got: " + value);
            }
        }
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
