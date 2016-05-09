/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.samplesviewer.commands.attributes;

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * * delete command
 * * Daniel Huson, 9.2015
 */
public class MoveColumnsLeftCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "move attribute=<name> [<name>...] direction={left|right};";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("move attribute=");
        Set<String> attributes = new HashSet<>();
        while (!np.peekMatchIgnoreCase("direction=")) {
            String attribute = np.getWordRespectCase();
            attributes.add(attribute);
        }
        np.matchIgnoreCase("direction=");
        String direction = np.getWordMatchesIgnoringCase("left right");
        np.matchIgnoreCase(";");

        if (attributes.size() > 0) {
            final SamplesViewer viewer = ((SamplesViewer) getViewer());
            viewer.getSamplesTable().moveColumns(direction.equalsIgnoreCase("left"), attributes.toArray(new String[attributes.size()]));
        }
    }

    public void actionPerformed(ActionEvent event) {
        final SamplesViewer viewer = ((SamplesViewer) getViewer());
        final Collection<String> attributes = viewer.getSamplesTable().getSelectedAttributes();

        if (attributes.size() > 0) {
            final StringBuilder buf = new StringBuilder();
            buf.append("move attribute=");
            for (String attributeName : attributes) {
                buf.append(" '").append(attributeName).append("'");
            }
            buf.append(" direction=left;");
            execute(buf.toString());
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTable().getNumberOfSelectedCols() > 0 && !((SamplesViewer) getViewer()).getSamplesTable().getSelectedAttributesIndices().get(1);
    }

    public String getName() {
        return "Move Left";
    }


    public String getDescription() {
        return "Move selected columns";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/navigation/Back16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | java.awt.event.InputEvent.ALT_MASK);

    }
}
