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
package megan.commands.show;


import jloda.gui.Message;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * how to cite
 * Daniel Huson, 6.2010
 */
public class ShowHowToCiteCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=howToCite;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        new Message(getViewer().getFrame(),
                "Please cite:\nD.H. Huson et al., MEGAN Community Edition - Interactive exploration and 2 analysis of large-scale microbiome sequencing data, under review\n" +
                        "D.H. Huson et al., Integrative analysis of environmental sequences using MEGAN 4, Genome Res. 2011. 21:1552-1560.");
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "How to Cite...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Help16.gif");
    }

    public String getDescription() {
        return "Show how to cite the program";
    }

    public boolean isCritical() {
        return false;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
