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
package megan.viewer.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.util.Message;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Document;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ShowVersionInfoCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show versionInfo;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final ClassificationViewer classificationViewer = (ClassificationViewer) getViewer();
        String version = Document.getVersionInfo().get(classificationViewer.getClassName() + " tree");
        if (version != null)
            new Message(getViewer().getFrame(), classificationViewer.getClassName() + " classification:\n" + version);
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() != null && getViewer() instanceof ClassificationViewer && Document.getVersionInfo().get(getViewer().getClassName() + " tree") != null;
    }

    private final static String NAME = "Show Info...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Show version info on this classification";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Help16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}


