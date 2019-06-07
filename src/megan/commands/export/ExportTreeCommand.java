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
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.dialogs.export.ExportTree;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class ExportTreeCommand extends CommandBase implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=tree");

        np.matchIgnoreCase("file=");
        String outputFile = np.getAbsoluteFileName();

        boolean simplify = false;
        if (np.peekMatchIgnoreCase("simplify")) {
            np.matchIgnoreCase("simplify=");
            simplify = np.getBoolean();
        }
        boolean showInternalLabels = true;
        if (np.peekMatchIgnoreCase("showInternalLabels")) {
            np.matchIgnoreCase("showInternalLabels=");
            showInternalLabels = np.getBoolean();
        }
        boolean showUnassigned = true;
        if (np.peekMatchIgnoreCase("showUnassigned")) {
            np.matchIgnoreCase("showUnassigned=");
            showUnassigned = np.getBoolean();
        }

        np.matchIgnoreCase(";");

        if (getViewer() instanceof ViewerBase) {
            ViewerBase viewer = (ViewerBase) getViewer();
            BufferedWriter w = new BufferedWriter(new FileWriter(outputFile));
            ExportTree.apply(viewer, w, showInternalLabels, showUnassigned, simplify);
            w.close();
        } else
            System.err.println("Invalid command");
    }

    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getSelectedNodes().size() > 0;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "export what=tree file=<filename> [simplify={false|true}] [showInternalLabels={true|false}] [showUassigned={true|false}];";
    }

    public void actionPerformed(ActionEvent event) {
        Director dir = getDir();

        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), ".tre");
        File lastOpenFile = new File(name);
        String lastDir = ProgramProperties.get("TreeDirectory", "");
        if (lastDir.length() > 0) {
            lastOpenFile = new File(lastDir, name);
        }


        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), event, "Save as tree", ".txt");

        if (file != null) {
            execute("export what=tree file='" + file.getPath() + "' showInternalLabels=true showUnassigned=true;");
            ProgramProperties.put("TreeDirectory", file.getParent());
        }
    }

    public String getName() {
        return "Tree...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export induced tree (in Newick format)";
    }
}

