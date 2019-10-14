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

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.ProjectManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.main.MeganProperties;
import megan.samplesviewer.SamplesViewer;
import megan.util.MeganAndRMAFileFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * * add command
 * * Daniel Huson, 9.2012
 */
public class AddSampleFromFileCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "addSample [sample=<name>] source=<filename|pid> ... [overwrite={false|true}];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("addSample");
        Set<Pair<String, String>> sampleAndSources = new HashSet<>();
        do {
            String sampleName = "ALL";
            if (np.peekMatchIgnoreCase("sample")) {
                np.matchIgnoreCase("sample=");
                sampleName = np.getWordRespectCase();
            }
            np.matchIgnoreCase("source=");
            String source = np.getWordFileNamePunctuation();
            sampleAndSources.add(new Pair<>(sampleName, source));
        }
        while (np.peekMatchAnyTokenIgnoreCase("sample source"));

        boolean overwrite = false;
        if (np.peekMatchIgnoreCase("overwrite")) {
            np.matchIgnoreCase("overwrite=");
            overwrite = np.getBoolean();
        }
        np.matchIgnoreCase(";");

        final SamplesViewer samplesViewer = (SamplesViewer) getDir().getViewerByClass(SamplesViewer.class);
        if (samplesViewer != null)
            samplesViewer.getSamplesTableView().syncFromViewToDocument();

        LinkedList<String> skipped = new LinkedList<>();
        Document doc = ((Director) getDir()).getDocument();
        int count = 0;
        for (Pair<String, String> pair : sampleAndSources) {
            String sampleName = pair.get1();
            String source = pair.get2();
            if (Basic.isInteger(source)) { // is a director id
                Director sourceDir = (Director) ProjectManager.getProject(Basic.parseInt(source));
                if (sourceDir == null)
                    throw new IOException("Document not found (pid=" + source + ")");
                if (!sampleName.equalsIgnoreCase("ALL") && !sourceDir.getDocument().getSampleNames().contains(sampleName))
                    throw new IOException("Sample not found in document (pid=" + source + "): " + sampleName);
                if (sampleName.equalsIgnoreCase("ALL")) {
                    for (String sample : sourceDir.getDocument().getSampleNames()) {
                        if (!overwrite && doc.getSampleNames().contains(sample)) {
                            skipped.add(sample);
                        } else {
                            doc.addSample(sample, sourceDir.getDocument());
                            count++;
                        }
                    }
                } else {
                    if (!overwrite && doc.getSampleNames().contains(sampleName)) {
                        skipped.add(sampleName);
                    } else {
                        doc.addSample(sampleName, sourceDir.getDocument());
                        count++;
                    }
                }
            } else  // is a file
            {
                if (!(new File(source)).isFile())
                    throw new IOException("File not found: " + source);
                Director sourceDir = Director.newProject();
                try {
                    sourceDir.executeImmediately("open file='" + source + "';update;", sourceDir.getMainViewer().getCommandManager());
                    if (!sampleName.equalsIgnoreCase("ALL") && !sourceDir.getDocument().getSampleNames().contains(sampleName))
                        throw new IOException("Sample not found in document (pid=" + source + "): " + sampleName);
                    if (sampleName.equalsIgnoreCase("ALL")) {
                        for (String sample : sourceDir.getDocument().getSampleNames()) {
                            if (!overwrite && doc.getSampleNames().contains(sample)) {
                                skipped.add(sample);
                            } else {
                                doc.addSample(sample, sourceDir.getDocument());
                                count++;
                            }
                        }
                    } else {
                        if (!overwrite && doc.getSampleNames().contains(sampleName)) {
                            skipped.add(sampleName);
                        } else {
                            doc.addSample(sampleName, sourceDir.getDocument());
                            count++;
                        }
                    }
                } finally {
                    sourceDir.close();
                }
            }
        }
        if (skipped.size() > 0) {
            NotificationsInSwing.showWarning(getViewer().getFrame(), String.format("Skipped %d samples because their names are already present, e.g.: %s",
                    skipped.size(), skipped.iterator().next()));
        }
        if (count > 0) {
            doc.setDirty(true);
            if (samplesViewer != null)
                samplesViewer.getSamplesTableView().syncFromDocumentToView();
            try {
                doc.processReadHits();
                NotificationsInSwing.showInformation(getViewer().getFrame(), String.format("Added %d reads to file '%s'", count, doc.getMeganFile().getName()));
            } catch (CanceledException e) {
                Basic.caught(e);
            }
            ((Director) getDir()).getMainViewer().setDoReInduce(true);
        }
    }

    public void actionPerformed(ActionEvent event) {
        File lastOpenFile = ProgramProperties.getFile(MeganProperties.MEGANFILE);

        getDir().notifyLockInput();
        List<File> files = ChooseFileDialog.chooseFilesToOpen(getViewer().getFrame(), lastOpenFile, new MeganAndRMAFileFilter(), new MeganAndRMAFileFilter(), event, "Open MEGAN file to add");
        getDir().notifyUnlockInput();

        if (files.size() > 0) {
            StringBuilder command = new StringBuilder("addSample");
            for (File file : files) {
                command.append(" source='").append(file.getPath()).append("'");
            }
            command.append(";");
            execute(command.toString());
            ProgramProperties.put(MeganProperties.MEGANFILE, files.iterator().next().getAbsolutePath());
        }
    }

    public boolean isApplicable() {
        return ((Director) getDir()).getDocument().getMeganFile().isMeganSummaryFile();
    }

    public String getName() {
        return "Add From File...";
    }

    public String getAltName() {
        return "Add Samples From File...";
    }

    public String getDescription() {
        return "Add samples from other documents";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Import16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
