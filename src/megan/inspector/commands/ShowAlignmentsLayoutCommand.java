/*
 *  Copyright (C) 2015 Daniel H. Huson
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
package megan.inspector.commands;

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Pair;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockGetter;
import megan.fxdialogs.matchlayout.MatchLayoutViewer;
import megan.inspector.InspectorWindow;
import megan.inspector.ReadHeadLineNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * show alignment command
 * Daniel Huson, 2.2107
 */
public class ShowAlignmentsLayoutCommand extends CommandBase implements ICommand {
    private static Map<Pair<Integer, String>, MatchLayoutViewer> readName2Viewer = new HashMap<>();

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final Director dir = (Director) getDir();
        final InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        final IConnector connector = dir.getDocument().getConnector();

        final TreePath[] selectedPaths = inspectorWindow.getDataTree().getSelectionPaths();
        if (selectedPaths != null) {
            for (TreePath selectedPath : selectedPaths) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();

                if (node instanceof ReadHeadLineNode) {
                    final ReadHeadLineNode readHeadLine = (ReadHeadLineNode) node;

                    MatchLayoutViewer viewer = readName2Viewer.get(new Pair<>(dir.getID(), readHeadLine.getName()));
                    if (viewer != null) {
                        viewer.setVisible(true);
                        viewer.toFront();
                    } else {
                        try (IReadBlockGetter readBlockGetter = connector.getReadBlockGetter(0, 100000, true, true)) {
                            final IReadBlock readBlock = readBlockGetter.getReadBlock(readHeadLine.getUId());

                            final MatchLayoutViewer matchLayoutViewer = new MatchLayoutViewer(inspectorWindow.getFrame(), dir, readBlock);
                            matchLayoutViewer.runOnDestroy(new Runnable() {
                                @Override
                                public void run() {
                                    readName2Viewer.keySet().remove(new Pair<>(dir.getID(), readBlock.getReadName()));
                                }
                            });
                            dir.addViewer(matchLayoutViewer);
                            readName2Viewer.put(new Pair<>(dir.getID(), readBlock.getReadName()), matchLayoutViewer);
                        }
                    }
                }
            }
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "show alignmentLayout;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately(getSyntax());
    }

    public static final String NAME = "Show Layout...";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Show layout of all alignments for this node";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        final InspectorWindow inspectorWindow = (InspectorWindow) getViewer();

        return inspectorWindow.getDataTree() != null && inspectorWindow.getDataTree().getSelectionCount() == 1 && inspectorWindow.getDataTree().getSelectionPath().getLastPathComponent() instanceof ReadHeadLineNode;
    }
}
