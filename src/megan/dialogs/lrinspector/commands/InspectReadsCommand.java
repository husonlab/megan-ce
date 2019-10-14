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
package megan.dialogs.lrinspector.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.Triplet;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.dialogs.lrinspector.LRInspectorViewer;
import megan.inspector.InspectorWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

public class InspectReadsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "inspector reads=all;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final LRInspectorViewer viewer = ((LRInspectorViewer) getViewer());
        final Director dir = viewer.getDir();

        final InspectorWindow inspectorWindow;
        if (dir.getViewerByClass(InspectorWindow.class) != null)
            inspectorWindow = (InspectorWindow) dir.getViewerByClass(InspectorWindow.class);
        else
            inspectorWindow = (InspectorWindow) dir.addViewer(new InspectorWindow(dir));

        final LinkedList<Triplet<String, Float, Collection<Integer>>> name2Size2Ids = new LinkedList<>();

        final String classificationName = viewer.getClassificationName();
        final int id = viewer.getClassId();
        final String classIdName = viewer.getClassIdName();
        float size = viewer.getController().getTableView().getItems().size();

        final Collection<Integer> ids = ClassificationManager.get(classificationName, true).getFullTree().getAllDescendants(id);
        name2Size2Ids.add(new Triplet<>(classIdName, size, ids));

        SwingUtilities.invokeLater(() -> {
            inspectorWindow.getFrame().setVisible(true);
            inspectorWindow.getFrame().toFront();
            inspectorWindow.getFrame().setState(JFrame.NORMAL);
            inspectorWindow.addTopLevelNode(name2Size2Ids, classificationName);
            final Runnable job = () -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SwingUtilities.invokeLater(() -> inspectorWindow.getFrame().toFront());
            };
            Thread thread = new Thread(job);
            thread.setDaemon(true);
            thread.start();
        });
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() instanceof LRInspectorViewer;
    }

    private final static String NAME = "Inspect...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Inspect reads and their alignments";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Inspector16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}


