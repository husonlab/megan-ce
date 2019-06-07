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
package megan.commands.clipboard;

import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class CopyCommand extends ClipboardBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        IDirectableViewer viewer = getViewer();
        if (viewer instanceof ViewerBase) {
            ViewerBase graphView = (ViewerBase) viewer;
            if (graphView.getSelectedNodes().size() > 0) {
                StringBuilder buf = new StringBuilder();
                boolean first = true;
                for (String label : graphView.getSelectedNodeLabels(true)) {
                    if (label != null) {
                        if (first)
                            first = false;
                        else
                            buf.append("\n");
                        buf.append(label);
                    }
                }
                if (buf.toString().length() > 0) {
                    StringSelection selection = new StringSelection(buf.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                    return;
                }
            }
        }
        Action action = findAction(DefaultEditorKit.copyAction);
        if (action != null)
            action.actionPerformed(event);
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Copy";
    }

    public String getDescription() {
        return "Copy";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Copy16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}

