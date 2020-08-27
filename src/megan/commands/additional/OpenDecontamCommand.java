/*
 * RunBlastOnNCBICommand.java Copyright (C) 2020. Daniel H. Huson
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
 *
 */

package megan.commands.additional;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ProgressDialog;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import megan.blastclient.BlastService;
import megan.blastclient.RemoteBlastClient;
import megan.blastclient.RemoteBlastDialog;
import megan.core.Director;
import megan.core.Document;
import megan.fx.dialogs.decontam.DecontamDialog;
import megan.importblast.ImportBlastDialog;
import megan.util.IReadsProvider;
import megan.util.MeganFileFilter;
import megan.util.WindowUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * open the decontam viewer
 * Daniel Huson, 8/2020
 */
public class OpenDecontamCommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return "open decontam;";
    }

    /**
     * apply the command
     *
     * @param np
     * @throws Exception
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final Director dir = (Director) getDir();

        DecontamDialog viewer = (DecontamDialog) dir.getViewerByClass(DecontamDialog.class);
        if (viewer == null) {
            viewer = new DecontamDialog(getViewer().getFrame(), dir);
            dir.addViewer(viewer);
        } else {
            WindowUtilities.toFront(viewer);
        }
    }

    public boolean isApplicable() {
        return ((Director)getDir()).getDocument().getNumberOfReads()>0;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    private static final String NAME = "Decontam...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Open the Decontam dialog";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
