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

package megan.dialogs.meganize;

import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.importblast.ImportBlastDialog;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * runs the meganizer in MEGAN
 * Daniel Huson, 3.2016
 */
public class MeganizeDAADialog extends ImportBlastDialog {

    /**
     * constructor
     *
     * @param parent
     * @param dir
     */
    public MeganizeDAADialog(Component parent, Director dir) {
        super(parent, dir, "Meganize Diamond DAA files - MEGAN");
    }

    @Override
    public void addFilesTab(JTabbedPane tabbedPane) {
        getCommandManager().addCommands(this, new String[]{"megan.dialogs.meganize.commands"});
        getAlignmentModeCBox().setEnabled(false);
        getFormatCBox().setEnabled(false);
        tabbedPane.addTab("Files", new FilesPanel(this));
    }

    /**
     * apply meganize DAA file
     */
    public void apply() {
        ClassificationManager.get(Classification.Taxonomy, true).getIdMapper().setUseTextParsing(isParseTaxonNames());
        final String daaFileNames = getBlastFileName();
        if (daaFileNames.length() > 0) {
            ProgramProperties.put(MeganProperties.BLASTFILE, new File(Basic.getLinesFromString(daaFileNames).get(0)));

            final StringBuilder buf = new StringBuilder();

            buf.append("meganize daaFile=");

            java.util.List<String> fileNames = Basic.getLinesFromString(daaFileNames);

            boolean first = true;
            for (String name : fileNames) {
                File file = new File(name);
                if (!(file.exists() && file.canRead())) {
                    NotificationsInSwing.showError(this, "Failed to open DAA file for reading: " + name);
                    return;
                }
                if (first)
                    first = false;
                else
                    buf.append(", ");
                buf.append("'").append(name).append("'");
            }

            buf.append(" minScore=").append(getMinScore());
            buf.append(" maxExpected=").append(getMaxExpected());
            buf.append(" minPercentIdentity=").append(getMinPercentIdentity());
            buf.append(" topPercent=").append(getTopPercent());
            buf.append(" minSupportPercent=").append(getMinSupportPercent());
            buf.append(" minSupport=").append(getMinSupport());
            buf.append(" lcaAlgorithm=").append(getLcaAlgorithm().toString());
            if (getLcaAlgorithm().equals(Document.LCAAlgorithm.weighted) || getLcaAlgorithm().equals(Document.LCAAlgorithm.longReads))
                buf.append(" lcaCoveragePercent=").append(getLCACoveragePercent());
            if (getMinPercentReadToCover() > 0)
                buf.append(" minPercentReadToCover=").append(getMinPercentReadToCover());
            if (getMinPercentReferenceToCover() > 0)
                buf.append(" minPercentReferenceToCover=").append(getMinPercentReferenceToCover());

            buf.append(" minComplexity=").append(getMinComplexity());
            buf.append(" useIdentityFilter=").append(isUsePercentIdentityFilter());

            buf.append(" readAssignmentMode=").append(getReadAssignmentMode());

            buf.append(" fNames=").append(Basic.toString(getSelectedFNames(), " "));

            buf.append(" longReads=").append(isLongReads());

            buf.append(" paired=").append(isUsePairedReads());

            if (isUsePairedReads()) {
                String pattern1 = getPairedReadSuffix1();
                //String pattern2 = ProgramProperties.get(MeganProperties.PAIRED_READ_SUFFIX2, "");
                buf.append(" pairSuffixLength=").append(pattern1.length());
            }

            if (getShortDescription().length() > 0)
                buf.append(" description='").append(getShortDescription()).append("'");

            buf.append(";");

            setResult(buf.toString());
            try {
                destroyView();
            } catch (CanceledException e) {
                Basic.caught(e);
            }
        }
    }

    @Override
    public void updateView(String what) {
        getAlignmentModeCBox().setEnabled(false);
        getFormatCBox().setEnabled(false);
        super.updateView(what);
        getAlignmentModeCBox().setEnabled(false);
        getFormatCBox().setEnabled(false);
    }

    /**
     * is meganization applicable?
     *
     * @return true, if applicable
     */
    public boolean isAppliable() {
        return getBlastFileName().trim().length() > 0 && (new File(Basic.getFirstLine(getBlastFileName()).trim())).exists();
    }

}

