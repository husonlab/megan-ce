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
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.BasicSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

public class OpenNCBIWebPageCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show webpage taxon=<name|id>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show webpage taxon=");
        String taxon = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        boolean ok = false;
        int taxId;
        if (Basic.isInteger(taxon))
            taxId = Integer.parseInt(taxon);
        else
            taxId = TaxonomyData.getName2IdMap().get(taxon);

        if (taxId > 0) {
            try {
                BasicSwing.openWebPage(new URL("https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?mode=Info&id=" + taxId));
                ok = true;
            } catch (Exception e1) {
                Basic.caught(e1);
            }
        }
        if (!ok)
            NotificationsInSwing.showError(getViewer().getFrame(), "Failed to open NCBI website for taxon: " + taxon);
    }


    public void actionPerformed(ActionEvent event) {
        ClassificationViewer viewer = (ClassificationViewer) getViewer();
        int selectedTaxa = viewer.getSelectedNodes().size();
        if (selectedTaxa >= 5 && JOptionPane.showConfirmDialog(viewer.getFrame(), "Do you really want to open " + selectedTaxa +
                " windows in your browser?", "Confirmation - MEGAN", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
            return;

        boolean ok = false;
        for (Node v : viewer.getSelectedNodes()) {
            Integer taxId = (Integer) v.getInfo();
            if (taxId != null && taxId > 0) {
                try {
                    BasicSwing.openWebPage(new URL("https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?mode=Info&id=" + taxId));
                    ok = true;
                } catch (Exception e1) {
                    Basic.caught(e1);
                }
            }
        }
        if (!ok)
            NotificationsInSwing.showError(viewer.getFrame(), "Failed to open NCBI website");
    }

    public boolean isApplicable() {
        return getViewer() instanceof MainViewer && ((MainViewer) getViewer()).getSelectedNodes().size() > 0;
    }

    static final public String NAME = "Open NCBI Web Page...";

    public String getName() {
        return NAME;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Ncbi16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }

    public String getDescription() {
        return "Open NCBI Taxonomy web site in browser";
    }

    public boolean isCritical() {
        return true;
    }
}

