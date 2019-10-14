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
package megan.dialogs.lrinspector.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.daa.io.ByteInputStream;
import megan.dialogs.lrinspector.LRInspectorViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * * selection command
 * * Daniel Huson, 4.2017
 */
public class ExportSelectedCommand extends CommandBase implements ICommand {
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export-selection file=");
        final String fileName = np.getWordFileNamePunctuation();
        np.matchIgnoreCase(";");

        if (getViewer() instanceof LRInspectorViewer) {
            final Document doc = ((LRInspectorViewer) getViewer()).getDir().getDocument();
            final byte[] selection = ((LRInspectorViewer) getViewer()).getSelection(doc.getProgressListener()).getBytes();
            try {
                Basic.writeStreamToFile(new ByteInputStream(selection, selection.length), new File(fileName));
                NotificationsInSwing.showInformation("Exported " + Basic.countOccurrences(selection, '\n') + " lines to file: " + fileName);
            } catch (IOException ex) {
                NotificationsInSwing.showError("Export failed: " + ex.getMessage());
            }
        }
    }

    public String getSyntax() {
        return "export-selection file=<filename>;";
    }

    public void actionPerformed(ActionEvent event) {
        if (getViewer() instanceof LRInspectorViewer) {
            final String className = Basic.toCleanName(((LRInspectorViewer) getViewer()).getClassIdDisplayName());
            final File lastOpenFile = new File(Basic.replaceFileSuffix(((LRInspectorViewer) getViewer()).getDir().getDocument().getMeganFile().getFileName(), "-" + className + ".txt"));
            final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), event, "Export table");
            if (file != null) {
                execute("export-selection file='" + file.getPath() + "';");
            }
        }

    }

    public boolean isApplicable() {
        if (getViewer() instanceof LRInspectorViewer) {
            return ((LRInspectorViewer) getViewer()).getNumberOfSelectedItems() > 0;
        } else
            return false;
    }

    public String getName() {
        return "Selection...";
    }

    public String getAltName() {
        return "Export Selection...";
    }

    public String getDescription() {
        return "Export selected rows to a file";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
