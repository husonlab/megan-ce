/*
 * ShowParametersDialogCommand.java Copyright (C) 2020. Daniel H. Huson
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
package megan.commands.show;

import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.ContaminantManager;
import megan.core.Document;
import megan.dialogs.lrinspector.LRInspectorViewer;
import megan.dialogs.parameters.ParametersDialog;
import megan.inspector.InspectorWindow;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class ShowParametersDialogCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        final ParametersDialog dialog = new ParametersDialog(getViewer().getFrame(), getDir());
        if (dialog.apply()) {
            execute("recompute " + dialog.getParameterString() + ";");
        }
        dialog.dispose();
    }

    public boolean isApplicable() {
        Document doc = getDoc();
        return !doc.getMeganFile().isReadOnly() && doc.getMeganFile().hasDataConnector() && !doc.getMeganFile().isMeganSummaryFile();
    }

    public String getName() {
        return "Change LCA Parameters...";
    }

    public String getDescription() {
        return "Rerun the LCA analysis with different parameters";
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }
}


