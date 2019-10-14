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

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.BasicSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

public class OpenClassificationViewerWebPageCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show url=<url>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show url=");
        String url = np.getWordRespectCase();
        np.matchIgnoreCase(";");

        try {
            if (url != null)
                BasicSwing.openWebPage(new URL(url));
        } catch (Exception e1) {
            Basic.caught(e1);
            NotificationsInSwing.showError(getViewer().getFrame(), "Failed to open URL: " + url);
        }
    }


    public void actionPerformed(ActionEvent event) {
        final ClassificationViewer viewer = (ClassificationViewer) getViewer();

        java.util.List<String> urls = viewer.getURLsForSelection();
        if (urls.size() >= 5 && JOptionPane.showConfirmDialog(getViewer().getFrame(), "Do you really want to open " + urls.size() +
                        " URLs in your browser?", "Confirmation - MEGAN", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon()) != JOptionPane.YES_OPTION)
            return;

        for (String url : urls) {
            try {
                BasicSwing.openWebPage(new URL(url));
            } catch (Exception e1) {
                Basic.caught(e1);
            }
        }
    }

    public boolean isApplicable() {
        ClassificationViewer viewer = (ClassificationViewer) getViewer();
        return viewer != null && (viewer.hasURLsForSelection());
    }

    private static final String NAME = "Open Web Page...";

    public String getName() {
        return NAME;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/WebComponent16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }

    public String getDescription() {
        return "Open web site in browser";
    }

    public boolean isCritical() {
        return true;
    }
}

