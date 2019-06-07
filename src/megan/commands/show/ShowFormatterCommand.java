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
package megan.commands.show;

import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.format.Formatter;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.commands.CommandBase;
import megan.util.FormatterUtils;
import megan.util.WindowUtilities;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ShowFormatterCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=formatter;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        Formatter formatter;
        IDirectableViewer viewer = getViewer();

        if (!(viewer instanceof ClusterViewer))
            formatter = FormatterUtils.getFormatter((ViewerBase) getViewer(), getDir());
        else
            formatter = FormatterUtils.getFormatter(((ClusterViewer) viewer).getGraphView(), getDir());
        WindowUtilities.toFront(formatter.getFrame());
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() != null && (getViewer() instanceof ViewerBase || getViewer() instanceof ClusterViewer);
    }

    public String getName() {
        return "Format...";
    }

    public String getDescription() {
        return "Format nodes and edges";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_J, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

