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
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.LabelsJList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;

public class SortByEnabledCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set sort=enabled;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        ChartViewer chartViewer = (ChartViewer) getViewer();
        final LabelsJList list = chartViewer.getActiveLabelsJList();
        LinkedList<String> disabled = new LinkedList<>(list.getDisabledLabels());
        LinkedList<String> labels = new LinkedList<>();
        labels.addAll(list.getEnabledLabels());
        labels.addAll(list.getDisabledLabels());
        list.sync(labels, list.getLabel2ToolTips(), true);
        list.disableLabels(disabled);
        list.fireSyncToViewer();
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        final LabelsJList list = ((ChartViewer) getViewer()).getActiveLabelsJList();
        return list != null && list.isEnabled() && !list.isDoClustering() && list.getAllLabels().size() > list.getEnabledLabels().size();
    }

    public String getName() {
        return "Group Enabled Entries";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public String getDescription() {
        return "Groups the list of enabled entries";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("GroupEnabledDomains16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

