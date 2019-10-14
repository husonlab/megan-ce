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
package megan.samplesviewer.commands;

import javafx.application.Platform;
import javafx.scene.control.ChoiceDialog;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.clipboard.ClipboardBase;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Optional;

public class PasteByAttributeCommand extends ClipboardBase implements ICommand {

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        Platform.runLater(() -> {
            SamplesViewer samplesViewer = (SamplesViewer) getViewer();
            final java.util.List<String> list = getDoc().getSampleAttributeTable().getUnhiddenAttributes();
            if (list.size() > 0) {
                String choice = ProgramProperties.get("PasteByAttribute", list.get(0));
                if (!list.contains(choice))
                    choice = list.get(0);

                final ChoiceDialog<String> dialog = new ChoiceDialog<>(choice, list);
                dialog.setTitle("Paste By Attribute");
                dialog.setHeaderText("Select an attribute to guide paste");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    final String selected = result.get();
                    try {
                        ProgramProperties.put("PasteByAttribute", selected);
                        samplesViewer.getSamplesTableView().pasteClipboardByAttribute(selected);
                        samplesViewer.getSamplesTableView().syncFromViewToDocument();
                        samplesViewer.getCommandManager().updateEnableStateFXItems();
                        if (!samplesViewer.getDocument().isDirty() && samplesViewer.getSamplesTableView().isDirty()) {
                            samplesViewer.getDocument().setDirty(true);
                            samplesViewer.setWindowTitle();
                        }
                    } catch (IOException e) {
                        Basic.caught(e);
                    }
                }
            }
        });
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTableView().getCountSelectedAttributes() > 0;
    }

    private static final String ALT_NAME = "Samples Viewer Paste By Attribute";

    public String getAltName() {
        return ALT_NAME;
    }

    public String getName() {
        return "Paste By Attribute...";
    }

    public String getDescription() {
        return "Paste values guided by values of a selected attribute." +
                "E.g. if you have multiple samples per Subject and you want to add an Age to each sample given by one value per Subject, then paste by 'Subject'";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Paste16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }
}

