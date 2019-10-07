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
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.main.MeganProperties;
import megan.util.MeganFileFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ExportSummaryCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
// no need to implement, never called
    }

    public void actionPerformed(ActionEvent event) {
        Director dir = getDir();
        if (dir.getDocument().getMeganFile().hasDataConnector()) {
            final File savedFile = ProgramProperties.getFile(MeganProperties.SAVEFILE);
            final File directory = (savedFile != null ? savedFile.getParentFile() : null);
            final File lastOpenFile = Basic.replaceFileSuffix(new File(directory, dir.getTitle()), ".megan");
            final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new MeganFileFilter(), new MeganFileFilter(), event, "Save MEGAN summary file", ".megan");

            if (file != null) {
                ProgramProperties.put(MeganProperties.SAVEFILE, file);
                String cmd;
                cmd = ("save file='" + file.getPath() + "' summary=true;");
                execute(cmd);
            }
        }
    }

    // if in ask to save summary, modify event source to tell calling method can see that user has canceled

    void replyUserHasCanceledInAskToSavesummary(ActionEvent event) {
        ((Boolean[]) event.getSource())[0] = true;
    }

    public boolean isApplicable() {
        return getDoc().getNumberOfReads() > 0 && getDoc().getMeganFile().hasDataConnector();
    }

    public String getName() {
        return "MEGAN Summary File...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export as summary file";
    }

    public boolean isCritical() {
        return true;
    }
}

