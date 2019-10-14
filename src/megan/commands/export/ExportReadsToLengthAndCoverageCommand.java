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

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.export.ExportReads2LengthAndAlignmentCoverage;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * export reads in length and amount covered
 * Daniel Huson, 3.2017
 */
public class ExportReadsToLengthAndCoverageCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=lengthAndCovered file=");
        final String fileName = np.getWordFileNamePunctuation();
        np.matchIgnoreCase(";");

        if (getViewer() instanceof ClassificationViewer) {
            final ClassificationViewer viewer = (ClassificationViewer) getViewer();
            final Document doc = viewer.getDocument();
            System.err.println("Writing file: " + fileName);
            int lines = ExportReads2LengthAndAlignmentCoverage.apply(viewer, new File(fileName), doc.getProgressListener());
            System.err.println("done (" + lines + ")");
        }
    }

    public String getSyntax() {
        return "export what=lengthAndCovered file=<file-name>";
    }

    public void actionPerformed(ActionEvent event) {
        final Director dir = (Director) getDir();

        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), ".txt");
        String lastGFFFile = ProgramProperties.get("lastExportFile", "");
        File lastOpenFile = new File((new File(lastGFFFile)).getParent(), name);

        final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), event, "Save read length and covered bases", ".txt");

        if (file != null) {
            ProgramProperties.put("lastExportFile", file.getPath());
            execute("export what=lengthAndCovered file='" + file.getPath() + "';");
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof ClassificationViewer && ((ClassificationViewer) getViewer()).getNumberSelectedNodes() > 0;
    }

    private static final String NAME = "Export Read Lengths and Coverage...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Export length and number of bases covered for all selected reads";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
