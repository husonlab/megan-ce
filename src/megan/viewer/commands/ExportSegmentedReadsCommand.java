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

package megan.viewer.commands;

import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.FastaFileFilter;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.dialogs.export.analysis.SegmentedReadsExporter;
import megan.fx.NotificationsInSwing;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * export taxonomic segmentation of reads
 * Daniel Huson, 8.2018
 */
public class ExportSegmentedReadsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export segmentedReads file=<filename> [what={all|selected}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export segmentedReads file=");
        String fileName = np.getAbsoluteFileName();
        boolean saveAll = true;
        if (np.peekMatchIgnoreCase("what")) {
            np.matchIgnoreCase("what=");
            saveAll = np.getWordMatchesIgnoringCase("selected all").equalsIgnoreCase("all");
        }
        np.matchIgnoreCase(";");

        try {
            final int count;
            final ClassificationViewer viewer = (ClassificationViewer) getViewer();
            final Document doc = viewer.getDocument();
            if (saveAll)
                count = SegmentedReadsExporter.exportAll(doc, doc.getConnector(), fileName);
            else
                count = SegmentedReadsExporter.export(doc, viewer.getClassification().getName(), viewer.getSelectedIds(), doc.getConnector(), fileName);

            NotificationsInSwing.showInformation("Exported segmented reads: " + count);

        } catch (IOException e) {
            NotificationsInSwing.showError("Export segmented reads failed: " + e.getMessage());
        }
    }

    public void actionPerformed(ActionEvent event) {
        if (getViewer() instanceof ClassificationViewer) {
            final ClassificationViewer viewer = (ClassificationViewer) getViewer();

            final String fileName = Basic.replaceFileSuffix(viewer.getDocument().getMeganFile().getFileName(), "-corrected.fasta");
            File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), new File(fileName), new FastaFileFilter(), new FastaFileFilter(), event, "Save corrected reads file", ".fasta");

            if (file != null) {
                if (Basic.getFileSuffix(file.getName()) == null)
                    file = Basic.replaceFileSuffix(file, ".fasta");
                execute("export segmentedReads file='" + file.getPath() + "' what=" + (viewer.hasSelectedNodes() ? "selected" : "all") + ";");
            }
        }
    }

    public boolean isApplicable() {
        if (getViewer() instanceof ClassificationViewer) {
            final ClassificationViewer viewer = (ClassificationViewer) getViewer();
            return viewer.getDocument().getMeganFile().hasDataConnector();

        } else
            return false;
    }

    public String getName() {
        return "Export Segmented Reads...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Export16.gif");
    }

    public String getDescription() {
        return "Export segmented reads, use %t or %i in filename for to save each class into a different file";
    }

    public boolean isCritical() {
        return false;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
