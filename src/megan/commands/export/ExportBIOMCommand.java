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
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.biom.biom1.Biom1ExportFViewer;
import megan.biom.biom1.Biom1ExportTaxonomy;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.util.BiomFileFilter;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * export BIOM 1.0 format
 * Daniel Huson, 2012
 */
public class ExportBIOMCommand extends CommandBase implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export format=biom");

        Director dir = getDir();
        Document doc = dir.getDocument();

        np.matchIgnoreCase("data=");
        String data = np.getWordMatchesIgnoringCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));

        np.matchIgnoreCase("file=");
        String outputFile = np.getAbsoluteFileName();

        boolean officialRanksOnly = true;
        if (np.peekMatchIgnoreCase("officialRanksOnly")) {
            np.matchIgnoreCase("officialRanksOnly=");
            officialRanksOnly = np.getBoolean();
        }

        np.matchIgnoreCase(";");
        if (data.equalsIgnoreCase(Classification.Taxonomy)) {
            try {
                int numberOfRows = Biom1ExportTaxonomy.apply(dir, new File(outputFile), officialRanksOnly, doc.getProgressListener());
                NotificationsInSwing.showInformation(getViewer().getFrame(), String.format("Exported %,d rows in BIOM1 format", numberOfRows));
            } catch (Throwable th) {
                Basic.caught(th);
            }
        } else {
            int numberOfRows = Biom1ExportFViewer.apply(getDir(), data, new File(outputFile), doc.getProgressListener());
            NotificationsInSwing.showInformation(getViewer().getFrame(), String.format("Exported %,d rows in BIOM1 format", numberOfRows));
        }
    }

    public boolean isApplicable() {
        return getDoc().getNumberOfReads() > 0 && getViewer() instanceof ViewerBase
                && ((ViewerBase) getViewer()).getSelectedNodes().size() > 0;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "export format=biom data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "} file=<filename> [officialRanksOnly={true|false}];";
    }

    public void actionPerformed(ActionEvent event) {
        final Director dir = getDir();

        boolean officialRanksOnly = false;
        String choice;
        if (getViewer() instanceof MainViewer) {
            choice = Classification.Taxonomy;

            switch (JOptionPane.showConfirmDialog(getViewer().getFrame(), "Export taxa at offical ranks only (KPCOFGS)?", "Setup biom export", JOptionPane.YES_NO_CANCEL_OPTION)) {
                case JOptionPane.CANCEL_OPTION:
                    return;
                case JOptionPane.YES_OPTION:
                    officialRanksOnly = true;
            }
        } else if (getViewer() instanceof ClassificationViewer)
            choice = getViewer().getClassName();
        else
            return;

        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-" + choice + ".biom");

        File lastOpenFile = new File(name);
        String lastDir = ProgramProperties.get("BiomDirectory", "");
        if (lastDir.length() > 0) {
            lastOpenFile = new File(lastDir, name);
        }

        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new BiomFileFilter(BiomFileFilter.Version.V1), new BiomFileFilter(BiomFileFilter.Version.V1), event,
                "Save as BIOM 1.0.0 file (JSON format)", ".txt");

        if (file != null) {
            if (getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getSelectedNodes().size() == 0)
                executeImmediately("select nodes=leaves;");
            ProgramProperties.put("BiomDirectory", file.getParent());
            execute("export format=biom data=" + choice + " file='" + file.getPath() + "'" + (choice.equals(Classification.Taxonomy) ? " officialRanksOnly=" + officialRanksOnly + ";" : ";"));
        }
    }

    public String getName() {
        return "BIOM1 Format...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export data in BIOM 1.0.0 (JSON) format (see http://biom-format.org/documentation/format_versions/biom-1.0.html)";

    }
}

