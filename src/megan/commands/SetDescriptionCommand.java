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
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.core.SampleAttributeTable;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 1.2015
 */
public class SetDescriptionCommand extends CommandBase implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Description...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Edit or show the description of the data";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Command16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
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
        np.matchIgnoreCase("set description=");
        String description = np.getWordRespectCase();
        np.matchIgnoreCase(";");
        getDoc().getSampleAttributeTable().put(getDoc().getSampleNames().get(0), SampleAttributeTable.DescriptionAttribute, description);
        getDoc().setDirty(true);

    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        Document doc = getDoc();
        if ((doc.getMeganFile().isRMA2File() || doc.getMeganFile().isRMA3File()) && !doc.getMeganFile().isReadOnly()) {
            Object object = doc.getSampleAttributeTable().get(doc.getSampleNames().get(0), SampleAttributeTable.DescriptionAttribute);
            if (object == null)
                object = "";
            String description = JOptionPane.showInputDialog(getViewer().getFrame(), "A short description:", object);
            if (description != null) {
                description = description.replaceAll("^ +| +$|( )+", "$1"); // replace all white spaces by a single spac
                execute("set description='" + description + "';");
            }
        } else {
            StringBuilder buf = new StringBuilder();
            for (String name : doc.getSampleNames()) {
                Object object = doc.getSampleAttributeTable().get(name, SampleAttributeTable.DescriptionAttribute);
                if (object != null) {
                    buf.append(name).append(": ").append(object).append("\n");
                }
            }
            if (buf.length() > 0) {
                NotificationsInSwing.showInformation(getViewer().getFrame(), "Description:\n" + buf.toString());
            }
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
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return (getDoc().getMeganFile().isRMA2File() || getDoc().getMeganFile().isRMA3File()) ||
                (getDoc().getSampleNames().size() > 0 && getDoc().getSampleAttributeTable().get(getDoc().getSampleNames().get(0), SampleAttributeTable.DescriptionAttribute) != null);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set  description=<text>;";
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
