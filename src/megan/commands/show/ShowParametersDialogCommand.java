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
import jloda.swing.director.IDirectableViewer;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.ContaminantManager;
import megan.core.Document;
import megan.dialogs.lrinspector.LRInspectorViewer;
import megan.dialogs.parameters.ParametersDialog;
import megan.inspector.InspectorWindow;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class ShowParametersDialogCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "recompute [minSupportPercent=<number>] [minSupport=<number>] [minScore=<number>] [maxExpected=<number>] [minPercentIdentity=<number>] [topPercent=<number>]\n" +
                "\t[weightedLCA={false|true}] [lcaCoveragePercent=<number>] [percentReadToCover=<number>] [minComplexity=<number>] [pairedReads={false|true}] [useIdentityFilter={false|true}]\n" +
                "\t[useContaminantFilter={false|true}] [loadContaminantFile=<filename>]\n" +
                "\t[readAssignmentMode={" + Basic.toString(Document.ReadAssignmentMode.values(), "|") + "} [fNames={" + Basic.toString(ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy(), "|") + "];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("recompute");

        if (np.peekMatchIgnoreCase("minSupportPercent")) {
            np.matchIgnoreCase("minSupportPercent=");
            getDoc().setMinSupportPercent((float) np.getDouble(0, 100));
        }
        if (np.peekMatchIgnoreCase("minSupport")) {
            np.matchIgnoreCase("minSupport=");
            getDoc().setMinSupport(np.getInt(1, Integer.MAX_VALUE));
        }
        if (np.peekMatchIgnoreCase("minScore")) {
            np.matchIgnoreCase("minScore=");
            getDoc().setMinScore((float) np.getDouble(0, Float.MAX_VALUE));
        }
        if (np.peekMatchIgnoreCase("maxExpected")) {
            np.matchIgnoreCase("maxExpected=");
            getDoc().setMaxExpected((float) np.getDouble(0, Float.MAX_VALUE));
        }
        if (np.peekMatchIgnoreCase("minPercentIdentity")) {
            np.matchIgnoreCase("minPercentIdentity=");
            getDoc().setMinPercentIdentity((float) np.getDouble(0, 100));
        }
        if (np.peekMatchIgnoreCase("topPercent")) {
            np.matchIgnoreCase("topPercent=");
            getDoc().setTopPercent((float) np.getDouble(0, Float.MAX_VALUE));
        }
        if (np.peekMatchIgnoreCase("weightedLCA")) {
            np.matchIgnoreCase("weightedLCA=");
            getDoc().setLcaAlgorithm(Document.LCAAlgorithm.weighted);
        } else if (np.peekMatchIgnoreCase("lcaAlgorithm")) {
            np.matchIgnoreCase("lcaAlgorithm=");
            getDoc().setLcaAlgorithm(Document.LCAAlgorithm.valueOfIgnoreCase(np.getWordRespectCase()));
        }
        if (np.peekMatchAnyTokenIgnoreCase("lcaCoveragePercent weightedLCAPercent")) {
            np.matchAnyTokenIgnoreCase("lcaCoveragePercent weightedLCAPercent");
            np.matchIgnoreCase("=");
            getDoc().setLcaCoveragePercent((float) np.getDouble(1, 100));
        }
        if (np.peekMatchIgnoreCase("minPercentReadToCover")) {
            np.matchIgnoreCase("minPercentReadToCover=");
            getDoc().setMinPercentReadToCover((float) np.getDouble(0, 100));
        }
        if (np.peekMatchIgnoreCase("minPercentReferenceToCover")) {
            np.matchIgnoreCase("minPercentReferenceToCover=");
            getDoc().setMinPercentReferenceToCover((float) np.getDouble(0, 100));
        }
        if (np.peekMatchIgnoreCase("minComplexity")) {
            np.matchIgnoreCase("minComplexity=");
            getDoc().setMinComplexity((float) np.getDouble(-1.0, 1.0));
        }
        if (np.peekMatchIgnoreCase("longReads")) {
            np.matchIgnoreCase("longReads=");
            getDoc().setLongReads(np.getBoolean());
        }
        if (np.peekMatchIgnoreCase("pairedReads")) {
            np.matchIgnoreCase("pairedReads=");
            getDoc().setPairedReads(np.getBoolean());
        }
        if (np.peekMatchIgnoreCase("useIdentityFilter")) {
            np.matchIgnoreCase("useIdentityFilter=");
            getDoc().setUseIdentityFilter(np.getBoolean());
        }
        if (np.peekMatchIgnoreCase("useContaminantFilter")) {
            np.matchIgnoreCase("useContaminantFilter=");
            getDoc().setUseContaminantFilter(np.getBoolean());
        } else
            getDoc().setUseContaminantFilter(false);
        if (np.peekMatchIgnoreCase("loadContaminantFile")) {
            np.matchIgnoreCase("loadContaminantFile=");
            final String fileName = np.getWordFileNamePunctuation();
            final ContaminantManager contaminantManager = new ContaminantManager();
            System.err.println("Loading contaminant file: " + fileName);
            contaminantManager.read(fileName);
            System.err.println("Contaminants: " + contaminantManager.inputSize());
            getDoc().getDataTable().setContaminants(contaminantManager.getTaxonIdsString());
            getDoc().setUseContaminantFilter(true);
        }

        if (np.peekMatchIgnoreCase("readAssignmentMode")) {
            np.matchIgnoreCase("readAssignmentMode=");
            getDoc().setReadAssignmentMode(Document.ReadAssignmentMode.valueOfIgnoreCase(np.getWordMatchesIgnoringCase(Basic.toString(Document.ReadAssignmentMode.values(), " "))));
        }
        if (np.peekMatchIgnoreCase("fNames")) {
            getDoc().getActiveViewers().clear();
            getDoc().getActiveViewers().add(Classification.Taxonomy);
            np.matchIgnoreCase("fNames=");
            while (!np.peekMatchIgnoreCase(";"))
                getDoc().getActiveViewers().add(np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy(), " ")));
        }
        np.matchIgnoreCase(";");

        final InspectorWindow inspectorWindow = (InspectorWindow) getDir().getViewerByClass(InspectorWindow.class);
        if (inspectorWindow != null && inspectorWindow.getDataTree().getRowCount() > 1) {
            SwingUtilities.invokeLater(() -> inspectorWindow.clear());
        }

        final ArrayList<LRInspectorViewer> toClose = new ArrayList<>();
        for (IDirectableViewer viewer : getDir().getViewers()) {
            if (viewer instanceof LRInspectorViewer)
                toClose.add((LRInspectorViewer) viewer);
        }
        for (final IDirectableViewer viewer : toClose) {
            SwingUtilities.invokeLater(() -> {
                try {
                    viewer.destroyView();
                } catch (CanceledException e) {
                    Basic.caught(e);
                }
            });
        }

        getDoc().processReadHits();
        getDoc().setDirty(true);
        if (getViewer() instanceof MainViewer)
            ((MainViewer) getViewer()).setDoReInduce(true);
        NotificationsInSwing.showInformation(String.format("Classified %,d reads", +getDoc().getNumberOfReads()));
    }

    public void actionPerformed(ActionEvent event) {
        final ParametersDialog dialog = new ParametersDialog(getViewer().getFrame(), getDir());
        if (dialog.apply()) {
            execute("recompute " + dialog.getParameterString() + ";");
        }
        dialog.dispose();
    }

    public boolean isApplicable() {
        Document doc = getDoc();
        return !doc.getMeganFile().isReadOnly() && doc.getMeganFile().hasDataConnector() && !doc.getMeganFile().isMeganSummaryFile();
    }

    public String getName() {
        return "Change LCA Parameters...";
    }

    public String getDescription() {
        return "Rerun the LCA analysis with different parameters";
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
    }
}


