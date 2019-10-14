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
package megan.viewer.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.util.CallBack;
import megan.util.PopupChoice;
import megan.viewer.ClassificationViewer;
import megan.viewer.TaxonomicLevels;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * selection command
 * Daniel Huson, 9.2015
 */
public class SelectByRankCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "select rank={" + Basic.toString(TaxonomicLevels.getAllNames(), "|") + "}";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final ClassificationViewer viewer = (ClassificationViewer) getViewer();

        np.matchIgnoreCase("select rank=");
        String rankName = np.getWordMatchesIgnoringCase(Basic.toString(TaxonomicLevels.getAllNames(), " "));
        int rank = TaxonomicLevels.getId(rankName);
        np.matchRespectCase(";");
        if (rank == 0)
            NotificationsInSwing.showError(getViewer().getFrame(), "Unknown rank: " + rankName);
        else {
            viewer.setSelectedIds(ClassificationManager.get(viewer.getClassName(), true).getFullTree().getNodeIdsAtGivenRank(rank, false), true);
        }
        viewer.repaint();
    }

    public void actionPerformed(ActionEvent event) {
        final String[] ranks = TaxonomicLevels.getAllMajorRanks().toArray(new String[0]);

        PopupChoice<String> popupChoice = new PopupChoice<>(ranks, null, new CallBack<>() {
            @Override
            public void call(String choice) {
                execute("select rank='" + choice + "';");

            }
        });
        popupChoice.showAtCurrentMouseLocation(getViewer().getFrame());
    }

    public String getName() {
        return "Rank...";
    }

    public String getAltName() {
        return "Select By Rank";
    }

    public String getDescription() {
        return "Select nodes by rank";
    }

    public ImageIcon getIcon() {
        return null;
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
        return ClassificationManager.get(getViewer().getClassName(), false).getId2Rank().size() > 0;
    }
}
