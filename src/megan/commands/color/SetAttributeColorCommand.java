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
package megan.commands.color;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.ChartColorManager;
import megan.core.Director;
import megan.core.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * set color by attribute
 * Daniel Huson, 7.2012
 */
public class SetAttributeColorCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {

        np.matchIgnoreCase("set color=");
        final Color color = np.getColor();
        np.matchIgnoreCase("attribute=");
        final String attribute = np.getLabelRespectCase();
        np.matchIgnoreCase("state=");
        final String state = np.getLabelRespectCase();
        if (attribute != null && state != null) {
            final Document doc = ((Director) getDir()).getDocument();
            final ChartColorManager chartColorManager = doc.getChartColorManager();
            chartColorManager.setAttributeStateColor(attribute, state, color);
            for (String sample : doc.getSampleNames()) {
                chartColorManager.setSampleColor(sample, chartColorManager.getAttributeStateColor(attribute, doc.getSampleAttributeTable().get(sample, attribute)));
            }
            doc.setDirty(true);
            ((Director) getDir()).updateView(Director.ALL);
        }
        np.matchIgnoreCase(";");
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set color=<color> attribute=<name> state=<name>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
    }

    public String getName() {
        return null;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the color for an attribute value";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("YellowSquare16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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
        return true;
    }
}
