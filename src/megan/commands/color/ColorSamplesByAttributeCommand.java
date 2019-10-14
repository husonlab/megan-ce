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
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ColorSamplesByAttributeCommand extends CommandBase implements ICheckBoxCommand {
    @Override
    public boolean isSelected() {
        return ((Director) getDir()).getDocument().getSampleAttributeTable().isSomeSampleColored();
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        if (isSelected()) { // erase all sample colors
            final Document doc = ((Director) getDir()).getDocument();
            for (String sample : doc.getSampleNames()) {
                doc.getSampleAttributeTable().putSampleColor(sample, null);
            }
        } else if (getViewer() instanceof SamplesViewer) {
            final SamplesViewer viewer = ((SamplesViewer) getViewer());
            viewer.getFrame().toFront();
            final String attribute = viewer.getSamplesTableView().getASelectedAttribute();
            if (attribute != null)
                execute("colorBy attribute='" + attribute + "';");
        } else
            execute("show window=samplesViewer;");

    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTableView().getCountSelectedAttributes() > 0;
    }

    public static final String ALT_NAME = "Color Samples By Attributes CBOX";

    public String getAltName() {
        return ALT_NAME;
    }

    public String getName() {
        return "Color Samples By Attributes";
    }

    public String getDescription() {
        return "Determine whether to color samples by attributes";
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

