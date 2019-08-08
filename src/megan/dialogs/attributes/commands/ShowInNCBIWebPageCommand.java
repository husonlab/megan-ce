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
package megan.dialogs.attributes.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.BasicSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.dialogs.attributes.AttributesWindow;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class ShowInNCBIWebPageCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show webpage taxon=");
        String taxon = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        AttributesWindow viewer = (AttributesWindow) getViewer();
        if (viewer != null) {
            boolean ok = false;
            int taxId;
            if (Basic.isInteger(taxon))
                taxId = Integer.parseInt(taxon);
            else taxId = TaxonomyData.getName2IdMap().get(taxon);

            if (taxId > 0) {
                try {
                    final URL url = new URL("https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?mode=Info&id=" + taxId);
                    BasicSwing.openWebPage(url);
                    ok = true;
                } catch (Exception e1) {
                    Basic.caught(e1);
                }
            }
            if (!ok)
                NotificationsInSwing.showError(viewer.getFrame(), "Failed to open NCBI website for taxon: " + taxon);
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "show webpage taxon=<name|id>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        AttributesWindow viewer = (AttributesWindow) getViewer();
        if (viewer != null) {
            if (TaxonomyData.getName2IdMap().get(viewer.selectedTaxon) != 0) {
                execute("show webpage taxon='" + viewer.selectedTaxon + "';");
            }
        }
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Open NCBI Webpage...";
    }

    final public static String ALTNAME = "Open NCBI Webpage Attributes";

    public String getAltName() {
        return ALTNAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Open NCBI taxonomy web site in browser";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Ncbi16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
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
        AttributesWindow viewer = (AttributesWindow) getViewer();
        return viewer != null && viewer.selectedTaxon != null;
    }
}
