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
package megan.commands;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.parse.NexusStreamParser;
import megan.classification.IdMapper;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * list assignments to different levels
 * Daniel Huson, 7.2010
 */
public class ListAssignmentsToLevelsCommand extends CommandBase implements ICommand {
    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "list assignmentsToLevels [outFile=<name>];";
    }


    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("list assignmentsToLevels");
        final String fileName;
        if (np.peekMatchIgnoreCase("outFile")) {
            np.matchIgnoreCase("outFile=");
            fileName = np.getWordFileNamePunctuation();
        } else
            fileName = null;
        np.matchIgnoreCase(";");


        // use -3 for leaves, -2 and -1 for no hits and unassigned
        final SortedMap<Integer, Float> level2count = new TreeMap<>();
        level2count.put(-3, 0f);
        level2count.put(-2, 0f);
        level2count.put(-1, 0f);

        final SortedMap<String, Float> rank2count = new TreeMap<>();

        final PhyloTree tree = getDir().getMainViewer().getTree();
        listAssignmentsRec(tree, tree.getRoot(), 0, level2count, rank2count);

        final Writer w = new BufferedWriter(fileName == null ? new OutputStreamWriter(System.out) : new FileWriter(fileName));
        int count = 0;
        try {
            w.write("########## Begin of level-to-assignment listing for file: " + getDir().getDocument().getMeganFile().getName() + "\n");
            w.write("To leaves:  " + level2count.get(-3) + "\n");
            w.write("Unassigned: " + level2count.get(-2) + "\n");
            w.write("No hits:    " + level2count.get(-1) + "\n");
            w.write("Assignments to levels (distance from root):\n");
            count += 5;

            for (int level : level2count.keySet()) {
                if (level >= 0) {
                    w.write(level + "\t" + level2count.get(level) + "\n");
                    count++;
                }
            }

            w.write("Assignments to taxonomic ranks (where known):\n");
            count++;
            for (String rank : TaxonomicLevels.getAllNames()) {
                if (rank2count.get(rank) != null) {
                    w.write(rank + "\t" + rank2count.get(rank) + "\n");
                    count++;
                }
            }
            w.write("########## End of level-to-assignment listing\n");
            count++;

        } finally {
            if (fileName != null)
                w.close();
            else
                w.flush();
        }
        if (fileName != null && count > 0)
            NotificationsInSwing.showInformation(getViewer().getFrame(), "Lines written to file: " + count);
    }

    /**
     * recursively collects the numbers
     *
     * @param tree
     * @param v
     * @param level
     * @param level2count
     */
    private void listAssignmentsRec(PhyloTree tree, Node v, int level, SortedMap<Integer, Float> level2count, Map<String, Float> rank2count) {
        int taxonId = (Integer) (tree.getInfo(v));
        if (taxonId == IdMapper.UNASSIGNED_ID || taxonId == IdMapper.NOHITS_ID || taxonId == IdMapper.LOW_COMPLEXITY_ID) {
            level2count.put(taxonId, ((NodeData) v.getData()).getCountAssigned());
        } else // a true node in the taxonomy
        {
            final Float count = level2count.get(level);
            level2count.merge(level, ((NodeData) v.getData()).getCountAssigned(), Float::sum);

            final int taxLevel = TaxonomyData.getTaxonomicRank(taxonId);
            if (taxLevel != 0) {
                String rank = TaxonomicLevels.getName(taxLevel);
                if (rank != null) {
                    rank2count.merge(rank, ((NodeData) v.getData()).getCountAssigned(), Float::sum);
                }
            }

            if (v.getOutDegree() == 0) // is leaf
            {
                level = -3;
                if (count == null)
                    level2count.put(level, ((NodeData) v.getData()).getCountAssigned());
                else
                    level2count.put(level, count + ((NodeData) v.getData()).getCountAssigned());
            } else {
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    listAssignmentsRec(tree, e.getTarget(), level + 1, level2count, rank2count);
                }
            }
        }
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "List Assignments to Levels";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "List the number of reads assigned to each level of the taxonomy";
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
        return getViewer() instanceof MainViewer && getDir().getDocument().getNumberOfSamples() > 0;
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("show window=message;");
        execute("list assignmentsToLevels;");
    }
}
