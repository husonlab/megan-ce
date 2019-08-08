/*
 *  Copyright (C) 2015 Daniel H. Huson
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
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.inspector.InspectorWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class ExportSelectionCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export what=selection file=<filename>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=selection");

        np.matchIgnoreCase("file=");
        String outputFile = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");

        final String selection = ((InspectorWindow) getViewer()).getSelection();
        if (selection.length() > 0) {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(Basic.getOutputStreamPossiblyZIPorGZIP(outputFile)))) {
                writer.write(selection);
            }
        }
        NotificationsInSwing.showInformation(getViewer().getFrame(), "Wrote " + Basic.countOccurrences(selection, '\n') + " lines to file: " + outputFile);
    }

    public boolean isApplicable() {
        return getViewer() instanceof InspectorWindow && ((InspectorWindow) getViewer()).hasSelectedNodes();
    }

    public boolean isCritical() {
        return true;
    }

    public void actionPerformed(ActionEvent event) {
        final Director dir = (Director) getDir();
        if (getViewer() instanceof InspectorWindow) {
            String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-inspector.txt");
            File lastOpenFile = new File(name);

            final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), event, "Save selected text to file", ".txt");

            if (file != null) {
                execute("export what=selection file='" + file.getPath() + "';");
            }
        }
    }

    public String getName() {
        return "Export Selected Text...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export selection to a text file";
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
