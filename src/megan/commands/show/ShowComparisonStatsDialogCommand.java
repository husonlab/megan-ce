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

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.stats.ComparisonStatsWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ShowComparisonStatsDialogCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=comparisonStats;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        Director dir = getDir();
        new ComparisonStatsWindow(getViewer().getFrame(), dir);
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Statistical Comparison...";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }

    public String getDescription() {
        return "Open dialog to produce a statistical comparison of two datasets";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/History16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

