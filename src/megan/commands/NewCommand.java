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
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * new main viewer command
 * Daniel Huson, 6.2010
 */
public class NewCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "new document;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        makeNewDocument();
    }

    public static void makeNewDocument() {
        Director newDir = Director.newProject();
        newDir.getMainViewer().getFrame().setVisible(true);
        newDir.getMainViewer().setDoReInduce(true);
        newDir.getMainViewer().setDoReset(true);
        newDir.execute(";", newDir.getMainViewer().getCommandManager());
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "New...";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public String getDescription() {
        return "Open a new empty document";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/New16.gif");
    }

    public boolean isCritical() {
        return false;
    }
}
