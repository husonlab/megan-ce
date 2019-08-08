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
package megan.dialogs.extractor.commands;

import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.extractor.ExtractReadsViewer;
import megan.main.MeganProperties;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class ApplyCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return null;
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        Director dir = (Director) getDir();
        MainViewer mainViewer = dir.getMainViewer();
        ExtractReadsViewer viewer = (ExtractReadsViewer) getViewer();
        JTextField outDirectory = viewer.getOutDirectory();
        JTextField outFileTemplate = viewer.getOutFileTemplate();

        StringBuilder buf = new StringBuilder();

        buf.append("extract what=reads");
        buf.append(" outdir='");
        buf.append(outDirectory.getText()).append("'");
        buf.append(" outfile='");
        buf.append(outFileTemplate.getText()).append("'");
        buf.append(" data=").append(viewer.getMode());
        buf.append(" ids=");
        if (viewer.getMode().equals(ClassificationType.Taxonomy.toString())) {
            PhyloTree tree = mainViewer.getTree();
            if (mainViewer.getSelectedNodes() == null || mainViewer.getSelectedNodes().size() == 0) {
                NotificationsInSwing.showWarning(viewer.getFrame(), "Nothing to save, no nodes selected");
                return;
            }
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                if (mainViewer.getSelected(v)) {
                    Integer taxId = (Integer) v.getInfo();
                    buf.append(" ").append(taxId);
                }
            }
        } else if (ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy().contains(viewer.getMode())) {
            boolean ok = false;
            ClassificationViewer classificationViewer = (ClassificationViewer) dir.getViewerByClassName(viewer.getMode());
            if (classificationViewer != null) {
                Collection<Integer> selected = classificationViewer.getSelectedIds();
                if (selected != null && selected.size() > 0) {
                    buf.append(Basic.toString(selected, " "));
                    ok = true;
                }
            }
            if (!ok) {
                NotificationsInSwing.showWarning(viewer.getFrame(), "Must first select nodes in the " + viewer.getMode() + " viewer");
                return;
            }
        }
        buf.append(" allBelow=").append(viewer.isIncludeSummarized()).append(";");

        ProgramProperties.put(MeganProperties.EXTRACT_OUTFILE_DIR, outDirectory.getText());
        ProgramProperties.put(MeganProperties.EXTRACT_OUTFILE_TEMPLATE, outFileTemplate.getText());

        execute(buf.toString());
    }

    public static final String NAME = "Extract";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Extract the reads";
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
        ExtractReadsViewer extractReadsViewer = (ExtractReadsViewer) getParent();
        Document doc = ((Director) getDir()).getDocument();

        return extractReadsViewer != null && doc.getNumberOfReads() > 0;
    }
}
