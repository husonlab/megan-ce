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
package megan.commands.export;

import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.TextFileFilter;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.export.ExportAlignedReads2GFF;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * export reads in GFF format
 * Daniel Huson, 3.2017
 */
public class ExportReadsToGFFCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=GFF file=");
        final String fileName = np.getWordFileNamePunctuation();
        np.matchIgnoreCase(";");

        if (getViewer() instanceof ClassificationViewer) {
            final ClassificationViewer viewer = (ClassificationViewer) getViewer();
            final Document doc = viewer.getDocument();
            final String[] cnames = doc.getActiveViewers().toArray(new String[doc.getActiveViewers().size()]);

            System.err.println("Writing file: " + fileName);
            int lines = ExportAlignedReads2GFF.apply(viewer, cnames, new File(fileName), doc.getProgressListener());
            System.err.println("done (" + lines + ")");
        }
    }

    public String getSyntax() {
        return "export what=GFF file=<file-name>";
    }

    public void actionPerformed(ActionEvent event) {
        final Director dir = (Director) getDir();

        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), ".gff");
        String lastGFFFile = ProgramProperties.get("lastGFFFile", "");
        File lastOpenFile = new File((new File(lastGFFFile)).getParent(), name);

        final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(".gff"), new TextFileFilter(".gff"), event, "Save read annotations to file", ".gff");

        if (file != null) {
            ProgramProperties.put("lastGFFFile", file.getPath());
            execute("export what=GFF file='" + file.getPath() + "';");
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof ClassificationViewer && ((ClassificationViewer) getViewer()).getNumberSelectedNodes() > 0;
    }

    public static final String NAME = "Export Reads in GFF Format...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Export selected read annotations in GFF format";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Export16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
