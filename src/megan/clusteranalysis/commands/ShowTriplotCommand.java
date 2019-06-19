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
import jloda.swing.director.IDirector;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * show triplot
 * Daniel Huson, 7.2014
 */
public class ShowTriplotCommand extends CommandBase implements ICheckBoxCommand {
    /**
     * this is currently selected?
     *
     * @return selected
     */
    public boolean isSelected() {
        ClusterViewer viewer = getViewer();
        return viewer.getPcoaTab() != null && viewer.getPcoaTab().isShowTriPlot();
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set showTriPlot=");
        final boolean show = np.getBoolean();
        np.matchIgnoreCase(";");

        final ClusterViewer viewer = getViewer();
        viewer.getPcoaTab().setShowTriPlot(show);
        try {
            viewer.updateView(IDirector.ENABLE_STATE);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set showTriPlot={false|true};";
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
        return getViewer().isPCoATab() && getViewer().getDir().getDocument().getSampleAttributeTable().getNumericalAttributes(null).size() > 0;
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Show TriPlot";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Show tri-plot loading vectors";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null; //ResourceManager.getIcon("PC1vPC2_16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("set showTriPlot=" + (!isSelected()) + ";");
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
