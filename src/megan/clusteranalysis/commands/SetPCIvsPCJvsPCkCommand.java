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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * PC2 vs PC3
 * Daniel Huson, 9.2012
 */
public class SetPCIvsPCJvsPCkCommand extends CommandBase implements ICheckBoxCommand {
    /**
     * this is currently selected?
     *
     * @return selected
     */
    public boolean isSelected() {
        ClusterViewer viewer = getViewer();
        return viewer.getPcoaTab() != null && viewer.getPcoaTab().getPCoA() != null && viewer.getPcoaTab().getPCoA().getNumberOfPositiveEigenValues() > 3
                && !(viewer.getPcoaTab().getFirstPC() == 0 && viewer.getPcoaTab().getSecondPC() == 1 && viewer.getPcoaTab().getThirdPC() == 2)
                && viewer.getPcoaTab().isIs3dMode();
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return null;
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
        return getViewer().isPCoATab();
    }


    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "PCi PCj PCk...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Display three principle components";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("PCIvPCJvsPCK_16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_6, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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

        String value = (tab.getFirstPC() + 1) + " x " + (tab.getSecondPC() + 1) + " x " + (tab.getThirdPC() + 1);
        value = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter PCs (range 1-" + numberOfPCs + "):", value);
        if (value != null) {
            try {
                String[] tokens = value.split("x");
                int pc1 = Integer.parseInt(tokens[0].trim());
                int pc2 = Integer.parseInt(tokens[1].trim());
                int pc3 = Integer.parseInt(tokens[2].trim());
                execute("set pc1=" + pc1 + " pc2=" + pc2 + " pc3=" + pc3 + ";");
            } catch (Exception ex) {
                NotificationsInSwing.showError(getViewer().getFrame(), "Expected 'pc1 x pc2 xpc3', got: " + value);
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
