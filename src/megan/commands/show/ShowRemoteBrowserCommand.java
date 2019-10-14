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
package megan.commands.show;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.remote.RemoteServiceBrowser;
import megan.remote.client.RemoteServiceManager;
import megan.util.WindowUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class ShowRemoteBrowserCommand extends CommandBase implements ICommand {
    private static RemoteServiceBrowser remoteServiceBrowser;

    public String getSyntax() {
        return "show window=RemoteBrowser;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        if (remoteServiceBrowser == null) {
            RemoteServiceManager.setupDefaultService();
            Document doc = new Document();
            Director dir = new Director(doc);
            doc.setDir(dir);

            remoteServiceBrowser = (RemoteServiceBrowser) dir.addViewer(new RemoteServiceBrowser(getViewer().getFrame()));
        }
        WindowUtilities.toFront(remoteServiceBrowser.getFrame());

    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return true;
    }

    private final static String NAME = "Open From Server...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Open browser for remote files";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Open16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
    }
}


