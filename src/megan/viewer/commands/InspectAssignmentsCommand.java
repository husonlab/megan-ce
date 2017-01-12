/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.graph.Node;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.Triplet;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.inspector.InspectorWindow;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class InspectAssignmentsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "inspector nodes=selected;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final Director dir = ((ClassificationViewer) getViewer()).getDir();
        final ClassificationViewer classificationViewer = ((ClassificationViewer) getViewer());

        final InspectorWindow inspectorWindow;
        if (dir.getViewerByClass(InspectorWindow.class) != null)
            inspectorWindow = (InspectorWindow) dir.getViewerByClass(InspectorWindow.class);
        else
            inspectorWindow = (InspectorWindow) dir.addViewer(new InspectorWindow(dir));

            final LinkedList<Triplet<String, Integer, Collection<Integer>>> name2Size2Ids = new LinkedList<>();
            for (Integer id : classificationViewer.getSelectedIds()) {
                String name = classificationViewer.getClassification().getName2IdMap().get(id);
                Node v = classificationViewer.getANode(id);
                if (v.getOutDegree() > 0) { // internal node
                    int size = classificationViewer.getNodeData(v).getCountAssigned();
                    name2Size2Ids.add(new Triplet<>(name, size, (Collection<Integer>) Collections.singletonList(id)));
                } else {
                    int size = classificationViewer.getNodeData(v).getCountSummarized();
                    final Collection<Integer> ids = classificationViewer.getClassification().getFullTree().getAllDescendants(id);
                    name2Size2Ids.add(new Triplet<>(name, size, ids));
                }
            }
            if (name2Size2Ids.size() > 0)
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        inspectorWindow.getFrame().setVisible(true);
                        inspectorWindow.getFrame().toFront();
                        inspectorWindow.getFrame().setState(JFrame.NORMAL);
                        inspectorWindow.addTopLevelNode(name2Size2Ids, classificationViewer.getClassName());
                    }
                });
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() != null && ((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0 &&
                ((ClassificationViewer) getViewer()).getDocument().getMeganFile().hasDataConnector();
    }

    final public static String NAME = "Inspect...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Inspect the read-to-function assignments";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Inspector16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }
}


