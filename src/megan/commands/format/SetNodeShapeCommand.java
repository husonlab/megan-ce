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
package megan.commands.format;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.graphview.NodeShape;
import jloda.swing.graphview.NodeShapeIcon;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.util.CallBack;
import megan.util.PopupChoice;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * draw selected nodes as circles
 * Daniel Huson, 3.2013
 */
public class SetNodeShapeCommand extends CommandBase implements ICommand {
    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set nodeShape={" + Basic.toString(NodeShape.values(), "|") + "} sample=<name name...>;";
    }

    /**
     * apply
     *
     * @param np
     * @throws Exception
     */
    public void apply(NexusStreamParser np) throws Exception {
        final Document doc = ((Director) getDir()).getDocument();

        np.matchIgnoreCase("set nodeShape=");
        String shape = np.getWordMatchesIgnoringCase(Basic.toString(NodeShape.values(), " "));
        final List<String> samples = new LinkedList<>();
        if (np.peekMatchIgnoreCase("sample=")) {
            np.matchIgnoreCase("sample=");
            while (!np.peekMatchIgnoreCase(";")) {
                samples.add(np.getWordRespectCase());
            }
        }
        np.matchIgnoreCase(";");

        if (samples.size() == 0)
            samples.addAll(doc.getSampleSelection().getAll());

        if (samples.size() > 0) {
            for (String sample : samples) {
                doc.getSampleAttributeTable().putSampleShape(sample, shape);
            }
            ((Director) getDir()).getDocument().setDirty(true);
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        final Icon[] icons = new Icon[NodeShape.values().length];
        for (int i = 0; i < NodeShape.values().length; i++) {
            icons[i] = new NodeShapeIcon(NodeShape.values()[i], 12, new Color(0, 174, 238));
        }

        PopupChoice<NodeShape> popupChoice = new PopupChoice<>(NodeShape.values(), null, icons, new CallBack<>() {
            @Override
            public void call(NodeShape choice) {
                execute("set nodeShape=" + choice.toString() + ";");
            }
        });
        popupChoice.showAtCurrentMouseLocation(getViewer().getFrame());
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    public boolean isApplicable() {
        final Document doc = ((Director) getDir()).getDocument();
        return doc.getSampleSelection().size() > 0;
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Node Shape...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the node shape";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("BlueTriangle16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
