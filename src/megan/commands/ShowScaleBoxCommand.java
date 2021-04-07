/*
 * ShowLabelsCommand.java Copyright (C) 2021. Daniel H. Huson
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
package megan.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ClassificationViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * show scale bar command
 * Daniel Huson, 2.2020
 */
public class ShowScaleBoxCommand extends CommandBase implements ICheckBoxCommand {
    public String getSyntax() {
        return "show scaleBox={true|false};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show scaleBox=");
        final boolean show=np.getBoolean();
        np.matchIgnoreCase(";");

        ViewerBase viewer = (ViewerBase) getViewer();
        if(viewer instanceof ClassificationViewer) {
            ((ClassificationViewer) viewer).setShowScaleBox(show);
            ProgramProperties.put("ShowScaleBox",show);
            viewer.repaint();
        }
    }

    @Override
    public boolean isSelected() {
        return getViewer() instanceof ClassificationViewer && ((ClassificationViewer)getViewer()).isShowScaleBox();
    }

    public void actionPerformed(ActionEvent event) {
        execute("show scaleBox="+(!isSelected())+";");
    }

    public boolean isApplicable() {
        return getViewer() instanceof ClassificationViewer;
    }

    public String getName() {
        return "Show Scale Bar";
    }

    public String getDescription() {
        return "Show scale bar";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

