/*
 * ReanalyzeDialog.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.dialogs.reanalyze;

import jloda.swing.util.ProgramProperties;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.StringUtils;
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
 * Reanalyze dialog
 * Daniel Huson, 12.2019
 */
public class ReanalyzeDialog extends ImportBlastDialog {

    /**
     * constructor
     *
	 */
    public ReanalyzeDialog(Component parent, Director dir) {
        super(parent, dir, "Reanlyze DAA and RMA Files - MEGAN");
    }

    @Override
    public void addFilesTab(JTabbedPane tabbedPane) {
        getCommandManager().addCommands(this, new String[]{"megan.dialogs.reanalyze.commands"});
        getAlignmentModeCBox().setEnabled(false);
        getFormatCBox().setEnabled(false);
        tabbedPane.addTab("Files", new FilesPanel(this));
    }

    @Override
    protected void addViewTab(String title, JPanel jpanel) {
        // don't add view tabs
    }

    /**
     * apply meganize DAA file
     */
    public void apply() {
        ClassificationManager.get(Classification.Taxonomy, true).getIdMapper().setUseTextParsing(isParseTaxonNames());
        final String files = getBlastFileName();
        if (files.length() > 0) {
			ProgramProperties.put(MeganProperties.BLASTFILE, new File(StringUtils.getLinesFromString(files).get(0)));

            final StringBuilder buf = new StringBuilder();

            buf.append("reanalyzeFiles file=");

			java.util.List<String> fileNames = StringUtils.getLinesFromString(files);

            boolean first = true;
            for (String name : fileNames) {
                File file = new File(name);
                if (!(file.exists() && file.canRead())) {
                    NotificationsInSwing.showError(this, "Failed to open file for reading: " + name);
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
            if (getMinSupportPercent() > 0)
                buf.append(" minSupportPercent=").append(getMinSupportPercent());
            else
                buf.append(" minSupport=").append(getMinSupport());
            buf.append(" lcaAlgorithm=").append(getLcaAlgorithm().toString());
            if (getLcaAlgorithm().equals(Document.LCAAlgorithm.weighted) || getLcaAlgorithm().equals(Document.LCAAlgorithm.longReads))
                buf.append(" lcaCoveragePercent=").append(getLCACoveragePercent());
            if (getMinPercentReadToCover() > 0)
                buf.append(" minPercentReadToCover=").append(getMinPercentReadToCover());
            if (getMinPercentReferenceToCover() > 0)
                buf.append(" minPercentReferenceToCover=").append(getMinPercentReferenceToCover());
            buf.append(" minComplexity=").append(getMinReadLength());
            buf.append(" longReads=").append(isLongReads());
            buf.append(" pairedReads=").append(isUsePairedReads());
            buf.append(" useIdentityFilter=").append(isUsePercentIdentityFilter());
            if (isUseContaminantsFilter())
                buf.append(" useContaminantFilter=").append(isUseContaminantsFilter());
            if (getContaminantsFileName() != null && getContaminantsFileName().length() > 0)
                buf.append(" loadContaminantFile=").append(getContaminantsFileName());
			buf.append(" readAssignmentMode=").append(getReadAssignmentMode());
			buf.append(" fNames=").append(StringUtils.toString(getSelectedFNames(), " "));
			buf.append(";");

            setResult(buf.toString());
            destroyView();
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
		return getBlastFileName().trim().length() > 0 && (new File(StringUtils.getFirstLine(getBlastFileName()).trim())).exists();
    }
}

