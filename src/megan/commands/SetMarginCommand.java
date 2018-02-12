/*
 *  Copyright (C) 2018 Daniel H. Huson
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
package megan.commands;

import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SetMarginCommand extends CommandBase implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set margin");

        ViewerBase viewer = getViewer() instanceof ViewerBase ? (ViewerBase) getViewer() : null;

        while (!(np.peekMatchIgnoreCase(";"))) {
            if (np.peekMatchIgnoreCase("left")) {
                np.matchIgnoreCase("left=");
                if (viewer != null)
                    viewer.trans.setLeftMargin(np.getInt());
            } else if (np.peekMatchIgnoreCase("right")) {
                np.matchIgnoreCase("right=");
                if (viewer != null)
                    viewer.trans.setRightMargin(np.getInt());
            } else if (np.peekMatchIgnoreCase("top")) {
                np.matchIgnoreCase("top=");
                if (viewer != null)
                    viewer.trans.setTopMargin(np.getInt());
            } else if (np.peekMatchIgnoreCase("bottom")) {
                np.matchIgnoreCase("bottom=");
                if (viewer != null)
                    viewer.trans.setBottomMargin(np.getInt());
            } else
                np.matchAnyTokenIgnoreCase("left right bottom top"); // will throw an exception
        }
        np.matchIgnoreCase(";");
        if (viewer != null)
            System.out.println("margin: left=" + viewer.trans.getLeftMargin()
                    + " right=" + viewer.trans.getRightMargin() + " top=" + viewer.trans.getTopMargin()
                    + " bottom=" + viewer.trans.getBottomMargin());
        if (viewer != null)
            viewer.fitGraphToWindow();
    }

    public boolean isApplicable() {
        return getViewer() != null && getViewer() instanceof ViewerBase;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "set margin [left=<number>] [right=<number>] [bottom=<number>] [top=<number>];";
    }

    public void actionPerformed(ActionEvent event) {
        if (getViewer() instanceof ViewerBase) {
            ViewerBase viewer = (ViewerBase) getViewer();
            String input = "left=" + viewer.trans.getLeftMargin()
                    + " right=" + viewer.trans.getRightMargin() + " top=" + viewer.trans.getTopMargin()
                    + " bottom=" + viewer.trans.getBottomMargin();
            input = JOptionPane.showInputDialog(viewer.getFrame(), "Set margin", input);
            if (input != null) {
                input = input.trim();
                if (input.length() > 0 && !input.equals(";")) {
                    if (!input.endsWith(";"))
                        input += ";";
                    execute("set margin=" + input);
                }
            }
        }
    }

    public String getName() {
        return "Set Margins...";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Set margins used in tree visualization";
    }
}
