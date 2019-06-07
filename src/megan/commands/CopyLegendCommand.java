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
import jloda.swing.director.IViewerWithLegend;
import jloda.swing.export.TransferableGraphic;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CopyLegendCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "copyLegend;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        IViewerWithLegend viewer = (IViewerWithLegend) getViewer();
        TransferableGraphic tg = new TransferableGraphic(viewer.getLegendPanel(), viewer.getLegendScrollPane());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(tg, tg);
    }

    public void actionPerformed(ActionEvent event) {
        execute(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() != null && getViewer() instanceof IViewerWithLegend && !((IViewerWithLegend) getViewer()).getShowLegend().equals("none");
    }

    public String getName() {
        return "Copy Legend";
    }

    public String getDescription() {
        return "Copy legend image to clipboard";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Copy16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

