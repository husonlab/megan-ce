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

import jloda.graph.Node;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.util.WindowUtilities;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class ShowAlignmentViewerCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=aligner;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show window=aligner");
        np.matchIgnoreCase(";");

        final Director dir = getDir();
        final Document doc = dir.getDocument();
        final ClassificationViewer classificationViewer = (ClassificationViewer) getViewer();

        if (classificationViewer.getSelectedNodeIds().size() == 1) {
            final int id = classificationViewer.getSelectedNodeIds().iterator().next();
            final Node v = classificationViewer.getANode(id);
            if (v != null) {
                final String name = classificationViewer.getLabel(v);
                final Set<Integer> ids = new HashSet<>();
                ids.add(id);
                if (v.getOutDegree() == 0) {
                    ids.addAll(classificationViewer.getClassification().getFullTree().getAllDescendants(id));
                }
                final AlignmentViewer alignerViewer = new AlignmentViewer(dir);
                dir.addViewer(alignerViewer);
                alignerViewer.getBlast2Alignment().loadData(classificationViewer.getClassName(), ids, name, doc.getProgressListener());
                WindowUtilities.toFront(alignerViewer);
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        execute("show window=aligner;");
    }

    public boolean isApplicable() {
        final Document doc = getDir().getDocument();
        final ClassificationViewer classificationViewer = (ClassificationViewer) getViewer();
        return classificationViewer != null && doc.getMeganFile().hasDataConnector() && classificationViewer.getSelectedNodeIds().size() == 1;
        // && (doc.getBlastMode().equals(BlastModeUtils.BlastX) || (doc.getBlastMode().equals(BlastModeUtils.BlastN) || (doc.getBlastMode().equals(BlastModeUtils.BlastP))));
    }

    private final static String NAME = "Show Alignment...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Show alignment of reads to a specified reference sequence";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Alignment16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }
}


