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
package megan.commands.export;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.FastaFileFilter;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.export.ReadsExporter;
import megan.dialogs.lrinspector.LRInspectorViewer;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ExportReadsCommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return "export what=reads [data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "}] file=<filename>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=reads");

        final String data;
        if (np.peekMatchIgnoreCase("data")) {
            np.matchIgnoreCase("data=");
            data = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        } else
            data = Classification.Taxonomy;
        np.matchIgnoreCase("file=");

        String outputFile = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");

        final Director dir = getDir();
        final Document doc = dir.getDocument();

        int count;

        if (getViewer() instanceof ClassificationViewer) {
            final ClassificationViewer classificationViewer = (ClassificationViewer) dir.getViewerByClassName(data);
            final Set<Integer> classIds = new HashSet<>();
            if (classificationViewer != null)
                classIds.addAll(classificationViewer.getSelectedIds());

            if (classIds.size() == 0)
                count = ReadsExporter.exportAll(doc.getConnector(), outputFile, doc.getProgressListener());
            else
                count = ReadsExporter.export(data, classIds, doc.getConnector(), outputFile, doc.getProgressListener());
        } else if (getViewer() instanceof LRInspectorViewer) {
            count = ((LRInspectorViewer) getViewer()).exportSelectedReads(outputFile, doc.getProgressListener());
        } else
            count = 0;

        NotificationsInSwing.showInformation(getViewer().getFrame(), "Wrote " + count + " reads to file: " + outputFile);
    }

    public boolean isApplicable() {
        return getDoc().getMeganFile().hasDataConnector();
    }

    public boolean isCritical() {
        return true;
    }

    public void actionPerformed(ActionEvent event) {
        Director dir = getDir();
        if (!dir.getDocument().getMeganFile().hasDataConnector())
            return;
        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-ex.fasta");
        File lastOpenFile = new File(name);

        final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new FastaFileFilter(), new FastaFileFilter(), event, "Save all READs to file", ".fasta");

        final String data;
        if (getViewer() instanceof ClassificationViewer)
            data = getViewer().getClassName();
        else
            data = ClassificationType.Taxonomy.toString();

        if (file != null) {
            execute("export what=reads data=" + data + " file='" + file.getPath() + "';");
        }
    }

    public String getName() {
        return "Reads...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export reads to a text file";
    }
}


