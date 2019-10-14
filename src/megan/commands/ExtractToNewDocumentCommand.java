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
package megan.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.director.ProjectManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.Single;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.data.ExtractToNewDocument;
import megan.main.MeganProperties;
import megan.rma6.ExtractToNewDocumentRMA6;
import megan.util.RMAFileFilter;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * extract to new document command
 * Daniel Huson, 2.2011, 4.2015
 */
public class ExtractToNewDocumentCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "extract what=document file=<megan-filename> [data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "}]\n" +
                "\t[ids=<SELECTED|numbers...>] [includeCollapsed={true|false}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director srcDir = getDir();
        final MainViewer mainViewer = srcDir.getMainViewer();
        final Document srcDoc = srcDir.getDocument();

        np.matchIgnoreCase("extract what=document");

        np.matchIgnoreCase("file=");
        final String tarFileName = np.getAbsoluteFileName();
        if (srcDir.getDocument().getMeganFile().getFileName().equals(tarFileName))
            throw new IOException("Target file name equals source file name");
        ProgramProperties.put("ExtractToNewFile", tarFileName);

        String classificationName = ClassificationType.Taxonomy.toString();
        if (np.peekMatchIgnoreCase("data")) {
            np.matchIgnoreCase("data=");
            classificationName = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " ") + " readNames");
        }

        final Set<Integer> ids = new HashSet<>();
        if (np.peekMatchIgnoreCase("ids=")) {
            np.matchIgnoreCase("ids=");
            if (np.peekMatchIgnoreCase("selected")) {
                np.matchIgnoreCase("selected");
                if (classificationName.equals(Classification.Taxonomy))
                    ids.addAll(mainViewer.getSelectedIds());
                else if (!classificationName.equalsIgnoreCase("readNames")) {
                    final ClassificationViewer viewer = (ClassificationViewer) srcDir.getViewerByClassName(classificationName);
                    ids.addAll(viewer.getSelectedIds());
                }
            } else {
                while (!np.peekMatchAnyTokenIgnoreCase("includeCollapsed ;"))
                    ids.add(np.getInt());
            }
        }

        boolean allBelow = true;
        if (np.peekMatchIgnoreCase("includeCollapsed")) {
            np.matchIgnoreCase("includeCollapsed=");
            allBelow = np.getBoolean();
        }
        np.matchIgnoreCase(";");


        if (ids.size() == 0) {
            NotificationsInSwing.showWarning("Nothing to extract");
            return;
        }

        if (allBelow) {
            System.err.println("Collecting all ids below selected leaves");
            final Set<Integer> collapsedIds = new HashSet<>();
            if (classificationName.equals(Classification.Taxonomy)) {
                for (Integer id : ids) {
                    if (mainViewer.getCollapsedIds().contains(id))
                        collapsedIds.add(id);
                }
                ids.addAll(TaxonomyData.getTree().getAllDescendants(collapsedIds));

            } else if (!classificationName.equalsIgnoreCase("readNames")) {
                final ClassificationViewer viewer = (ClassificationViewer) srcDir.getViewerByClassName(classificationName);
                if (viewer != null) {
                    for (Integer id : ids) {
                        if (viewer.getCollapsedIds().contains(id))
                            collapsedIds.add(id);

                    }
                    ids.addAll(viewer.getClassification().getFullTree().getAllDescendants(collapsedIds));
                }
            }
        }

        final Director tarDir = Director.newProject(false);
        final Document tarDoc = tarDir.getDocument();
        tarDoc.getMeganFile().setFile(tarFileName, MeganFile.Type.RMA6_FILE);

        boolean userCanceled = false;
        Single<Long> totalReadsExtracted = new Single<>(0L);
        try {
            tarDir.notifyLockInput();
            tarDoc.getActiveViewers().addAll(Arrays.asList(srcDoc.getMeganFile().getConnector().getAllClassificationNames()));
            tarDoc.parseParameterString(srcDoc.getParameterString());
            tarDoc.setBlastMode(srcDoc.getBlastMode());
            tarDoc.setPairedReads(srcDoc.isPairedReads());

            if (srcDoc.getMeganFile().isRMA6File())
                ExtractToNewDocumentRMA6.apply(srcDoc.getMeganFile().getFileName(), classificationName, ids, tarFileName, srcDoc.getProgressListener(), totalReadsExtracted);
            else {
                ExtractToNewDocument.apply(srcDoc, classificationName, ids, tarFileName, srcDoc.getProgressListener(), totalReadsExtracted);
                //tarDoc.processReadHits();
            }

        } catch (CanceledException ex) {
            srcDoc.getProgressListener().setUserCancelled(false);
            userCanceled = true;
        }
        if (totalReadsExtracted.get() == 0) {
            String message = "No reads extracted";
            if (userCanceled)
                message += "\n\tUSER CANCELED";
            NotificationsInSwing.showWarning(message);
            ProjectManager.removeProject(tarDir);
            ProjectManager.updateWindowMenus();
            return;
        }
        String message = String.format("Extracted %,d reads to file '%s'", totalReadsExtracted.get(), tarDoc.getMeganFile().getName());
        if (userCanceled)
            message += "\n\tUSER CANCELED, list of reads may not be complete";
        NotificationsInSwing.showInformation(message);


        tarDoc.neverOpenedReads = false;
        MeganProperties.addRecentFile(tarFileName);

        tarDir.getDocument().getProgressListener().setSubtask("Opening new document");
        if (ProgramProperties.isUseGUI())
            tarDir.execute("toFront;update reprocess=true reset=true reinduce=true;toFront;", tarDir.getCommandManager());
        else {
            srcDir.executeImmediately("close;", srcDir.getCommandManager());
            tarDir.executeImmediately("toFront;update reprocess=true reset=true reinduce=true;", tarDir.getCommandManager());
        }
    }

    public boolean isApplicable() {
        return getViewer() != null && ((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0 &&
                ((ClassificationViewer) getViewer()).getDocument().getMeganFile().hasDataConnector();
    }

    public boolean isCritical() {
        return true;
    }

    public void actionPerformed(ActionEvent event) {
        Director dir = getDir();
        if (!dir.getDocument().getMeganFile().hasDataConnector())
            return;

        String className = null;
        if (getViewer() instanceof ClassificationViewer) {
            final ClassificationViewer viewer = (ClassificationViewer) getViewer();
            Collection<String> selectedLabels = viewer.getSelectedNodeLabels(false);
            if (selectedLabels.size() == 1)
                className = Basic.toCleanName(selectedLabels.iterator().next());
        }
        final String name = ProjectManager.getUniqueName(Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-" + (className != null ? className : "Extracted") + ".rma"));
        final String directory = (new File(ProgramProperties.get("ExtractToNewFile", ""))).getParent();
        File lastOpenFile = new File(directory, name);

        dir.notifyLockInput();
        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new RMAFileFilter(true), new RMAFileFilter(true), event, "Extract selected data to document", ".rma");

        if (file != null) {
            final String data = (getViewer() instanceof ClassificationViewer ? getViewer().getClassName() : ClassificationType.Taxonomy.toString());
            execute("extract what=document file='" + file.getPath() + "' data=" + data + " ids=selected includeCollapsed=true;");
        } else
            dir.notifyUnlockInput();
    }

    public String getName() {
        return "Extract To New Document...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Extract all reads and matches for all selected nodes to a new document";
    }
}

