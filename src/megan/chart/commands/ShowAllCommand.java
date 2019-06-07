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
package megan.chart.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.chart.data.IChartData;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.LabelsJList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * show all
 * Daniel Huson, 7.2012
 */
public class ShowAllCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        ChartViewer viewer = (ChartViewer) getViewer();

        np.matchIgnoreCase("show what=");
        String what = np.getWordMatchesIgnoringCase("all none selected");

        final LabelsJList list;
        if (np.peekMatchIgnoreCase("target=")) {
            np.matchIgnoreCase("target=");
            list = viewer.getLabelsJList(np.getWordMatchesIgnoringCase("series classes attributes"));
        } else {
            list = viewer.getActiveLabelsJList();
        }
        np.matchIgnoreCase(";");


        if (what.equalsIgnoreCase("none")) {
            list.disableLabels(list.getAllLabels());
        } else if (what.equalsIgnoreCase("selected")) {
            list.enableLabels(list.getSelectedLabels());
        } else  // all
        {
            list.enableLabels(list.getAllLabels());
        }

        if (list.getName().equalsIgnoreCase("series"))
            viewer.getChartData().setEnabledSeries(list.getEnabledLabels());
        else if (list.getName().equalsIgnoreCase("classes"))
            ((IChartData) viewer.getChartData()).setEnabledClassNames(list.getEnabledLabels());
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "show what={all|none|selected} [target={series|classes|attributes}];";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("show what=all;");
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Show All";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Show data items";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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
        final LabelsJList list = ((ChartViewer) getViewer()).getActiveLabelsJList();
        return list != null && list.getDisabledLabels().size() > 0;
    }
}
