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
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.ClassificationType;
import megan.core.Document;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * shows the histogram of matches for a given node
 * Daniel Huson, 11.2010
 */
public class ShowMatchesHistogramCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show histogram taxonId=");
        int taxId = np.getInt();
        np.matchIgnoreCase(";");

        Document doc = getDir().getDocument();
        int[] values = computeHistogram(taxId, doc);
        System.err.println("Histogram for taxonId=" + taxId + ":");
        for (int value : values) {
            System.err.println(value);
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "show histogram taxonId=<num>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        int taxId = ((MainViewer) getViewer()).getSelectedIds().iterator().next();
        String command = "show histogram taxonId=" + taxId + ";";
        execute(command);
    }

    /**
     * compute the histogram associated with a given class
     *
     * @param classId
     * @param doc
     * @return histogram of counts of matches to different sequences
     */
    private int[] computeHistogram(int classId, Document doc) throws IOException {
        IConnector connector = doc.getConnector();

        Map<String, Integer> matched2count = new HashMap<>();

        try (IReadBlockIterator it = connector.getReadsIterator(ClassificationType.Taxonomy.toString(), classId, doc.getMinScore(), doc.getMaxExpected(), false, true)) {
            while (it.hasNext()) {
                final IReadBlock readBlock = it.next();
                for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                    IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                    if (matchBlock.getBitScore() >= doc.getMinScore() && matchBlock.getExpected() <= doc.getMaxExpected() &&
                            (matchBlock.getPercentIdentity() == 0 || matchBlock.getPercentIdentity() >= doc.getMinPercentIdentity())) {
                        String firstLine = matchBlock.getText().split("\n")[0];

                        matched2count.merge(firstLine, 1, Integer::sum);
                    }
                }
            }
        }
        int[] values = new int[matched2count.size()];
        int i = 0;
        for (Integer count : matched2count.values()) {
            values[i++] = count;
        }
        Arrays.sort(values);
        return values;
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Show Matches Histogram";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Shows the distribution of matches for a given taxon";
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
        return ((MainViewer) getViewer()).getSelectedIds().size() == 1;
    }
}
