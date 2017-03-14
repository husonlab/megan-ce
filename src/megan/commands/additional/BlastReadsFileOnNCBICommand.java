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

package megan.commands.additional;

import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.FastaFileFilter;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.blastclient.RemoteBlastDialog;
import megan.core.Director;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * analyse a sequence on NCBI
 * Daniel Huson, 3/2017
 */
public class BlastReadsFileOnNCBICommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    /**
     * apply the command
     *
     * @param np
     * @throws Exception
     */
    public void apply(NexusStreamParser np) throws Exception {

    }

    public void actionPerformed(ActionEvent event) {
        final String lastDir = ProgramProperties.get("RemoteBlastDir", System.getProperty("user.dir"));
        final File lastOpenFile = new File(lastDir, ProgramProperties.get("RemoteBlastFile", ""));
        final File file = ChooseFileDialog.chooseFileToOpen(getViewer().getFrame(), lastOpenFile, new FastaFileFilter(), new FastaFileFilter(), event, "Open FastA file");

        if (file.exists()) {
            String firstWord = Basic.swallowLeadingGreaterSign(Basic.getFirstWord(Basic.getFirstLineFromFile(file)));
            final String commandString = RemoteBlastDialog.apply(getViewer(), (Director) getDir(), null, file.getPath(), firstWord);
            if (commandString != null) {
                final Director newDir = Director.newProject();
                newDir.getMainViewer().getFrame().setVisible(true);
                newDir.getMainViewer().setDoReInduce(true);
                newDir.getMainViewer().setDoReset(true);
                newDir.executeImmediately(commandString, newDir.getMainViewer().getCommandManager());
                getCommandManager().updateEnableState(NAME);
            }
        }
    }

    public boolean isApplicable() {
        return true;
    }

    public static final String NAME = "BLAST Reads on NCBI...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Remotely BLAST small file of reads on NCBI website";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Import16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
