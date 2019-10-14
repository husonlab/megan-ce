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
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.chart.TaxaChart;
import megan.chart.data.IChartData;
import megan.chart.gui.ChartViewer;
import megan.viewer.TaxonomicLevels;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ColorByRankCommand extends CommandBase implements ICheckBoxCommand {
    private final String[] ranks = new String[]{TaxonomicLevels.Domain, TaxonomicLevels.Phylum, TaxonomicLevels.Class,
            TaxonomicLevels.Order, TaxonomicLevels.Family, TaxonomicLevels.Genus, TaxonomicLevels.Species, "None"};

    @Override
    public boolean isSelected() {
        return isApplicable() && ((TaxaChart) getViewer()).getColorByRank() != null && !((TaxaChart) getViewer()).getColorByRank().equalsIgnoreCase("none");
    }

    public String getSyntax() {
        return "colorBy rank={" + Basic.toString(ranks, "|") + "};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("colorBy rank=");
        String rank = np.getWordMatchesRespectingCase(Basic.toString(ranks, " "));
        np.matchIgnoreCase(";");
        if (rank != null) {
            ChartViewer chartViewer = (ChartViewer) getViewer();
            if (chartViewer instanceof TaxaChart) {
                TaxaChart taxaChart = (TaxaChart) chartViewer;
                taxaChart.setColorByRank(rank);
                taxaChart.updateColorByRank();
            }
            chartViewer.repaint();
        }
    }

    public void actionPerformed(ActionEvent event) {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        String choice = ranks[ranks.length - 1];
        if (chartViewer instanceof TaxaChart) {
            TaxaChart taxaChart = (TaxaChart) chartViewer;
            if (taxaChart.getColorByRank() != null)
                choice = taxaChart.getColorByRank();
        }

        String result = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Choose rank for coloring", "Choose colors", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), ranks, choice);

        if (result != null)
            execute("colorBy rank=" + result + ";");
    }

    public boolean isApplicable() {
        ChartViewer chartViewer = (ChartViewer) getViewer();
        return chartViewer != null && chartViewer.getChartDrawer() != null && chartViewer.getChartDrawer().canColorByRank() && chartViewer.getChartData() instanceof IChartData && chartViewer instanceof TaxaChart;
    }

    public static final String NAME = "Color Taxa by Rank...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Color classes by taxonomic rank";
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

