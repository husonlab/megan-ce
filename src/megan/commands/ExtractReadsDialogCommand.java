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
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.data.IName2IdMap;
import megan.dialogs.extractor.ReadsExtractor;
import megan.viewer.TaxonomyData;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;


public class ExtractReadsDialogCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "extract what=reads outDir=<directory> outFile=<filename-template> [data={"
                + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "}][ids=<SELECTED|numbers...>]\n" +
                "\t[names=<names...>] [allBelow={false|true}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final Document doc = dir.getDocument();

        np.matchIgnoreCase("extract what=reads");
        np.matchIgnoreCase("outdir=");
        String outDirectory = np.getWordFileNamePunctuation();
        np.matchIgnoreCase("outfile=");

        String outFile = np.getWordFileNamePunctuation();

        doc.getProgressListener().setTasks("Extracting reads", "Initialization");
        doc.getProgressListener().setMaximum(-1);

        String cName = Classification.Taxonomy;
        if (np.peekMatchIgnoreCase("data")) {
            np.matchIgnoreCase("data=");
            cName = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        }

        final ViewerBase viewer;
        if (cName.equals(Classification.Taxonomy))
            viewer = dir.getMainViewer();
        else viewer = (ViewerBase) dir.getViewerByClassName(cName);

        final Set<Integer> ids = new HashSet<>();
        if (np.peekMatchIgnoreCase("ids=")) {
            np.matchIgnoreCase("ids=");
            if (np.peekMatchIgnoreCase("selected")) {
                np.matchIgnoreCase("selected");
                ids.addAll(viewer.getSelectedIds());
            } else {
                while (!np.peekMatchAnyTokenIgnoreCase("names allBelow ;"))
                    ids.add(np.getInt());
            }
        }
        Set<String> names = new HashSet<>();
        if (np.peekMatchIgnoreCase("names")) {
            np.matchIgnoreCase("names=");
            while (!np.peekMatchAnyTokenIgnoreCase("allBelow ;") && np.peekNextToken() != NexusStreamParser.TT_EOF) {
                names.add(np.getWordRespectCase());
            }
        }

        boolean summary = false;
        if (np.peekMatchIgnoreCase("allBelow")) {
            np.matchIgnoreCase("allBelow=");
            summary = np.getBoolean();
        }
        np.matchIgnoreCase(";");

        if (names.size() > 0) {
            if (cName.equalsIgnoreCase(Classification.Taxonomy)) {
                for (String name : names) {
                    int id = TaxonomyData.getName2IdMap().get(name);
                    if (id != 0)
                        ids.add(id);
                    else
                        System.err.println("Unrecognized name: " + name);
                }
            } else {
                final IName2IdMap map = ClassificationManager.get(cName, true).getName2IdMap();
                for (String name : names) {
                    int id = map.get(name);
                    if (id != 0)
                        ids.add(id);
                    else
                        System.err.println("Unrecognized name: " + name);
                }
            }
        }

        if (ids.size() == 0) {
            NotificationsInSwing.showWarning(viewer.getFrame(), "Nothing to extract");
            return;
        }

        int count;

        if (cName.equalsIgnoreCase(Classification.Taxonomy)) {
            count = ReadsExtractor.extractReadsByTaxonomy(doc.getProgressListener(), ids, outDirectory, outFile, doc, summary);
        } else {
            count = ReadsExtractor.extractReadsByFViewer(cName, doc.getProgressListener(), ids, outDirectory, outFile, doc, true);
        }
        if (count != -1)
            NotificationsInSwing.showInformation(getViewer().getFrame(), "Number of reads written: " + count);
    }

    public void actionPerformed(ActionEvent event) {
    }

    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfReads() > 0;
    }

    private final static String NAME = "Extract Reads...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Extract reads for the selected nodes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Extractor16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}


