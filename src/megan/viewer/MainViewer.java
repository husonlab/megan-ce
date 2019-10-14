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
package megan.viewer;

import jloda.graph.Node;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.*;
import jloda.swing.util.ListTransferHandler;
import jloda.swing.util.PopupMenu;
import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.chart.ChartColorManager;
import megan.chart.gui.LabelsJList;
import megan.chart.gui.SyncListener;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CloseCommand;
import megan.commands.SaveCommand;
import megan.core.DataTable;
import megan.core.Director;
import megan.core.SelectionSet;
import megan.dialogs.compare.Comparer;
import megan.main.MeganProperties;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

/**
 * the main viewer
 * Daniel Huson, 11.05
 */
public class MainViewer extends ClassificationViewer implements IDirectableViewer, IViewerWithFindToolBar, IViewerWithLegend, IMainViewer {
    private static MainViewer lastActiveViewer = null;
    private static JFrame lastActiveFrame = null;

    private final LabelsJList seriesList;
    private final SelectionSet selectedSeries;

    /**
     * constructor
     *
     * @param dir
     * @param visible
     * @throws Exception
     */
    public MainViewer(final Director dir, boolean visible) throws Exception {
        super(dir, ClassificationManager.get(Classification.Taxonomy, false), visible);

        getMainSplitPane().getLeftComponent().setMinimumSize(new Dimension());
        getMainSplitPane().setDividerLocation(0.0d);

        selectedSeries = doc.getSampleSelection();

        if (!dir.isInternalDocument()) {
            MeganProperties.notifyListChange(MeganProperties.RECENTFILES);
        }
        ProgramProperties.checkState();

        SyncListener syncListener1 = enabledNames -> { // rescan enable state
            if (doc.getDataTable().setEnabledSamples(enabledNames)) {
                dir.execute("update reInduce=true;", commandManager);
            }
        };

        seriesList = new LabelsJList(this, syncListener1, new PopupMenu(this, megan.viewer.GUIConfiguration.getSeriesListPopupConfiguration(), commandManager));

        seriesList.addListSelectionListener(listSelectionEvent -> {
            if (!seriesList.inSelection) {
                seriesList.inSelection = true;
                try {
                    // select series in window
                    Set<String> selected = new HashSet<>();
                    selected.addAll(seriesList.getSelectedLabels());
                    selectedSeries.clear();
                    selectedSeries.setSelected(selected, true);
                } finally {
                    seriesList.inSelection = false;
                }
            }
        });
        seriesList.setDragEnabled(true);
        seriesList.setTransferHandler(new ListTransferHandler());

        selectedSeries.addSampleSelectionListener((labels, selected) -> {
            if (!seriesList.inSelection) {
                seriesList.inSelection = true;
                try {
                    DefaultListModel model = (DefaultListModel) seriesList.getModel();
                    for (int i = 0; i < model.getSize(); i++) {
                        String name = seriesList.getModel().getElementAt(i);
                        if (selectedSeries.isSelected(name))
                            seriesList.addSelectionInterval(i, i + 1);
                        else
                            seriesList.removeSelectionInterval(i, i + 1);
                    }
                } finally {
                    seriesList.inSelection = false;
                }
            }
        });

        getFrame().addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (dir.getDocument().getProgressListener() != null)
                    dir.getDocument().getProgressListener().setUserCancelled(true);
                commandManager.getCommand(CloseCommand.NAME).actionPerformed(null);
            }
        });
        getFrame().setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        collapseToDefault();
    }

    /**
     * update view
     *
     * @param what what should be updated? Possible values: Director.ALL or Director.TITLE
     */
    public void updateView(String what) {
        if (getClassification().getName().equals("null")) // is just temporary tree
        {
            classification = ClassificationManager.get(Classification.Taxonomy, true);
            getViewerJTree().update();
        }

        super.updateView(what);
        updateStatusBar();
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        JFrame saveLastActiveFrame = lastActiveFrame;
        lastActiveFrame = this.getFrame();
        try {
            askToSaveCurrent();
        } catch (CanceledException ex) {
            ProjectManager.setQuitting(false);
            throw ex;
        }
        try {
            if (ProjectManager.isQuitting() && ProjectManager.getNumberOfProjects() == 1) {
                if (!confirmQuit()) {
                    ProjectManager.setQuitting(false);
                }
            }
        } catch (CanceledException ex) {
            ProjectManager.setQuitting(false);
            throw ex;
        }

        doc.closeConnector();

        if (lastActiveViewer == MainViewer.this)
            lastActiveViewer = null;
        lastActiveFrame = saveLastActiveFrame;
        if (lastActiveFrame == getFrame())
            lastActiveFrame = null;

        super.destroyView();
    }

    /**
     * set the status line for given document
     */
    public void updateStatusBar() {
        int ntax = Math.max(0, getPhyloTree().getNumberOfNodes());

        getStatusBar().setText1("Taxa=" + ntax);

        StringBuilder buf2 = new StringBuilder();

        final DataTable dataTable = doc.getDataTable();
        if (dataTable.getNumberOfSamples() == 0 || dataTable.getTotalReads() == 0) {
            if (ProgramProperties.get(MeganProperties.TAXONOMYFILE, MeganProperties.DEFAULT_TAXONOMYFILE).equals(MeganProperties.DEFAULT_TAXONOMYFILE))
                buf2.append("Taxonomy");
            else
                buf2.append("Taxonomy=").append(Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(ProgramProperties.get(MeganProperties.TAXONOMYFILE, MeganProperties.DEFAULT_TAXONOMYFILE)), ""));
            if (TaxonomyData.isAvailable()) {
                if (TaxonomyData.getTree().getNumberOfNodes() > 0)
                    buf2.append(String.format(" size=%,d,", TaxonomyData.getTree().getNumberOfNodes()));
                final Set<Integer> disabledTaxa = TaxonomyData.getDisabledTaxa();
                if (disabledTaxa != null && disabledTaxa.size() > 0)
                    buf2.append(String.format(" disabledTaxa=%,d", disabledTaxa.size()));
            }
        } else {
            if (dataTable.getNumberOfSamples() == 1) {
                buf2.append(String.format("Reads=%,d Assigned=%,d", doc.getNumberOfReads(), getTotalAssignedReads()));
                buf2.append(String.format(" (%s)", doc.getReadAssignmentMode().toString()));
                buf2.append(" MinScore=").append(doc.getMinScore());
                if (doc.getMaxExpected() != 10000)
                    buf2.append(" MaxExpected=").append(doc.getMaxExpected());
                if (doc.getMinPercentIdentity() > 0)
                    buf2.append(" MinPercentIdentity=").append(doc.getMinPercentIdentity());
                buf2.append(" TopPercent=").append(doc.getTopPercent());
                if (doc.getMinSupportPercent() > 0)
                    buf2.append(" MinSupportPercent=").append(doc.getMinSupportPercent());
                if (doc.getMinSupportPercent() == 0 || doc.getMinSupport() > 1)
                    buf2.append(" MinSupport=").append(doc.getMinSupport());
                final Set<Integer> disabledTaxa = TaxonomyData.getDisabledTaxa();
                if (disabledTaxa != null && disabledTaxa.size() > 0)
                    buf2.append(String.format(" disabledTaxa=%,d", disabledTaxa.size()));
                if (doc.isUseIdentityFilter())
                    buf2.append(" UseIdentityFilter=true");
                buf2.append(" LCA=").append(doc.getLcaAlgorithm().toString());
                if (doc.getLcaCoveragePercent() < 100f)
                    buf2.append(String.format(" lcaCoveragePercent=%d", Math.round(doc.getLcaCoveragePercent())));
                if (doc.getMinPercentReadToCover() > 0)
                    buf2.append(String.format(" MinPercentReadToCover=%d", Math.round(doc.getMinPercentReadToCover())));
                if (doc.getMinPercentReferenceToCover() > 0)
                    buf2.append(String.format(" MinPercentReferenceToCover=%d", Math.round(doc.getMinPercentReferenceToCover())));
            } else {
                buf2.append(String.format("Samples=%d,", doc.getNumberOfSamples()));
                Comparer.COMPARISON_MODE mode = Comparer.parseMode(dataTable.getParameters());
                int normalized_to = Comparer.parseNormalizedTo(dataTable.getParameters());
                if (mode.equals(Comparer.COMPARISON_MODE.RELATIVE)) {
                    buf2.append(String.format(" Relative Comparison, Assigned=%,d (normalized to %,d per sample)", getTotalAssignedReads(), normalized_to));
                } else
                    buf2.append(String.format(" Absolute Comparison, Reads=%,d, Assigned=%,d", doc.getNumberOfReads(), getTotalAssignedReads()));
                buf2.append(String.format(" (%s)", doc.getReadAssignmentMode().toString()));
            }
            if (doc.getBlastMode() != BlastMode.Unknown)
                buf2.append(" mode=").append(doc.getBlastMode().toString());
        }

        getStatusBar().setText2(buf2.toString());
    }


    /**
     * determine whether current data needs saving and allows the user to do so, if necessary
     */
    private void askToSaveCurrent() throws CanceledException {
        if (ProgramProperties.isUseGUI()) {
            if (doc.getMeganFile().isMeganSummaryFile() && doc.getNumberOfSamples() > 0 && doc.isDirty()) {
                getFrame().toFront();
                getFrame().setAlwaysOnTop(true);
                try {
                    int result = JOptionPane.showConfirmDialog(getFrame(), "Document has been modified, save before " +
                                    (ProjectManager.isQuitting() ? "quitting?" : "closing?"), ProgramProperties.getProgramName() + " - Save Changes?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon());

                    if (result == JOptionPane.YES_OPTION) {
                        Boolean[] canceled = new Boolean[]{false};
                        getCommandManager().getCommand(SaveCommand.NAME).actionPerformed(new ActionEvent(canceled, 0, "askToSave"));
                        if (canceled[0])
                            throw new CanceledException();
                        doc.setDirty(false);
                    } else if (result == JOptionPane.NO_OPTION)
                        doc.setDirty(false);
                    else if (result == JOptionPane.CANCEL_OPTION) {
                        throw new CanceledException();
                    }
                } finally {
                    getFrame().setAlwaysOnTop(false);
                }
            }
        }
    }

    /**
     * ask whether user wants to quit
     */
    private boolean confirmQuit() throws CanceledException {
        if (ProgramProperties.isUseGUI()) {
            getFrame().toFront();
            int result = JOptionPane.showConfirmDialog(getLastActiveFrame(), "Quit " + ProgramProperties.getProgramName() + "?",
                    ProgramProperties.getProgramVersion() + " - Quit?", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon());
            if (result == JOptionPane.CANCEL_OPTION) {
                throw new CanceledException();
            } else return result != JOptionPane.NO_OPTION;
        } else
            return true;
    }

    /**
     * get the quit action
     *
     * @return quit action
     */
    public AbstractAction getQuit() {
        return CommandManager.createAction(getCommandManager().getCommand("Quit"));
    }

    public static JFrame getLastActiveFrame() {
        return lastActiveFrame;
    }

    public static void setLastActiveFrame(JFrame lastActiveFrame) {
        MainViewer.lastActiveFrame = lastActiveFrame;
    }

    /**
     * gets the series color getter
     *
     * @return series color getter
     */
    public ChartColorManager.ColorGetter getSeriesColorGetter() {
        return new ChartColorManager.ColorGetter() {
            private final ChartColorManager.ColorGetter colorGetter = doc.getChartColorManager().getSeriesColorGetter();

            public Color get(String label) {
                {
                    if (getNodeDrawer().getStyle() == NodeDrawer.Style.Circle || getNodeDrawer().getStyle() == NodeDrawer.Style.HeatMap)
                        return Color.WHITE;
                    else
                        return colorGetter.get(label);
                }
            }
        };
    }

    public LabelsJList getSeriesList() {
        return seriesList;
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "Taxonomy";
    }

    /**
     * collapse to default small tree
     */
    public void collapseToDefault() {
        getCollapsedIds().clear();
        getCollapsedIds().add(2759); // eukaryota
        getCollapsedIds().add(2157);  // archaea
        getCollapsedIds().add(2);     // bacteria
        getCollapsedIds().add(28384);        // other sequences
        getCollapsedIds().add(12908);          // unclassified sequences
        getCollapsedIds().add(12884);      // viroids
        getCollapsedIds().add(10239);   // viruses
    }

    public void setDoReInduce(boolean b) {
        // System.err.println("setDoReInduce: not implemented");
    }

    public void setDoReset(boolean b) {
        //System.err.println("setDoReset: not implemented");
    }


    public Node getTaxId2Node(int taxId) {
        return getANode(taxId);
    }

    /**
     * does this viewer currently have any URLs for selected nodes?
     *
     * @return true, if has URLs for selected nodes
     */
    public boolean hasURLsForSelection() {
        return getSelectedNodes().size() > 0;
    }

    /**
     * gets list of URLs associated with selected nodes
     *
     * @return URLs
     */
    public java.util.List<String> getURLsForSelection() {
        final ArrayList<String> urls = new ArrayList<>(getSelectedNodes().size());
        for (Node v : getSelectedNodes()) {
            Integer taxId = (Integer) v.getInfo();
            if (taxId != null && taxId > 0) {
                try {
                    urls.add("https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?mode=Info&id=" + taxId);
                } catch (Exception e1) {
                    Basic.caught(e1);
                }
            }
        }
        return urls;
    }
}