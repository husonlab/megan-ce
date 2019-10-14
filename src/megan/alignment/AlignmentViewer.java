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
package megan.alignment;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.director.IViewerWithFindToolBar;
import jloda.swing.director.ProjectManager;
import jloda.swing.find.FindToolBar;
import jloda.swing.find.JListSearcher;
import jloda.swing.find.SearchManager;
import jloda.swing.util.StatusBar;
import jloda.swing.util.ToolBar;
import jloda.swing.window.MenuBar;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.*;
import megan.alignment.commands.*;
import megan.alignment.gui.*;
import megan.alignment.gui.colors.ColorSchemeAminoAcids;
import megan.alignment.gui.colors.ColorSchemeNucleotides;
import megan.alignment.gui.colors.ColorSchemeText;
import megan.assembly.alignment.AlignmentAssembler;
import megan.core.Director;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.util.*;

/**
 * alignment window
 * Daniel Huson, 9.2011
 */
public class AlignmentViewer extends JFrame implements IDirectableViewer, IViewerWithFindToolBar, Printable {
    private boolean uptoDate = true;
    private boolean locked = false;
    private final Director dir;
    private final CommandManager commandManager;
    private final MenuBar menuBar;
    private final StatusBar statusBar;
    private final JTextField classNameTextField = new JTextField();
    private final JTextField referenceNameTextField = new JTextField();
    private final AlignmentViewerPanel alignmentViewerPanel = new AlignmentViewerPanel();

    private final JPanel referencePanel;
    private final JList referenceJList = new JList(new DefaultListModel());
    private String selectedReference = null;

    private boolean showFindToolBar = false;
    private final SearchManager searchManager;

    private String infoString = "";

    private final Blast2Alignment blast2Alignment;

    private boolean showAminoAcids = false;


    public enum AlignmentLayout {
        Mapping, ByStart, ByName, ByContigs, Unsorted;

        public static AlignmentLayout valueOfIgnoreCase(String str) {
            for (AlignmentLayout a : values()) {
                if (a.toString().equalsIgnoreCase(str))
                    return a;
            }
            return Mapping;
        }
    }

    private AlignmentLayout alignmentLayout = AlignmentLayout.valueOfIgnoreCase(ProgramProperties.get("AlignmentLayout", AlignmentLayout.Mapping.toString()));

    private boolean showInsertions = true;

    private String aminoAcidColoringScheme = ProgramProperties.get("AminoAcidColorScheme", ColorSchemeAminoAcids.NAMES.Default.toString());
    private String nucleotideColoringScheme = ProgramProperties.get("NucleotideColorScheme", ColorSchemeNucleotides.NAMES.Default.toString());

    /**
     * constructor
     *
     * @param dir
     */
    public AlignmentViewer(final Director dir) {
        this.dir = dir;
        blast2Alignment = new Blast2Alignment(dir.getDocument());

        statusBar = new StatusBar();
        searchManager = new SearchManager(dir, this, new JListSearcher(referenceJList), false, true);
        commandManager = new CommandManager(dir, this, new String[]{"megan.alignment.commands", "megan.commands"}, !ProgramProperties.isUseGUI());

        setAminoAcidColoringScheme(ProgramProperties.get("AminoAcidColorScheme", "Default"));
        setNucleotideColoringScheme(ProgramProperties.get("NucleotideColorScheme", "Default"));

        setTitle();

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        setJMenuBar(menuBar);
        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());


        final JFrame frame = getFrame();
        frame.setLocationRelativeTo(MainViewer.getLastActiveFrame());
        final int[] geometry = ProgramProperties.get("AlignerViewerGeometry", new int[]{100, 100, 850, 600});
        frame.setSize(geometry[2], geometry[3]);

        JToolBar toolBar = new ToolBar(this, GUIConfiguration.getToolBarConfiguration(), commandManager);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(toolBar, BorderLayout.NORTH);

        JPanel main = new JPanel();
        frame.getContentPane().add(main, BorderLayout.CENTER);
        main.setLayout(new BorderLayout());

        referencePanel = new JPanel();
        referencePanel.setBorder(BorderFactory.createEmptyBorder(1, 10, 5, 10));
        referencePanel.setLayout(new BorderLayout());

        JPanel aPanel = new JPanel();  // need this because north border of reference used by find toolbar
        aPanel.setLayout(new BorderLayout());
        classNameTextField.setEditable(false);
        classNameTextField.setBackground(aPanel.getBackground());
        classNameTextField.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        aPanel.add(classNameTextField, BorderLayout.NORTH);
        aPanel.add(new JScrollPane(referenceJList), BorderLayout.CENTER);
        referencePanel.add(aPanel, BorderLayout.CENTER);

        JPanel bPanel = new JPanel();
        bPanel.setLayout(new BorderLayout());
        referenceNameTextField.setEditable(false);
        referenceNameTextField.setBackground(bPanel.getBackground());
        referenceNameTextField.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bPanel.add(referenceNameTextField, BorderLayout.NORTH);

        bPanel.add(alignmentViewerPanel, BorderLayout.CENTER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(90);
        splitPane.add(referencePanel);
        splitPane.add(bPanel);
        main.add(splitPane, BorderLayout.CENTER);

        JPanel botPanel = new JPanel();
        botPanel.setLayout(new BoxLayout(botPanel, BoxLayout.Y_AXIS));
        botPanel.add(statusBar);

        main.add(botPanel, BorderLayout.SOUTH);

        getContentPane().validate();

        JPopupMenu namesPopup = new JPopupMenu();
        namesPopup.add(commandManager.getJMenuItem(FindReadCommand.NAME));
        namesPopup.add(commandManager.getJMenuItem(CopyReadNamesCommand.NAME));
        namesPopup.addSeparator();
        namesPopup.add(commandManager.getJMenuItem(ShowInspectReadsCommand.NAME));
        namesPopup.addSeparator();
        namesPopup.add(commandManager.getJMenuItem(LayoutByNameCommand.NAME));
        namesPopup.add(commandManager.getJMenuItem(LayoutByStartCommand.NAME));
        namesPopup.add(commandManager.getJMenuItem(LayoutByContigsCommand.NAME));
        namesPopup.addSeparator();
        namesPopup.add(commandManager.getJMenuItem(MoveUpCommand.NAME));
        namesPopup.add(commandManager.getJMenuItem(MoveDownCommand.NAME));
        alignmentViewerPanel.getNamesPanel().setComponentPopupMenu(namesPopup);
        alignmentViewerPanel.setShowAsMapping(true);

        JPopupMenu alignmentPopup = new JPopupMenu();
        alignmentPopup.add(commandManager.getJMenuItem(ShowMatchCommand.NAME));
        alignmentPopup.add(commandManager.getJMenuItem(CopyReadNamesCommand.NAME));
        alignmentPopup.addSeparator();
        alignmentPopup.add(commandManager.getJMenuItem(ShowInspectReadsCommand.NAME));
        alignmentPopup.addSeparator();
        alignmentPopup.add(commandManager.getJMenuItem(ZoomToSelectionCommand.NAME));
        alignmentPopup.add(commandManager.getJMenuItem(ZoomToFitCommand.NAME));
        alignmentPopup.addSeparator();
        alignmentPopup.add(commandManager.getJMenuItem("Select All"));
        alignmentPopup.add(commandManager.getJMenuItem("Select None"));
        alignmentViewerPanel.getAlignmentPanel().setComponentPopupMenu(alignmentPopup);

        ((DefaultListModel) referenceJList.getModel()).addElement("Loading...");
        referenceJList.setEnabled(false);
        referenceJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    ReferenceItem item = (ReferenceItem) referenceJList.getSelectedValue();
                    if (item != null && item.name.length() > 0 && !isLocked()) {
                        setSelectedReference(item.name);
                        dir.execute("apply;", commandManager);
                    } else
                        setSelectedReference(null);
                }
            }

            public void mousePressed(MouseEvent me) {
                super.mousePressed(me);
                if (me.isPopupTrigger()) {
                    referenceJList.setSelectedIndex(referenceJList.locationToIndex(me.getPoint()));
                    JPopupMenu popupMenu = new JPopupMenu();
                    popupMenu.add(commandManager.getJMenuItem(ApplyCommand.NAME));
                    popupMenu.show(referenceJList, me.getX(), me.getY());
                }
            }

            public void mouseReleased(MouseEvent me) {
                super.mouseReleased(me);
                if (me.isPopupTrigger())
                    mousePressed(me);
            }
        });
        referenceJList.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                ReferenceItem item = (ReferenceItem) referenceJList.getSelectedValue();
                if (item != null)
                    setSelectedReference(item.name);
                else
                    setSelectedReference(null);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent windowEvent) {
                super.windowActivated(windowEvent);
                getAlignmentViewerPanel().getAlignmentPanel().setAnimateSelection(true);
            }

            @Override
            public void windowDeactivated(WindowEvent windowEvent) {
                super.windowDeactivated(windowEvent);
                getAlignmentViewerPanel().getAlignmentPanel().setAnimateSelection(false);

                if (!isShowAsMapping()) {
                    Set<String> selectedLabels = new HashSet<>();
                    for (int row = getSelectedBlock().getFirstRow(); row <= getSelectedBlock().getLastRow(); row++) {
                        selectedLabels.add(Basic.getFirstWord(getAlignment().getLane(row).getName()));
                    }

                    if (selectedLabels.size() != 0) {
                        ProjectManager.getPreviouslySelectedNodeLabels().clear();
                        ProjectManager.getPreviouslySelectedNodeLabels().addAll(selectedLabels);
                    }
                }
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                componentResized(e);
            }

            public void componentResized(ComponentEvent event) {
                if ((event.getID() == ComponentEvent.COMPONENT_RESIZED || event.getID() == ComponentEvent.COMPONENT_MOVED) &&
                        (frame.getExtendedState() & JFrame.MAXIMIZED_HORIZ) == 0
                        && (frame.getExtendedState() & JFrame.MAXIMIZED_VERT) == 0) {
                    ProgramProperties.put("AlignerViewerGeometry", new int[]
                            {frame.getLocation().x, frame.getLocation().y, frame.getSize().width,
                                    frame.getSize().height});
                }

            }
        });

        getSelectedBlock().addSelectionListener((selected, minRow, minCol, maxRow, maxCol) -> {
            commandManager.updateEnableState();
            if (getSelectedBlock().isSelected())
                statusBar.setText2("Selection: " + getSelectedBlock().getNumberOfSelectedRows() + " x " + getSelectedBlock().getNumberOfSelectedCols());
            else
                statusBar.setText2(infoString);
        });
        if (alignmentViewerPanel.isShowReference())
            alignmentViewerPanel.setShowReference(true); // rescan size of reference panel

        if (alignmentViewerPanel.isShowAsMapping())
            alignmentViewerPanel.setNamesPanelVisible(false);

        updateView(IDirector.ALL);
    }

    public boolean isUptoDate() {
        return uptoDate;
    }

    public JFrame getFrame() {
        return this;
    }

    public void updateView(String what) {
        setTitle();

        int h = getAlignmentViewerPanel().getAlignmentScrollPane().getHorizontalScrollBar().getValue();
        int v = getAlignmentViewerPanel().getAlignmentScrollPane().getVerticalScrollBar().getValue();

        if (referenceJList.getModel().getSize() == 1 && referenceJList.getModel().getElementAt(0).toString().contains("Loading...")) {
            SortedSet<ReferenceItem> sorted = new TreeSet<>((a, b) -> {
                if (a.count > b.count)
                    return -1;
                else if (a.count < b.count)
                    return 1;
                else
                    return a.getText().compareTo(b.getText());
            });
            for (String reference : blast2Alignment.getReferences()) {
                sorted.add(new ReferenceItem(blast2Alignment.getReference2Count(reference), reference));
            }

            if (sorted.size() > 0) {
                ((DefaultListModel) referenceJList.getModel()).removeElementAt(0);
                for (ReferenceItem item : sorted) {
                    ((DefaultListModel) referenceJList.getModel()).addElement(item);
                }
                referenceJList.setEnabled(true);
            }/*
            else {
                ((DefaultListModel) referenceJList.getModel()).removeElementAt(0);
                ((DefaultListModel) referenceJList.getModel()).addElement("No alignments available");
            }
            */

        }

        if (referenceJList.getModel().getSize() > 1)
            classNameTextField.setText("List of " + referenceJList.getModel().getSize() + " available reference sequences for '" + blast2Alignment.getClassName() + "' (double click on one to see alignment):");

        if (selectedReference == null)
            referenceNameTextField.setText("Alignment panel:");
        else
            referenceNameTextField.setText("Alignment for reference sequence '" + selectedReference + "':");


        alignmentViewerPanel.setShowReference(alignmentViewerPanel.isShowReference()); // rescan size of reference panel
        alignmentViewerPanel.setShowConsensus(alignmentViewerPanel.isShowConsensus()); // rescan size of consensus panel

        alignmentViewerPanel.revalidateGrid();
        commandManager.updateEnableState();

        getAlignmentViewerPanel().getAlignmentScrollPane().getHorizontalScrollBar().setValue(h);
        getAlignmentViewerPanel().getAlignmentScrollPane().getVerticalScrollBar().setValue(v);

        statusBar.setText1("Alignment: " + getAlignment().getNumberOfSequences() + " x " + getAlignment().getLength());
        statusBar.setText2(infoString);

        FindToolBar findToolBar = searchManager.getFindDialogAsToolBar();
        if (findToolBar.isClosing()) {
            showFindToolBar = false;
            findToolBar.setClosing(false);
        }
        if (!findToolBar.isEnabled() && showFindToolBar) {
            referencePanel.add(findToolBar, BorderLayout.NORTH);
            referencePanel.setPreferredSize(new Dimension(200, 1000000));
            findToolBar.setEnabled(true);
            getContentPane().validate();
            getCommandManager().updateEnableState();
        } else if (findToolBar.isEnabled() && !showFindToolBar) {
            referencePanel.remove(findToolBar);
            findToolBar.setEnabled(false);
            getContentPane().validate();
            getCommandManager().updateEnableState();
        }
        if (findToolBar.isEnabled())
            findToolBar.clearMessage();

    }

    public boolean isShowInsertions() {
        return showInsertions;
    }

    public void setShowInsertions(boolean showInsertions) {
        this.showInsertions = showInsertions;
    }

    static class ReferenceItem extends JButton {
        final int count;
        final String name;

        ReferenceItem(Integer count, String name) {
            this.count = count;
            this.name = name;
            setBold(true);
        }

        void setBold(boolean bold) {
            if (bold)
                setText("<html><strong>" + name + ":: " + count + "</strong></html>");     // store negative count to get correct sorting
            else
                setText(name + ":: " + count);     // store negative count to get correct sorting
        }

        public String toString() {
            return getText();
        }
    }

    /**
     * gets the selected block
     *
     * @return selected block
     */
    public SelectedBlock getSelectedBlock() {
        return alignmentViewerPanel.getSelectedBlock();
    }

    public void lockUserInput() {
        statusBar.setText1("");
        statusBar.setText2("Busy...");
        locked = true;
        commandManager.setEnableCritical(false);
        searchManager.getFindDialogAsToolBar().setEnableCritical(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        referenceJList.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void unlockUserInput() {
        commandManager.setEnableCritical(true);
        searchManager.getFindDialogAsToolBar().setEnableCritical(true);
        locked = false;
        setCursor(Cursor.getDefaultCursor());
        referenceJList.setCursor(Cursor.getDefaultCursor());
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
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());

        ProgramProperties.put("AlignerViewerGeometry", new int[]
                {getLocation().x, getLocation().y, getSize().width, getSize().height});

        searchManager.getFindDialogAsToolBar().close();

        alignmentViewerPanel.getAlignmentPanel().close();
        setVisible(false);
        dir.removeViewer(this);
        dispose();
    }

    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * set the title of the window
     */
    private void setTitle() {
        String className = getAlignment().getName();
        if (className == null)
            className = "Untitled";
        else if (className.length() > 80)
            className = className.substring(0, 80) + ".";

        String newTitle = "Alignment for '" + className + "' - " + dir.getDocument().getTitle() + " - " + ProgramProperties.getProgramName();

        if (!getFrame().getTitle().equals(newTitle)) {
            getFrame().setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * set the alignment for this viewer
     *
     * @param alignment
     * @param updateInfoString
     */
    public void setAlignment(Alignment alignment, boolean updateInfoString) {
        lockUserInput();

        for (int i = 0; i < referenceJList.getModel().getSize(); i++) {
            ReferenceItem item = (ReferenceItem) referenceJList.getModel().getElementAt(i);
            if (item.name.equals(getSelectedReference())) {
                item.setBold(false);
                break;
            }
        }

        if (alignment.getNumberOfSequences() > 0 && alignment.getLength() > 0 && alignment.getReference().getLength() == 0)
            alignmentViewerPanel.setShowReference(false);

        alignment.getGapColumnContractor().processAlignment(alignment);
        try {
            setAlignmentLayout(getAlignmentLayout(), new ProgressPercentage());
        } catch (CanceledException ignored) {
        }
        if (isShowAsMapping()) {
            alignment.getRowCompressor().update();
        } else {
            alignment.getRowCompressor().clear();
            try {
                if (getAlignmentLayout() != AlignmentLayout.ByContigs) // todo: not sure this is ok, but want to avoid doing assembly twice
                    setAlignmentLayout(getAlignmentLayout(), new ProgressPercentage());
            } catch (CanceledException ignored) {
            }
        }

        if (!alignment.getReferenceType().equals(alignment.getSequenceType())) {
            alignmentViewerPanel.getAlignmentPanel().setColorMatchesVsReference(true);
            alignmentViewerPanel.getAlignmentPanel().setColorMismatchesVsReference(true);
        }

        alignmentViewerPanel.zoom("both", "fit", null);

        final SelectedBlock selectedBlock = getSelectedBlock();
        final AlignmentPanel alignmentPanel = alignmentViewerPanel.getAlignmentPanel();
        final NamesPanel namesPanel = alignmentViewerPanel.getNamesPanel();
        final ReferencePanel referencePanel = alignmentViewerPanel.getReferencePanel();
        final ConsensusPanel consensusPanel = alignmentViewerPanel.getConsensusPanel();
        final AxisPanel axisPanel = alignmentViewerPanel.getAxisPanel();

        selectedBlock.setTotalRows(alignment.getNumberOfSequences());
        selectedBlock.setTotalCols(alignment.getLength());
        namesPanel.setAlignment(alignment);
        axisPanel.setAlignment(alignment);
        alignmentPanel.setAlignment(alignment);
        referencePanel.setAlignment(alignment);
        consensusPanel.setAlignment(alignment);

        switch (alignment.getSequenceType()) {
            case Alignment.PROTEIN:
                alignmentPanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
                referencePanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
                consensusPanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
                break;
            case Alignment.DNA:
                alignmentPanel.setColorScheme(new ColorSchemeNucleotides(nucleotideColoringScheme));
                referencePanel.setColorScheme(new ColorSchemeNucleotides(nucleotideColoringScheme));
                consensusPanel.setColorScheme(new ColorSchemeNucleotides(nucleotideColoringScheme));
                break;
            case Alignment.cDNA:
                if (alignment.isTranslate()) {
                    alignmentPanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
                    consensusPanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
                } else {
                    alignmentPanel.setColorScheme(new ColorSchemeNucleotides(nucleotideColoringScheme));
                    consensusPanel.setColorScheme(new ColorSchemeNucleotides(nucleotideColoringScheme));
                }
                referencePanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
                break;
            default:
                alignmentPanel.setColorScheme(new ColorSchemeText());
                consensusPanel.setColorScheme(new ColorSchemeText());
                referencePanel.setColorScheme(new ColorSchemeText());
                break;
        }

        if (alignment.getNumberOfSequences() < 5000 && alignment.getLength() < 10000)
            if (alignment.getSequenceType().equals(Alignment.DNA) || alignment.getSequenceType().equals(Alignment.cDNA)) {
                try {
                    Pair<Double, Double> gcAndCoverage = ComputeAlignmentProperties.computeCGContentAndCoverage(alignment, new ProgressCmdLine());
                    infoString = String.format("GC-content=%.1f %% Coverage=%.1f", gcAndCoverage.getFirst(), gcAndCoverage.getSecond());
                } catch (CanceledException e) {
                    Basic.caught(e);
                }
            }
        alignmentViewerPanel.zoom("both", "fit", null);

        // System.err.println("Consensus:\n"+alignment.getConsensus());
    }

    /**
     * Print the frame associated with this viewer.
     *
     * @param gc0        the graphics context.
     * @param format     page format
     * @param pagenumber page index
     */

    public int print(Graphics gc0, PageFormat format, int pagenumber) throws PrinterException {
        if (pagenumber == 0) {
            Graphics2D gc = ((Graphics2D) gc0);
            gc.setFont(getFont());

            Dimension dim = getContentPane().getSize();

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

            getContentPane().paint(gc);

            return Printable.PAGE_EXISTS;
        } else
            return Printable.NO_SUCH_PAGE;
    }

    public boolean isShowAminoAcids() {
        return showAminoAcids;
    }

    /**
     * show amino acids in alignment
     *
     * @param showAminoAcids
     */
    public void setShowAminoAcids(boolean showAminoAcids) {
        this.showAminoAcids = showAminoAcids;

        if (getAlignment().getSequenceType().equals(Alignment.cDNA))
            getAlignment().setTranslate(showAminoAcids);

        final AlignmentPanel alignmentPanel = alignmentViewerPanel.getAlignmentPanel();
        final ConsensusPanel consensusPanel = alignmentViewerPanel.getConsensusPanel();
        final ReferencePanel referencePanel = alignmentViewerPanel.getReferencePanel();

        if (showAminoAcids && (getAlignment().getSequenceType().equals(Alignment.PROTEIN) || getAlignment().getSequenceType().equals(Alignment.cDNA))) {
            alignmentPanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
            consensusPanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
            referencePanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
        } else if (getAlignment().getSequenceType().equals(Alignment.DNA)) {
            alignmentPanel.setColorScheme(new ColorSchemeNucleotides(nucleotideColoringScheme));
            consensusPanel.setColorScheme(new ColorSchemeNucleotides(nucleotideColoringScheme));
            referencePanel.setColorScheme(new ColorSchemeAminoAcids(nucleotideColoringScheme));
        } else if (getAlignment().getSequenceType().equals(Alignment.cDNA)) {
            alignmentPanel.setColorScheme(new ColorSchemeNucleotides(nucleotideColoringScheme));
            consensusPanel.setColorScheme(new ColorSchemeNucleotides(nucleotideColoringScheme));
            referencePanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColoringScheme));
        } else {
            alignmentPanel.setColorScheme(new ColorSchemeText());
            consensusPanel.setColorScheme(new ColorSchemeText());
            referencePanel.setColorScheme(new ColorSchemeText());
        }

        if (alignmentPanel.getAlignment().isTranslate() && getSelectedBlock().getNumberOfSelectedCols() > 0)
            getSelectedBlock().selectCols(getSelectedBlock().getFirstCol(), getSelectedBlock().getLastCol(), true);
    }

    public boolean isContractGaps() {
        return getAlignment().getGapColumnContractor().isEnabled();
    }

    public void setContractGaps(boolean contract) {
        getAlignment().getGapColumnContractor().setEnabled(contract);
        if (contract)
            alignmentViewerPanel.zoom("horizontal", "fit", null);
    }

    /**
     * get the alignment
     *
     * @return alignment
     */
    public Alignment getAlignment() {
        return alignmentViewerPanel.getAlignment();
    }


    public boolean isAllowNucleotides() {
        return getAlignment() != null && (getAlignment().getSequenceType().equals(Alignment.DNA) || getAlignment().getSequenceType().equals(Alignment.cDNA));
    }

    public boolean isAllowAminoAcids() {
        return getAlignment() != null && (getAlignment().getSequenceType().equals(Alignment.PROTEIN) || getAlignment().getSequenceType().equals(Alignment.cDNA));
    }

    public AlignmentLayout getAlignmentLayout() {
        return alignmentLayout;
    }

    public void setAlignmentLayout(AlignmentLayout alignmentLayout, final ProgressListener progressListener) throws CanceledException {
        this.alignmentLayout = alignmentLayout;
        ProgramProperties.put("AlignmentViewLayout", alignmentLayout.toString());

        switch (alignmentLayout) {
            case Unsorted:
                getAlignmentViewerPanel().setNamesPanelVisible(true);
                getAlignment().getRowCompressor().clear();
                alignmentViewerPanel.setShowAsMapping(false);
                AlignmentSorter.sortByOriginalOrder(getAlignment());
                break;

            case ByStart:
                getAlignmentViewerPanel().setNamesPanelVisible(true);
                getAlignment().getRowCompressor().clear();
                alignmentViewerPanel.setShowAsMapping(false);
                AlignmentSorter.sortByStart(getAlignment(), false);
                break;
            case ByName:
                getAlignmentViewerPanel().setNamesPanelVisible(true);
                getAlignment().getRowCompressor().clear();
                alignmentViewerPanel.setShowAsMapping(false);
                AlignmentSorter.sortByName(getAlignment(), false);
                break;
            case ByContigs:
                try {
                    getAlignmentViewerPanel().setNamesPanelVisible(true);
                    getAlignment().getRowCompressor().clear();
                    alignmentViewerPanel.setShowAsMapping(false);
                    progressListener.setTasks("Sorting reads by contigs", "Assembling");
                    final AlignmentAssembler alignmentAssembler = new AlignmentAssembler();
                    final int minOverlap = ProgramProperties.get("AssemblyMinOverlap", 20);
                    System.err.println("Assuming minOverlap=" + minOverlap + " (use setProp AssemblyMinOverlap=<number> to change)");
                    alignmentAssembler.computeOverlapGraph(minOverlap, getAlignment(), progressListener);

                    final int count = alignmentAssembler.computeContigs(0, 0, 0, 0, true, progressListener);
                    repaint();
                    NotificationsInSwing.showInformation(getFrame(), "Number of contigs assembled: " + count);
                } catch (IOException e) {
                    Basic.caught(e);
                }
                break;
            default:
            case Mapping:
                getAlignmentViewerPanel().setNamesPanelVisible(false);
                AlignmentSorter.sortByOriginalOrder(getAlignment());
                alignmentViewerPanel.setShowAsMapping(true);
                getAlignment().getRowCompressor().update();
                break;

        }
        repaint();
    }

    public Blast2Alignment getBlast2Alignment() {
        return blast2Alignment;
    }

    public AlignmentViewerPanel getAlignmentViewerPanel() {
        return alignmentViewerPanel;
    }

    public boolean isShowAsMapping() {
        return alignmentViewerPanel.isShowAsMapping();
    }

    public void setShowAsMapping(boolean showAsMapping) {
        alignmentViewerPanel.setShowAsMapping(showAsMapping);
    }

    public String getAminoAcidColoringScheme() {
        return aminoAcidColoringScheme;
    }

    public void setAminoAcidColoringScheme(String aminoAcidColoringScheme) {
        this.aminoAcidColoringScheme = aminoAcidColoringScheme;
    }

    public String getNucleotideColoringScheme() {
        return nucleotideColoringScheme;
    }

    public void setNucleotideColoringScheme(String nucleotideColoringScheme) {
        this.nucleotideColoringScheme = nucleotideColoringScheme;
    }

    public String getSelectedReference() {
        return selectedReference;
    }

    private void setSelectedReference(String selectedReference) {
        this.selectedReference = selectedReference;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }

    public boolean isShowFindToolBar() {
        return showFindToolBar;
    }

    public void setShowFindToolBar(boolean showFindToolBar) {
        this.showFindToolBar = showFindToolBar;
    }


    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "AlignmentViewer";
    }

}

