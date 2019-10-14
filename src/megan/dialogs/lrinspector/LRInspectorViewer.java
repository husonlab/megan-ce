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

package megan.dialogs.lrinspector;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import jloda.fx.util.IHasJavaFXStageAndRoot;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IUsesHeatMapColors;
import jloda.swing.director.IViewerWithFindToolBar;
import jloda.swing.director.ProjectManager;
import jloda.swing.find.CompositeObjectSearcher;
import jloda.swing.find.FindToolBar;
import jloda.swing.find.SearchManager;
import jloda.swing.util.IViewerWithJComponent;
import jloda.swing.util.ToolBar;
import jloda.swing.window.MenuBar;
import jloda.util.*;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.data.IClassificationBlock;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.dialogs.input.InputDialog;
import megan.fx.SwingPanel4FX;
import megan.main.MeganProperties;
import megan.samplesviewer.commands.PasteCommand;
import megan.util.IReadsProvider;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * visual read inspector viewer
 * Created by huson on 2/21/17.
 */
public class LRInspectorViewer extends JFrame implements IDirectableViewer, Printable, IViewerWithJComponent, IViewerWithFindToolBar, IUsesHeatMapColors, IReadsProvider, IHasJavaFXStageAndRoot {
    private final Director dir;
    private boolean uptoDate = true;
    private boolean locked = false;

    private final String classificationName;
    private final int classId;
    private String classIdName;
    private final Set<Integer> classificationIds = new TreeSet<>();
    private final IntegerProperty maxReadLength = new SimpleIntegerProperty();

    private final MenuBar menuBar;
    private final CommandManager commandManager;
    private final JPanel mainPanel;
    private final ToolBar toolBar;

    private final SwingPanel4FX<LRInspectorController> swingPanel4FX;

    private boolean showFindToolBar = false;
    private final SearchManager searchManager;
    private final CompositeObjectSearcher searcher;

    private final JFrame frame;

    private Runnable runOnDestroy;

    /**
     * constructor
     *
     * @param parent
     * @param viewer
     * @param classId
     */
    public LRInspectorViewer(JFrame parent, ClassificationViewer viewer, int classId) {
        this.dir = viewer.getDir();
        this.classificationName = viewer.getClassName();
        this.classId = classId;
        this.classIdName = ClassificationManager.get(classificationName, true).getName2IdMap().get(classId);
        if (classIdName != null)
            classIdName = Basic.abbreviateDotDotDot(Basic.toCleanName(classIdName), 60);
        else
            classIdName = "" + classId;

        this.frame = this;
        {
            if (viewer.getANode(classId).getOutDegree() == 0) // is a leaf
            {
                getClassificationIds().addAll(ClassificationManager.get(classificationName, true).getFullTree().getAllDescendants(classId));
            } else
                getClassificationIds().add(classId);
        }
        commandManager = new CommandManager(dir, this, new String[]{"megan.commands", "megan.dialogs.lrinspector.commands"}, !ProgramProperties.isUseGUI());

        setTitle();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImages(ProgramProperties.getProgramIconImages());

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        setJMenuBar(menuBar);

        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(mainPanel);
        toolBar = new ToolBar(this, GUIConfiguration.getToolBarConfiguration(), commandManager);
        frame.add(toolBar, BorderLayout.NORTH);

        searcher = new CompositeObjectSearcher("Alignments", frame);
        searchManager = new SearchManager(dir, this, searcher, false, true);
        searchManager.getFindDialogAsToolBar().setEnabled(false);

        commandManager.updateEnableState();

        getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    destroyView();
                } catch (CanceledException e1) {
                    Basic.caught(e1);
                }
            }

            public void windowActivated(WindowEvent event) {
                MainViewer.setLastActiveFrame(frame);
                commandManager.updateEnableState(PasteCommand.ALT_NAME);
                final InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null) {
                    inputDialog.setViewer(dir, LRInspectorViewer.this);
                }

            }
        });

        frame.setLocationRelativeTo(parent);
        setSize(900, 800);
        frame.setVisible(true);

        commandManager.updateEnableState();

        swingPanel4FX = new SwingPanel4FX<>(this.getClass());

        swingPanel4FX.runLaterInSwing(() -> {
            mainPanel.add(swingPanel4FX.getPanel(), BorderLayout.CENTER); // add panel once initialization complete
            mainPanel.validate();
            Platform.runLater(() -> {
                try {
                    swingPanel4FX.getController().setupControls(LRInspectorViewer.this, toolBar);
                    swingPanel4FX.getController().updateScene(LRInspectorViewer.this);
                    SwingUtilities.invokeLater(() -> getCommandManager().updateEnableState());
                    // uptodate is set by controller
                } catch (IOException e) {
                    Basic.caught(e);
                }
            });
        });
    }

    public boolean isUptoDate() {
        return uptoDate;
    }

    public JFrame getFrame() {
        return frame;
    }

    public void updateView(String what) {
        uptoDate = false;
        setTitle();
        if (what.equals(Director.ALL)) {
            Platform.runLater(() -> {
                if (swingPanel4FX.getPanel() != null)
                    swingPanel4FX.getController().recolor();
            });
        }
        FindToolBar findToolBar = searchManager.getFindDialogAsToolBar();
        if (findToolBar.isClosing()) {
            showFindToolBar = false;
            findToolBar.setClosing(false);
        }
        if (!findToolBar.isEnabled() && showFindToolBar) {
            mainPanel.add(findToolBar, BorderLayout.NORTH);
            findToolBar.setEnabled(true);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        } else if (findToolBar.isEnabled() && !showFindToolBar) {
            mainPanel.remove(findToolBar);
            findToolBar.setEnabled(false);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        }
        if (findToolBar.isEnabled())
            findToolBar.clearMessage();
        uptoDate = true;
    }

    public void lockUserInput() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        searchManager.getFindDialogAsToolBar().setEnableCritical(false);
        locked = true;
        commandManager.setEnableCritical(false);
    }

    public void unlockUserInput() {
        setCursor(Cursor.getDefaultCursor());
        searchManager.getFindDialogAsToolBar().setEnableCritical(true);
        commandManager.setEnableCritical(true);
        locked = false;
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return locked;
    }

    public void destroyView() throws CanceledException {
        Platform.runLater(() -> {
            if (swingPanel4FX != null)
                swingPanel4FX.getController().getService().cancel();
        });
        if (runOnDestroy != null)
            runOnDestroy.run();
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        dir.removeViewer(this);
        searchManager.getFindDialogAsToolBar().close();
        dispose();
    }

    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * set the title of the window
     */
    private void setTitle() {
        String newTitle = "LR Inspector [" + getClassIdDisplayName() + "] - " + dir.getDocument().getTitle();

        /*
        if (dir.getDocument().isDirty())
            newTitle += "*";
          */

        if (dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + dir.getID() + "] - " + ProgramProperties.getProgramVersion();

        if (!frame.getTitle().equals(newTitle)) {
            frame.setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "LRInspector";
    }

    /**
     * the taxon ids associated with this viewer
     *
     * @return taxon ids
     */
    public Set<Integer> getClassificationIds() {
        return classificationIds;
    }

    public String getClassificationName() {
        return classificationName;
    }

    public IntegerProperty maxReadLengthProperty() {
        return maxReadLength;
    }

    public void setRunOnDestroy(Runnable runnable) {
        this.runOnDestroy = runnable;
    }

    public Director getDir() {
        return dir;
    }

    /**
     * print the window
     *
     * @param gc0
     * @param format
     * @param pagenumber
     * @return
     * @throws PrinterException
     */
    public int print(Graphics gc0, PageFormat format, int pagenumber) throws PrinterException {
        if (pagenumber == 0) {
            Graphics2D gc = ((Graphics2D) gc0);
            gc.setFont(getFont());

            Dimension dim = swingPanel4FX.getPanel().getSize();

            int image_w = dim.width;
            int image_h = dim.height;

            double paper_x = format.getImageableX() + 1;
            double paper_y = format.getImageableY() + 1;
            double paper_w = format.getImageableWidth() - 2;
            double paper_h = format.getImageableHeight() - 2;

            double scale_x = paper_w / image_w;
            double scale_y = paper_h / image_h;
            double scale = Math.min(scale_x, scale_y);

            double shift_x = paper_x + (paper_w - scale * image_w) / 2.0;
            double shift_y = paper_y + (paper_h - scale * image_h) / 2.0;

            gc.translate(shift_x, shift_y);
            gc.scale(scale, scale);

            gc.setStroke(new BasicStroke(1.0f));
            gc.setColor(Color.BLACK);

            swingPanel4FX.getPanel().paint(gc);

            return Printable.PAGE_EXISTS;
        } else
            return Printable.NO_SUCH_PAGE;
    }

    public JComponent getComponent() {
        return swingPanel4FX.getPanel();
    }

    @Override
    public boolean useHeatMapColors() {
        return swingPanel4FX.getController().isUsingHeatmap();
    }

    /**
     * find the named block. Need to do this because read blocks get re-used during parsing
     *
     * @param readName
     * @return read block
     */
    public IReadBlock findReadBlock(String readName) {
        final Collection<Integer> all = new HashSet<>(classificationIds);
        try {
            final Document doc = dir.getDocument();
            final IClassificationBlock classificationBlock = doc.getConnector().getClassificationBlock(classificationName);
            all.retainAll(classificationBlock.getKeySet());

            try (IReadBlockIterator it = doc.getConnector().getReadsIteratorForListOfClassIds(classificationName, all, 0, 10000, true, false)) {
                while (it.hasNext()) {
                    final IReadBlock readBlock = it.next();
                    if (readBlock.getReadName().equals(readName))
                        return readBlock;
                }
            }
        } catch (IOException e) {
            Basic.caught(e);
        }
        return null;
    }

    public int getNumberOfSelectedItems() {
        if (getController() == null)
            return 0;
        else
            return getController().getTableView().getSelectionModel().getSelectedItems().size();
    }

    public CompositeObjectSearcher getSearcher() {
        return searcher;
    }

    @Override
    public boolean isShowFindToolBar() {
        return showFindToolBar;
    }

    @Override
    public void setShowFindToolBar(boolean show) {
        this.showFindToolBar = show;
    }

    @Override
    public SearchManager getSearchManager() {
        return searchManager;
    }

    /**
     * get the controller, if it has been setup
     *
     * @return controller or null
     */
    public LRInspectorController getController() {
        if (swingPanel4FX == null)
            return null;
        else
            return swingPanel4FX.getController();
    }

    @Override
    public boolean isReadsAvailable() {
        try {
            if (swingPanel4FX != null) {
                for (TableItem item : swingPanel4FX.getController().getTableView().getSelectionModel().getSelectedItems()) {
                    if (item.getReadLength() > 0)
                        return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public Collection<Pair<String, String>> getReads(int maxNumber) {
        ArrayList<Pair<String, String>> list = new ArrayList<>();
        for (TableItem tableItem : swingPanel4FX.getController().getTableView().getSelectionModel().getSelectedItems()) {
            if (tableItem.getReadLength() > 0 && tableItem.getReadSequence() != null) {
                final Pair<String, String> pair = new Pair<>(tableItem.getReadName(), tableItem.getReadSequence());
                list.add(pair);
                if (maxNumber >= 0 && list.size() >= maxNumber)
                    break;
            }
        }
        return list;
    }

    public int getClassId() {
        return classId;
    }

    public String getClassIdName() {
        return classIdName;
    }

    public String getClassIdDisplayName() {
        if (getClassificationName().equals(Classification.Taxonomy)) {
            int rank = TaxonomyData.getTaxonomicRank(getClassId());
            if (rank != 0) {
                return Basic.abbreviateDotDotDot(TaxonomicLevels.getName(rank) + " " + getClassIdName(), 80);
            }
        }
        return Basic.abbreviateDotDotDot(getClassIdName(), 80);
    }

    public void selectAllCompatible(boolean compatible) {
        Collection<TableItem> tableItems = getController().getTableView().getSelectionModel().getSelectedItems();
        if (tableItems.size() == 0)
            tableItems = getController().getTableView().getItems();
        for (TableItem tableItem : tableItems) {
            tableItem.getPane().selectAllCompatibleTaxa(compatible, classificationName, tableItem.getClassId());
        }
        updateEnableState();
    }

    public void selectAll() {
        final Collection<TableItem> tableItems = getController().getTableView().getSelectionModel().getSelectedItems();
        if (tableItems.size() == 0) { // if no rows selected, selected all rows
            getController().getTableView().getSelectionModel().selectAll();
        } else {
            boolean changed = false; // if some rows selected, select all their matches
            for (TableItem tableItem : tableItems) {
                final int numberPreviouslySelected = tableItem.getPane().getMatchSelection().getSelectedItems().size();
                tableItem.getPane().getMatchSelection().selectAll();
                if (!changed && tableItem.getPane().getMatchSelection().getSelectedItems().size() > numberPreviouslySelected)
                    changed = true;
            }
            if (!changed)  // if some rows selected and all their matches are already selected, select all rows
                getController().getTableView().getSelectionModel().selectAll();
        }
        updateEnableState();
    }

    public void selectNone() {
        final Collection<TableItem> tableItems = getController().getTableView().getSelectionModel().getSelectedItems();
        boolean hasCleared = false;

        for (TableItem tableItem : tableItems) {
            if (tableItem.getPane().getMatchSelection().getSelectedItems().size() > 0)
                hasCleared = true;
            tableItem.getPane().getMatchSelection().clearSelection();
        }

        if (!hasCleared) {
            if (getController().getTableView().getSelectionModel().getSelectedItems().size() > 0) {
                getController().getTableView().getSelectionModel().clearSelection();
                hasCleared = true;
            }
        }
        if (!hasCleared) {
            for (TableItem tableItem : getController().getTableView().getItems()) {
                tableItem.getPane().getMatchSelection().clearSelection();
            }
        }
        updateEnableState();
    }

    public void invertSelectionAlignments() {
        Collection<TableItem> tableItems = getController().getTableView().getSelectionModel().getSelectedItems();
        if (tableItems.size() == 0)
            tableItems = getController().getTableView().getItems();
        for (TableItem tableItem : tableItems) {
            tableItem.getPane().getMatchSelection().invertSelection();
        }
        updateEnableState();
    }

    public void hideSelectedAlignments() {
        Collection<TableItem> tableItems = getController().getTableView().getSelectionModel().getSelectedItems();
        if (tableItems.size() == 0)
            tableItems = getController().getTableView().getItems();
        for (TableItem tableItem : tableItems) {
            tableItem.getPane().hideSelected();
        }
        updateEnableState();
    }

    public void showAllAlignments() {
        Collection<TableItem> tableItems = getController().getTableView().getSelectionModel().getSelectedItems();
        if (tableItems.size() == 0)
            tableItems = getController().getTableView().getItems();
        for (TableItem tableItem : tableItems) {
            tableItem.getPane().showAll();
        }
        updateEnableState();
    }

    public boolean hasSelectedAlignments() {
        if (getController() != null) {
            final Collection<TableItem> tableItems = getController().getTableView().getItems();
            for (TableItem tableItem : tableItems) {
                if (tableItem.getPane().getMatchSelection().getSelectedItems().size() > 0)
                    return true;
            }
        }
        return false;
    }

    public boolean hasHiddenAlignments() {
        if (getController() != null) {
            Collection<TableItem> tableItems = getController().getTableView().getSelectionModel().getSelectedItems();
            if (tableItems.size() == 0)
                tableItems = getController().getTableView().getItems();
            for (TableItem tableItem : tableItems) {
                if (tableItem != null && tableItem.getPane() != null && tableItem.getPane().hasHiddenAlignments())
                    return true;
            }
        }
        return false;
    }

    public void updateEnableState() {
        Runnable runnable = () -> commandManager.updateEnableState();
        if (SwingUtilities.isEventDispatchThread())
            runnable.run();
        else
            SwingUtilities.invokeLater(runnable);
    }

    /**
     * copy all selected alignments
     */
    public void copy() {
        final String selection = getSelection(dir.getDocument().getProgressListener());
        Platform.runLater(() -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(selection);
            clipboard.setContent(content);
        });
    }

    public String getSelection(ProgressListener progress) {
        final StringBuilder buf = new StringBuilder();
        try {
            Collection<TableItem> tableItems = getController().getTableView().getSelectionModel().getSelectedItems();
            if (tableItems.size() == 0)
                tableItems = getController().getTableView().getItems();
            for (TableItem tableItem : tableItems) {
                final ReadLayoutPane pane = tableItem.getPane();
                if (!pane.getMatchSelection().isEmpty()) {
                    buf.append("Query=").append(tableItem.toString()).append("\n");
                    buf.append("# Selected alignments: ").append(pane.getMatchSelection().getSelectedItems().size()).append("\n\n");
                    for (IMatchBlock matchBlock : pane.getMatchSelection().getSelectedItems()) {
                        buf.append(matchBlock.getText()).append("\n");
                        progress.checkForCancel();
                    }
                }
            }
            if (buf.toString().isEmpty()) { // nothing selected, so just report the reads
                for (TableItem tableItem : tableItems) {
                    buf.append("Query=").append(tableItem.toString()).append("\n");
                    progress.checkForCancel();
                }
            }
        } catch (CanceledException ignored) {
        }
        return buf.toString();
    }

    public boolean someSelectedItemHasTaxonLabelsShowing() {
        if (getController() != null) {
            for (TableItem item : getController().getTableView().getItems()) {
                if (item.getPane().getClassificationLabelsShowing().contains(Classification.Taxonomy))
                    return true;
            }
        }
        return false;
    }

    public boolean someSelectedItemHasAnyLabelsShowing() {
        if (getController() != null) {
            for (TableItem item : getController().getTableView().getItems()) {
                if (item.getPane().getClassificationLabelsShowing().size() > 0)
                    return true;
            }
        }
        return false;
    }

    @Override
    public Node getJavaFXRoot() {
        if (getController() != null) {
            return swingPanel4FX.getPanel().getScene().getRoot();
        } else
            return null;
    }


    @Override
    public Stage getJavaFXStage() {
        return null;
    }

    /**
     * save all selected reads
     *
     * @param outputFile
     * @param progressListener
     * @return number saved
     * @throws IOException
     */
    public int exportSelectedReads(String outputFile, ProgressListener progressListener) throws IOException, CanceledException {
        int count = 0;
        if (getController() != null) {
            try (Writer w = new BufferedWriter(new FileWriter(outputFile))) {
                progressListener.setMaximum(getController().getTableView().getSelectionModel().getSelectedItems().size());
                progressListener.setProgress(0);
                for (TableItem item : getController().getTableView().getSelectionModel().getSelectedItems()) {
                    w.write(">" + Basic.swallowLeadingGreaterSign(item.getReadName()) + "\n");
                    w.write(item.getReadSequence() + "\n");
                    count++;
                    progressListener.incrementProgress();
                }
            }

        }
        return count;
    }
}
