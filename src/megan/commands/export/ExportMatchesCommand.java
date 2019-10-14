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
import jloda.swing.util.BlastFileFilter;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.export.MatchesExporter;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ExportMatchesCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export what=matches [data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "}] file=<filename>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=matches");

        Director dir = getDir();
        MainViewer mainViewer = dir.getMainViewer();
        Document doc = dir.getDocument();

        String classificationName = ClassificationType.Taxonomy.toString();
        if (np.peekMatchIgnoreCase("data")) {
            np.matchIgnoreCase("data=");
            classificationName = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        }
        Set<Integer> classIds = new HashSet<>();
        if (classificationName.equals(Classification.Taxonomy))
            classIds.addAll(mainViewer.getSelectedIds());
        else {
            ClassificationViewer viewer = (ClassificationViewer) getDir().getViewerByClassName(classificationName);
            if (viewer != null) {
                final Classification classification = ClassificationManager.get(classificationName, true);
                for (Integer id : viewer.getSelectedIds()) {
                    Set<Integer> ids = classification.getFullTree().getAllDescendants(id);
                    classIds.addAll(ids);
                }
            }
        }

        np.matchIgnoreCase("file=");
        String outputFile = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");

        long count;
        if (classIds.size() == 0)
            count = MatchesExporter.exportAll(doc.getBlastMode(), doc.getConnector(), outputFile, doc.getProgressListener());
        else
            count = MatchesExporter.export(classificationName, classIds, doc.getBlastMode(), doc.getConnector(), outputFile, doc.getProgressListener());

        NotificationsInSwing.showInformation(getViewer().getFrame(), "Wrote " + count + " matches to file: " + outputFile);

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

        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-ex.blast");
        File lastOpenFile = new File(name);
        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new BlastFileFilter(), new BlastFileFilter(), event, "Save all BLAST matches to file", ".blast");

        String data;
        if (getViewer() instanceof MainViewer) {
            data = Classification.Taxonomy;
        } else if (getViewer() instanceof ClassificationViewer) {
            data = getViewer().getClassName();
        } else
            return;

        if (file != null) {
            String cmd;
            cmd = ("export what=matches data=" + data + " file='" + file.getPath() + "';");
            execute(cmd);
        }
    }

    public String getName() {
        return "Matches...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export all matches to a file in BLAST text format (or only those for selected nodes, if any selected)";
    }
}


