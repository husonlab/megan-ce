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
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;
import megan.util.WindowUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ShowSamplesViewerCommand extends megan.commands.CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=samplesViewer;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        SamplesViewer samplesViewer = (SamplesViewer) getDir().getViewerByClass(SamplesViewer.class);
        if (samplesViewer == null) {
            samplesViewer = new SamplesViewer(getDir());
            getDir().addViewer(samplesViewer);
        }
        WindowUtilities.toFront(samplesViewer.getFrame());
    }

    public void actionPerformed(ActionEvent event) {
        SamplesViewer samplesViewer = (SamplesViewer) getDir().getViewerByClass(SamplesViewer.class);
        if (samplesViewer == null)
            execute(getSyntax());
        else // already exists, just need to bring to the front
            executeImmediately(getSyntax());

    }

    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfSamples() > 0;

    }

    public static final String NAME = "Samples Viewer...";

    public String getName() {
        return NAME;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Table16.gif");
    }

    public String getDescription() {
        return "Opens the Samples Viewer";
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}

