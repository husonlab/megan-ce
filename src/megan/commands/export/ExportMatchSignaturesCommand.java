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
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.FastaFileFilter;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.export.MatchSignaturesExporter;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ExportMatchSignaturesCommand extends CommandBase implements ICommand {
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=matchPatterns");

        final Document doc = getDir().getDocument();

        np.matchIgnoreCase("taxon=");
        String label = np.getWordRespectCase();
        int taxonId;
        if (Basic.isInteger(label))
            taxonId = Basic.parseInt(label);
        else
            taxonId = TaxonomyData.getName2IdMap().get(label);
        np.matchIgnoreCase("rank=");
        String rank = np.getWordMatchesRespectingCase(Basic.toString(TaxonomicLevels.getAllNames(), " "));

        np.matchIgnoreCase("file=");
        String outputFile = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");

        MatchSignaturesExporter.export(doc.getConnector(), taxonId, rank, doc.getMinScore(), doc.getMaxExpected(), doc.getMinPercentIdentity(), doc.getTopPercent(), outputFile, doc.getProgressListener());
    }

    public boolean isApplicable() {
        return getDoc().getMeganFile().hasDataConnector() && getDir().getMainViewer().getSelectedIds().size() == 1;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "export what=matchPatterns taxon=<id or name> rank=<name> file=<filename>;";
    }

    public void actionPerformed(ActionEvent event) {
        Director dir = getDir();
        if (!isApplicable())
            return;
        int taxon = getDir().getMainViewer().getSelectedIds().iterator().next();

        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-ex.txt");
        File lastOpenFile = new File(name);

        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new FastaFileFilter(), new FastaFileFilter(), event, "Save all READs to file", ".fasta");

        if (file != null) {
            String cmd;
            cmd = ("export what=matchPatterns taxon=" + taxon + " rank=Genus file='" + file.getPath() + "';");
            execute(cmd);
        }
    }

    public String getName() {
        return "Match Signatures...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export all match signatures for the select node";
    }
}


