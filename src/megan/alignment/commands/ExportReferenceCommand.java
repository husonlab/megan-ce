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
package megan.alignment.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.FastaFileFilter;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.SelectedBlock;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * save data command
 * Daniel Huson, 11.2010
 */
public class ExportReferenceCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export reference file=<filename> [what={all|selection}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export reference file=");
        String fileName = np.getAbsoluteFileName();
        boolean saveAll = true;
        if (np.peekMatchIgnoreCase("what")) {
            np.matchIgnoreCase("what=");
            saveAll = np.getWordMatchesIgnoringCase("selected all").equalsIgnoreCase("all");
        }
        np.matchIgnoreCase(";");

        try {
            AlignmentViewer viewer = (AlignmentViewer) getViewer();
            if (saveAll) {
                System.err.println("Exporting complete reference to: " + fileName);
                Alignment alignment = viewer.getAlignment();
                String string = alignment.getReference().toStringIncludingLeadingAndTrailingGaps().replaceAll(" ", "");
                Writer w = new FileWriter(fileName);
                w.write(string);
                w.write("\n");
                w.close();
            } else {
                System.err.println("Exporting selected reference to: " + fileName);
                Writer w = new FileWriter(fileName);
                w.write(viewer.getAlignmentViewerPanel().getSelectedReference());
                w.write("\n");
                w.close();
            }
        } catch (IOException e) {
            NotificationsInSwing.showError("Export Reference failed: " + e.getMessage());
        }
    }

    public void actionPerformed(ActionEvent event) {
        File lastOpenFile = ProgramProperties.getFile("SaveReference");
        String fileName = ((AlignmentViewer) getViewer()).getAlignment().getName();
        if (fileName == null)
            fileName = "Untitled";
        else
            fileName = Basic.toCleanName(fileName);
        if (lastOpenFile != null) {
            fileName = new File(lastOpenFile.getParent(), fileName).getPath();
        }
        fileName = Basic.replaceFileSuffix(fileName, "-reference.fasta");

        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), new File(fileName), new FastaFileFilter(), new FastaFileFilter(), event, "Save reference file", ".fasta");

        if (file != null) {
            if (Basic.getFileSuffix(file.getName()) == null)
                file = Basic.replaceFileSuffix(file, ".txt");
            ProgramProperties.put("SaveReference", file);
            SelectedBlock selectedBlock = ((AlignmentViewer) getViewer()).getSelectedBlock();

            executeImmediately("export reference file='" + file.getPath() + "' what=" + (selectedBlock == null || !selectedBlock.isSelected() ? "all" : "Selected") + ";");
        }
    }

    public boolean isApplicable() {
        return ((AlignmentViewer) getViewer()).getAlignment().getLength() > 0;
    }

    public String getName() {
        return "Reference...";
    }

    public String getAltName() {
        return "Export Reference...";
    }


    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export reference sequence to a file";
    }

    public boolean isCritical() {
        return false;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
