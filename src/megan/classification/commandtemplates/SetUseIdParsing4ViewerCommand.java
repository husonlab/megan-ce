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
package megan.classification.commandtemplates;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.director.IDirectableViewer;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * use Id parsing for the given fViewer and mapType
 * Daniel Huson, 10.2015
 */
public class SetUseIdParsing4ViewerCommand extends CommandBase implements ICheckBoxCommand {
    final private String cName;

    public SetUseIdParsing4ViewerCommand(String cName) {
        this.cName = cName;
    }

    public boolean isSelected() {
        return ProgramProperties.get(cName + "ParseIds", false);
    }

    /**
     * commandline syntax
     *
     * @return
     */
    @Override
    public String getSyntax() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        if (isSelected())
            execute("set idParsing=false cName=" + cName + ";");
        else {
            String idTags = Basic.toString(ProgramProperties.get(cName + "Tags", IdMapper.createTags(cName)), " ");
            final JFrame frame = ((getParent() instanceof IDirectableViewer) ? ((IDirectableViewer) getParent()).getFrame() : null);

            idTags = JOptionPane.showInputDialog(frame, "Enter tag(s) used to identify ids (separated by spaces):", idTags);
            if (idTags != null)
                execute("set idParsing=true cName=" + cName + " prefix='" + idTags + "';");
            else
                execute("set idParsing=false cName=" + cName + ";");
        }
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return  !ClassificationManager.isUseFastAccessionMappingMode();
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }


    public static String getAltName(String cName) {
        return "Use Id parsing for " + cName;
    }

    public String getAltName() {
        return getAltName(cName);
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Use Id parsing";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Parse class ids directly from header line using tags";
    }
}
