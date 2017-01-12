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

/**
 * * delete command
 * * Daniel Huson, 9.2015
 */
public class MoveColumnsRightCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
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
            buf.append(" direction=right;");
            execute(buf.toString());
        }
    }

    public boolean isApplicable() {
        if (getViewer() instanceof SamplesViewer) {
            SamplesViewer viewer = (SamplesViewer) getViewer();
            return viewer.getSamplesTable().getNumberOfSelectedCols() > 0 && !viewer.getSamplesTable().getSelectedAttributesIndices().get(Math.max(0, viewer.getSamplesTable().getDataGrid().getColumnCount() - 1));
        } else
            return false;
    }

    public String getName() {
        return "Move Right";
    }


    public String getDescription() {
        return "Move selected columns right";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/navigation/Forward16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | java.awt.event.InputEvent.ALT_MASK);
    }
}
