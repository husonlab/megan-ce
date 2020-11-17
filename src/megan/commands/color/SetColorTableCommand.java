/*
 * SetColorTableCommand.java Copyright (C) 2020. Daniel H. Huson
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
 *
 */
package megan.commands.color;


import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IUsesHeatMapColors;
import jloda.swing.util.ColorTableManager;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.commands.ColorByRankCommand;
import megan.core.Director;
import megan.core.Document;
import megan.core.SampleAttributeTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * sets the color table
 * Daniel Huson, 1.2016
 */
public class SetColorTableCommand extends CommandBase implements ICommand {
    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set colorTable={name} [heatMap={false|true}]";
    }


    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Colors...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set colors";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("ColorTable16.gif");
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
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set colorTable=");
        String name = np.getWordMatchesRespectingCase(ColorTableManager.getNames());
        boolean isHeatMap;
        if (np.peekMatchIgnoreCase("heatMap")) {
            np.matchIgnoreCase("heatMap=");
            isHeatMap = np.getBoolean();
        } else
            isHeatMap = false;
        np.matchIgnoreCase(";");

        final Document doc = ((Director) getDir()).getDocument();

        if (isHeatMap) {
            doc.getChartColorManager().setHeatMapTable(name);
            ColorTableManager.setDefaultColorTableHeatMap(name);
        } else {
            doc.getChartColorManager().setColorTable(name);
            ColorTableManager.setDefaultColorTable(name);
        }

        ColorByLabelCommand.updateColors(doc);

        doc.setDirty(true);
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        final String[] choices = ColorTableManager.getNamesOrdered();

        final boolean isHeatMap = (getViewer() instanceof IUsesHeatMapColors) && ((IUsesHeatMapColors) getViewer()).useHeatMapColors();

        final Document doc = ((Director) getDir()).getDocument();

        final String current = isHeatMap ? doc.getChartColorManager().getHeatMapTable().getName() : doc.getChartColorManager().getColorTable().getName();

        final JPopupMenu popMenu = new JPopupMenu();
        for (final String name : choices) {
            JCheckBoxMenuItem checkBoxMenuItem = new JCheckBoxMenuItem();
            checkBoxMenuItem.setAction(new AbstractAction(name) {
                public void actionPerformed(ActionEvent e) {
                    if (doc.getChartColorManager().hasChangedColors()) {
                        switch (JOptionPane.showConfirmDialog(getViewer().getFrame(), "Clear all individually set colors?", "Question", JOptionPane.YES_NO_CANCEL_OPTION)) {
                            case JOptionPane.YES_OPTION:
                                doc.getChartColorManager().clearChangedColors();
                                break;
                            case JOptionPane.NO_OPTION:
                                break;
                            default:
                                return; // canceled
                        }
                    }
                    execute("set colorTable='" + name + "'" + (isHeatMap ? " heatMap=true;" : ";"));
                }
            });
            checkBoxMenuItem.setSelected(name.equals(current));
            checkBoxMenuItem.setIcon(ColorTableManager.getColorTable(name).makeIcon());
            popMenu.add(checkBoxMenuItem);
        }
        popMenu.addSeparator();
        {
            final JMenuItem item = getViewer().getCommandManager().getJMenuItem(ColorByLabelCommand.NAME);
            if (item != null) {
                item.setEnabled(!isHeatMap);
                popMenu.add(item);
            }
        }
        {
            final JMenuItem item = getViewer().getCommandManager().getJMenuItem(ColorByPositionCommand.NAME);
            if (item != null) {
                item.setEnabled(!isHeatMap);
                popMenu.add(item);
            }
        }

        {
            final JMenuItem item = getViewer().getCommandManager().getJMenuItem(ColorSamplesByAttributeCommand.ALT_NAME);
            if (item != null) {
                popMenu.addSeparator();
                item.setEnabled(!isHeatMap);
                popMenu.add(item);
            }
        }
        {
            final JMenuItem item = getViewer().getCommandManager().getJMenuItem(ColorByRankCommand.NAME);
            if (item != null) {
                popMenu.addSeparator();
                item.setEnabled(!isHeatMap);
                popMenu.add(item);
            }
        }
        popMenu.addSeparator();
        {
            final JMenuItem item = getViewer().getCommandManager().getJMenuItem(UseProgramColorsCommand.NAME);
            if (item != null) {
                popMenu.add(item);
            }
        }

        final Point location = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(location, getViewer().getFrame());
        popMenu.show(getViewer().getFrame(), location.x, location.y);
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
