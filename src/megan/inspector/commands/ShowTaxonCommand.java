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

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.inspector.InspectorWindow;
import megan.main.MeganProperties;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class ShowTaxonCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show taxon=");
        String name = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        if (getViewer() instanceof InspectorWindow) {
            InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
            int taxId;
            if (Basic.isInteger(name))
                taxId = Integer.parseInt(name);
            else
                taxId = TaxonomyData.getName2IdMap().get(name);
            if (taxId == 0) {
                NotificationsInSwing.showWarning(inspectorWindow.getFrame(), "Unknown taxon: " + name);
            } else
                inspectorWindow.addTopLevelNode(name, taxId, Classification.Taxonomy);
        } else
            NotificationsInSwing.showError(getViewer().getFrame(), "Command in invalid context");
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "show taxon=<name|id>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        String name = ProgramProperties.get(MeganProperties.FINDTAXON, "");
        name = JOptionPane.showInputDialog(inspectorWindow.getFrame(), "Enter taxon name or Id", name);
        if (name != null && name.trim().length() > 0) {
            name = name.trim();
            ProgramProperties.put(MeganProperties.FINDTAXON, name);
            executeImmediately("show taxon='" + name + "';"); // do not use execute() here because locked dir will prevent node from showing
        }
    }

    private static final String NAME = "Show Taxon...";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Show the named taxon and all reads assigned to it";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
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
        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        return inspectorWindow != null;
    }
}
