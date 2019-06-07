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

import jloda.swing.commands.ICommand;
import jloda.swing.util.Alert;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.data.IConnector;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class SetNumberOfReadsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set totalReads=<num>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        // todo: fix so that number of reads can also be set for megan summary file
        np.matchIgnoreCase("set totalReads=");
        int num = np.getInt(1, Integer.MAX_VALUE);
        np.matchRespectCase(";");

        if (getDoc().getMeganFile().isReadOnly())
            throw new IOException("Can't set number of reads, file is read-only");
        if (!(getDoc().getMeganFile().hasDataConnector() || getDoc().getMeganFile().isMeganSummaryFile() && getDoc().getNumberOfSamples() == 1))
            throw new IOException("Can't set number of reads, wrong kind of file");

        if (getDoc().getMeganFile().hasDataConnector() || getDoc().getMeganFile().isMeganSummaryFile() && getDoc().getNumberOfSamples() == 1) {
            if (num != getDoc().getNumberOfReads()) {
                if (num > getDoc().getNumberOfReads())
                    getDoc().setAdditionalReads(num - getDoc().getNumberOfReads());
                else
                    getDoc().setAdditionalReads(0);

                getDoc().processReadHits();
                getDir().getMainViewer().setDoReInduce(true);
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        String str = JOptionPane.showInputDialog(getViewer().getFrame(), "Total number of reads:", getDoc().getNumberOfReads());
        if (str != null) {
            long numberOfReads = getDoc().getNumberOfReads();
            try {
                numberOfReads = Integer.parseInt(str);
            } catch (Exception ex) {
                new Alert(getViewer().getFrame(), "Number expected, got: " + str);
            }
            if (numberOfReads != getDoc().getNumberOfReads()) {
                if (getDoc().getMeganFile().hasDataConnector()) {
                    int numberOfMatches = 0;
                    try {
                        IConnector connector = getDoc().getConnector();
                        numberOfMatches = connector.getNumberOfMatches();
                    } catch (IOException e) {
                        Basic.caught(e);
                    }

                    if (numberOfMatches > 10000000) {
                        int result = JOptionPane.showConfirmDialog(getViewer().getFrame(),
                                String.format("This sample contains %,d matches, processing may take a long time, proceed?", numberOfMatches),
                                "Very large dataset, proceed?", JOptionPane.YES_NO_OPTION);
                        if (result != JOptionPane.YES_OPTION)
                            return;
                    }
                }
                execute("set totalReads=" + numberOfReads + ";");
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof MainViewer && !getDoc().getMeganFile().isReadOnly() && (getDoc().getMeganFile().hasDataConnector() || getDoc().getMeganFile().isMeganSummaryFile() && getDoc().getNumberOfSamples() == 1);
    }

    public String getName() {
        return "Set Number Of Reads...";
    }

    public String getDescription() {
        return "Set the total number of reads in the analysis (will initiate recalculation of all classifications)";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }
}

