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

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.BasicSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.chart.FViewerChart;
import megan.chart.TaxaChart;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.URL;

public class OpenWebPageCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show webpage classification=<name> id=<id>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show webpage classification=");
        String name = np.getWordRespectCase();
        np.matchIgnoreCase("id=");
        String id = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        boolean ok = false;
        try {
            final String searchURL = ProgramProperties.get(ProgramProperties.SEARCH_URL, ProgramProperties.defaultSearchURL);
            final URL url = new URL(String.format(searchURL, name.trim().replaceAll(" ", "+") + "+" + id.trim().replaceAll(" ", "+")));
            System.err.println(url);
            BasicSwing.openWebPage(url);
            ok = true;
        } catch (Exception e1) {
            Basic.caught(e1);
        }

        if (!ok)
            NotificationsInSwing.showWarning(getViewer().getFrame(), "Failed search for: " + name + "+" + id);
    }


    public void actionPerformed(ActionEvent event) {
        ViewerBase viewer = (ViewerBase) getViewer();

        java.util.Collection<String> selectedIds = viewer.getSelectedNodeLabels(false);
        if (selectedIds.size() >= 5 && JOptionPane.showConfirmDialog(getViewer().getFrame(), "Do you really want to open " + selectedIds.size() +
                " windows in your browser?", "Confirmation - MEGAN", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
            return;

        String name = "";
        if (getViewer() instanceof MainViewer || getViewer() instanceof TaxaChart)
            name = "Taxonomy";
        else if (getViewer() instanceof ClassificationViewer) {
            name = getViewer().getClassName();
        } else if (getViewer() instanceof FViewerChart) {
            name = ((FViewerChart) getViewer()).getCName();
        }
        if (name.contains("2"))
            name = name.substring(name.indexOf("2") + 1);

        boolean ok = false;
        for (String id : selectedIds) {
            try {
                final String searchURL = ProgramProperties.get(ProgramProperties.SEARCH_URL, ProgramProperties.defaultSearchURL);
                final URL url = new URL(String.format(searchURL, name.trim().replaceAll("\\s+", "+") + "+" + id.trim()
                        .replaceAll("<", " ").replaceAll(">", " ").replaceAll("\\s+", "+")));
                System.err.println(url);
                BasicSwing.openWebPage(url);
                ok = true;
            } catch (Exception e1) {
                Basic.caught(e1);
            }
        }
        if (!ok)
            NotificationsInSwing.showWarning(viewer.getFrame(), "Failed to open website");
    }

    public boolean isApplicable() {
        ViewerBase viewer = (ViewerBase) getViewer();
        return viewer.getSelectedNodes().size() > 0;
    }

    public static final String NAME = "Web Search...";

    public String getName() {
        return NAME;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/WebComponent16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Search for selected items in browser";
    }

    public boolean isCritical() {
        return true;
    }
}

