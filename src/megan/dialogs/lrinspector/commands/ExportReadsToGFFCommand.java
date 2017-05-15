/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.TextFileFilter;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.dialogs.export.ExportAlignedReads2GFF;
import megan.dialogs.lrinspector.LRInspectorViewer;
import megan.dialogs.lrinspector.TableItem;
import megan.parsers.blast.BlastMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * export reads in GFF format
 * Daniel Huson, 3.2017
 */
public class ExportReadsToGFFCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=GFF file=");
        final String fileName = np.getWordFileNamePunctuation();
        np.matchIgnoreCase(";");

        if (getViewer() instanceof LRInspectorViewer) {
            final LRInspectorViewer viewer = (LRInspectorViewer) getViewer();
            final BlastMode blastMode = viewer.getDir().getDocument().getBlastMode();

            if (viewer.getController() != null) {
                System.err.println("Writing file: " + fileName);
                int lines = 0;
                try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName))) {
                    w.write(ExportAlignedReads2GFF.getHeader());
                    for (TableItem item : viewer.getController().getTableView().getSelectionModel().getSelectedItems()) {
                        w.write(ExportAlignedReads2GFF.createGFFLine(blastMode, item.getReadName(), item.getReadLength(), item.getPane().getCNames(), item.getPane().getIntervals()));
                        lines++;
                    }
                }
                System.err.println("done (" + lines + ")");
            }
        }
    }

    public String getSyntax() {
        return "export what=GFF file=<file-name>";
    }

    public void actionPerformed(ActionEvent event) {
        final Director dir = (Director) getDir();

        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), ".gff");
        String lastGFFFile = ProgramProperties.get("lastGFFFile", "");
        File lastOpenFile = new File((new File(lastGFFFile)).getParent(), name);

        final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(".gff"), new TextFileFilter(".gff"), event, "Save Read annotations to file", ".gff");

        if (file != null) {
            ProgramProperties.put("lastGFFFile", file.getPath());
            execute("export what=GFF file='" + file.getPath() + "';");
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof LRInspectorViewer && ((LRInspectorViewer) getViewer()).getNumberOfSelectedItems() > 0;
    }

    public static final String NAME = "Export Reads in GFF Format...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Export selected read annotations in GFF format";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Export16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }
}
