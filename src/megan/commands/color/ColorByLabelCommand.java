/*
 * ColorByLabelCommand.java Copyright (C) 2020. Daniel H. Huson
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
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ColorTableManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.chart.ChartColorManager;
import megan.core.Director;
import megan.core.Document;
import megan.core.SampleAttributeTable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ColorByLabelCommand extends CommandBase implements ICheckBoxCommand {
    @Override
    public boolean isSelected() {
        return !((Director) getDir()).getDocument().getChartColorManager().isColorByPosition();
    }

    public String getSyntax() {
        return "set colorBy={label|position};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set colorBy=");
        final boolean byPosition = (np.getWordMatchesRespectingCase("label position").equalsIgnoreCase("position"));
        np.matchIgnoreCase(";");

        final Document doc = ((Director) getDir()).getDocument();
        final boolean oldByPosition = doc.getChartColorManager().isColorByPosition();

        doc.getChartColorManager().setColorByPosition(byPosition);
       updateColors(doc);

        if (oldByPosition != byPosition)
            doc.setDirty(true);
    }

    public static void updateColors (Document doc) {
        final boolean byPosition=doc.getChartColorManager().isColorByPosition();
        final List<String> samples=doc.getSampleNames();
        final ChartColorManager colorTableManager=doc.getChartColorManager();
        colorTableManager.clearChangedColors();
        for (int i = 0; i < doc.getNumberOfSamples(); i++) {
            doc.getSampleAttributeTable().put(samples.get(i), SampleAttributeTable.HiddenAttribute.Color, null);
        }
        if(byPosition)
            colorTableManager.setSampleColorPositions(samples);
    }

    public void actionPerformed(ActionEvent event) {

        execute("set colorBy=label;");
    }

    public boolean isApplicable() {
        return true;
    }

    public static final String NAME = "Color Classes By Label";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Color classes by their labels (same label always gets same color)";
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

