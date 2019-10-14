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
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.dialogs.extractor.ExtractReadsViewer;
import megan.util.WindowUtilities;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class ShowExtractReadsDialogCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=ExtractReads;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final Director dir = getDir();
        ExtractReadsViewer extractReadsViewer = (ExtractReadsViewer) dir.getViewerByClass(ExtractReadsViewer.class);
        final IDirectableViewer viewer = getViewer();

        if (extractReadsViewer == null)
            extractReadsViewer = (ExtractReadsViewer) dir.addViewer(new ExtractReadsViewer(viewer.getFrame(), dir));
        if (viewer instanceof MainViewer)
            extractReadsViewer.setMode(ClassificationType.Taxonomy.toString());
        else if (viewer instanceof ClassificationViewer)
            extractReadsViewer.setMode(getViewer().getClassName());
        else {
            throw new IOException("Invalid viewer");
        }
        WindowUtilities.toFront(extractReadsViewer);

    }

    public void actionPerformed(ActionEvent event) {
        execute(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() != null && getViewer() instanceof ClassificationViewer
                && getDir().getDocument().getMeganFile().hasDataConnector();
    }

    private final static String NAME = "Extract Reads...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Extract reads for the selected nodes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Extractor16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}


