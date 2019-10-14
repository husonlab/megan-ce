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

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.Pair;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.dialogs.lrinspector.LRInspectorViewer;
import megan.util.WindowUtilities;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * show long read inspector command
 * Daniel Huson, 2.2107
 */
public class ShowLRInspectorCommand extends CommandBase implements ICommand {
    private static final Map<Pair<String, Integer>, LRInspectorViewer> classification2viewer = new HashMap<>();

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show window=longReadInspector");
        final boolean alwaysNew;
        if (np.peekMatchIgnoreCase("alwaysNew")) {
            np.matchIgnoreCase("alwaysNew=");
            alwaysNew = np.getBoolean();
        } else
            alwaysNew = false;
        np.matchIgnoreCase(";");


        if (getViewer() instanceof ClassificationViewer) {
            final Director dir = (Director) getDir();
            final ClassificationViewer parentViewer = (ClassificationViewer) getViewer();
            final String classification = parentViewer.getClassName();

            final Collection<Integer> classIds = parentViewer.getSelectedNodeIds();
            if (classIds.size() > 0) {
                if (classIds.size() >= 5 && JOptionPane.showConfirmDialog(parentViewer.getFrame(), "Do you really want to open " + classIds.size() +
                        " windows?", "Confirmation - MEGAN", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION)
                    return;

                for (Integer classId : classIds) {
                    final Pair<String, Integer> pair = new Pair<>(dir.getID() + "." + classification, classId);
                    LRInspectorViewer viewer = classification2viewer.get(pair);
                    if (viewer != null) {
                        if (dir.getViewers().contains(viewer)) {
                            WindowUtilities.toFront(viewer);
                        } else {
                            classification2viewer.remove(pair);
                            viewer = null;
                        }
                    }
                    if (alwaysNew || viewer == null) {
                        viewer = new LRInspectorViewer(parentViewer.getFrame(), parentViewer, classId);
                        viewer.setRunOnDestroy(() -> classification2viewer.keySet().remove(pair));
                        classification2viewer.put(pair, viewer);
                        dir.addViewer(viewer);
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
        return "show window=longReadInspector [alwaysNew={false|true}];";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("show window=longReadInspector" + ((ev.getModifiers() & ActionEvent.SHIFT_MASK) == 0 ? ";" : " alwaysNew=false;"));
    }

    private static final String NAME = "Inspect Long Reads...";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Show visual long read inspector";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Inspector16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
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
        return getViewer() instanceof ClassificationViewer && ((ClassificationViewer) getViewer()).getDocument().getMeganFile().hasDataConnector()
                && ((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0;
    }
}
