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
package megan.viewer.commands.collapse;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class CollapseAllOtherCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "collapse except={name};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        java.util.List<String> names = np.getWordsRespectCase("collapse except=", ";");

        final ClassificationViewer classificationViewer = (ClassificationViewer) getViewer();

        Set<Integer> doNotCollapse = new HashSet<>();
        for (String name : names) {
            Integer id = classificationViewer.getClassification().getName2IdMap().get(name);
            SortedSet<Node> set = new TreeSet<>(classificationViewer.getNodes(id));
            for (Node v : set) {
                while (true) {
                    doNotCollapse.add((Integer) v.getInfo());
                    if (v.getInDegree() > 0)
                        v = v.getFirstInEdge().getSource();
                    else
                        break;
                }
            }

            while (set.size() > 0) {
                Node w = set.first();
                set.remove(w);
                doNotCollapse.add((Integer) w.getInfo());
                for (Edge e = w.getFirstOutEdge(); e != null; e = w.getNextOutEdge(e))
                    set.add(e.getTarget());
            }
        }
        boolean changed = false;
        {
            for (Node v = classificationViewer.getTree().getFirstNode(); v != null; v = v.getNext()) {
                Integer id = (Integer) v.getInfo();
                if (!doNotCollapse.contains(id)) {
                    classificationViewer.getCollapsedIds().add(id);
                    changed = true;
                }
            }
        }

        if (changed) {
            classificationViewer.updateTree();
            getDoc().setDirty(true);
        }
    }

    public void actionPerformed(ActionEvent event) {
        java.util.Collection<String> labels = ((ClassificationViewer) getViewer()).getSelectedNodeLabels(false);
        if (labels.size() > 0)
            execute("collapse except='" + Basic.toString(labels, "' '") + "';");
    }

    public boolean isApplicable() {
        return getViewer() != null && ((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0;
    }

    public String getName() {
        return "Collapse All Others";
    }

    public String getDescription() {
        return "Collapse all parts of tree that are not above or below the selected nodes";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return null;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }
}

