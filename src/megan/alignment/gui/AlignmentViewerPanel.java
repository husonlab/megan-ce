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
package megan.alignment.gui;

import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.alignment.gui.colors.ColorSchemeAminoAcids;
import megan.alignment.gui.colors.ColorSchemeNucleotides;
import megan.alignment.gui.colors.ColorSchemeText;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;

/**
 * an alignment viewer panel
 * Daniel Huson, 9.2011
 */
public class AlignmentViewerPanel extends JPanel {
    private final int laneHeight = 20;

    private final SelectedBlock selectedBlock = new SelectedBlock();

    private final JSplitPane splitPane;

    private final JPanel emptyPanel;
    private final JPanel emptyPanel2;
    private final AlignmentPanel alignmentPanel;
    private final NamesPanel namesPanel;
    private final AxisPanel axisPanel;
    private final ReferencePanel referencePanel;
    private final ConsensusPanel consensusPanel;

    private final JScrollPane namesScrollPane;
    private final JScrollPane referenceScrollPane;
    private final JScrollPane consensusScrollPane;

    private final JScrollPane alignmentScrollPane;
    private final JScrollPane axisScrollPane;

    private static final int DEFAULT_SCALE = 11;
    private double hScale = DEFAULT_SCALE;
    private double vScale = DEFAULT_SCALE;

    private final JButton showRefButton;

    private final JLabel blastTypeLabel = new JLabel();

    private boolean showReference = true;
    private boolean showConsensus = true;
    private boolean showAsMapping = false;

    private final String aminoAcidColorScheme = ColorSchemeAminoAcids.NAMES.Default.toString();
    private final String nuceoltidesColorScheme = ColorSchemeNucleotides.NAMES.Default.toString();

    /**
     * constructor
     */
    public AlignmentViewerPanel() {
        JPanel mainPanel = this;
        mainPanel.setLayout(new BorderLayout());

        emptyPanel = new JPanel();
        emptyPanel.setLayout(new BorderLayout());
        emptyPanel.setMinimumSize(new Dimension(100, laneHeight));
        emptyPanel.setPreferredSize(new Dimension(100, laneHeight));
        emptyPanel.setMaximumSize(new Dimension(100000, laneHeight));
        blastTypeLabel.setBackground(emptyPanel.getBackground());
        blastTypeLabel.setForeground(Color.LIGHT_GRAY);
        //emptyPanel.add(blastTypeLabel,BorderLayout.NORTH);
        GridBagConstraints emptyGBC = new GridBagConstraints();
        emptyGBC.gridx = 0;
        emptyGBC.gridy = 0;
        emptyGBC.fill = GridBagConstraints.BOTH;
        emptyGBC.insets = new Insets(3, 0, 0, 4);
        emptyGBC.weightx = 1;
        emptyGBC.weighty = 0;

        emptyPanel2 = new JPanel();
        emptyPanel2.setLayout(new BorderLayout());
        emptyPanel2.setMinimumSize(new Dimension(100, 0));
        emptyPanel2.setPreferredSize(new Dimension(100, 0));
        emptyPanel2.setMaximumSize(new Dimension(100000, 0));

        GridBagConstraints empty2GBC = new GridBagConstraints();
        empty2GBC.gridx = 0;
        empty2GBC.gridy = 2;
        empty2GBC.fill = GridBagConstraints.BOTH;
        empty2GBC.insets = new Insets(3, 0, 0, 4);
        empty2GBC.weightx = 1;
        empty2GBC.weighty = 0;

        final JButton upButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (AlignmentSorter.moveUp(getAlignment(), selectedBlock.getFirstRow(), selectedBlock.getLastRow())) {
                    selectedBlock.setFirstRow(selectedBlock.getFirstRow() - 1);
                    selectedBlock.setLastRow(selectedBlock.getLastRow() - 1);
                    selectedBlock.fireSelectionChanged();
                    repaint();
                }
            }
        });
        upButton.setBorder(BorderFactory.createEmptyBorder());
        upButton.setToolTipText("Move selected sequences up");
        upButton.setMaximumSize(new Dimension(16, 16));
        upButton.setIcon(ResourceManager.getIcon("sun/Up16.gif"));

        final JButton downButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (AlignmentSorter.moveDown(getAlignment(), selectedBlock.getFirstRow(), selectedBlock.getLastRow())) {
                    selectedBlock.setFirstRow(selectedBlock.getFirstRow() + 1);
                    selectedBlock.setLastRow(selectedBlock.getLastRow() + 1);
                    selectedBlock.fireSelectionChanged();
                    repaint();
                }
            }
        });
        downButton.setBorder(BorderFactory.createEmptyBorder());
        downButton.setToolTipText("Move selected sequences down");
        downButton.setMaximumSize(new Dimension(16, 16));
        downButton.setIcon(ResourceManager.getIcon("sun/Down16.gif"));

        selectedBlock.addSelectionListener((selected, minRow, minCol, maxRow, maxCol) -> {
            upButton.setEnabled(getAlignment().getNumberOfSequences() > 0 && selectedBlock.isSelected() && selectedBlock.getFirstRow() > 0);
            downButton.setEnabled(getAlignment().getNumberOfSequences() > 0 && selectedBlock.isSelected() && selectedBlock.getLastRow() < getAlignment().getNumberOfSequences() - 1);

        });

        showRefButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                setShowReference(!isShowReference());
            }
        });
        showRefButton.setBorder(BorderFactory.createEmptyBorder());
        showRefButton.setToolTipText("Show/hide reference sequence (if available)");
        showRefButton.setMaximumSize(new Dimension(16, 16));
        showRefButton.setIcon(ResourceManager.getIcon("sun/Forward16.gif"));

        JButton showAsMappingButton = new JButton(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                setShowAsMapping(!isShowAsMapping());
            }
        });
        showAsMappingButton.setBorder(BorderFactory.createEmptyBorder());
        showAsMappingButton.setToolTipText("Show As Mapping");
        //showAsMappingButton.setMaximumSize(new Dimension(16, 16));

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(upButton);
        row.add(downButton);
        row.add(Box.createHorizontalGlue());
        row.add(showRefButton);
        row.add(showAsMappingButton);
        emptyPanel.add(row, BorderLayout.SOUTH);

        namesPanel = new NamesPanel(selectedBlock);
        namesScrollPane = new JScrollPane(namesPanel);
        namesScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        namesScrollPane.setPreferredSize(new Dimension(100, 50));
        namesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        namesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        namesScrollPane.setAutoscrolls(true);
        namesScrollPane.setWheelScrollingEnabled(false);
        namesScrollPane.getViewport().setBackground(new Color(240, 240, 240));

        GridBagConstraints namesGBC = new GridBagConstraints();
        namesGBC.gridx = 0;
        namesGBC.gridy = 1;
        namesGBC.fill = GridBagConstraints.BOTH;
        namesGBC.insets = new Insets(0, 2, 0, 0);
        namesGBC.weightx = 1;
        namesGBC.weighty = 1;

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.add(emptyPanel, emptyGBC);
        leftPanel.add(namesScrollPane, namesGBC);
        leftPanel.add(emptyPanel2, empty2GBC);

        axisPanel = new AxisPanel(selectedBlock);
        axisScrollPane = new JScrollPane(axisPanel);
        axisScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        axisScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        axisScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        axisScrollPane.setMinimumSize(new Dimension(150, laneHeight));
        axisScrollPane.setMaximumSize(new Dimension(100000, laneHeight));
        axisScrollPane.setPreferredSize(new Dimension(100000, laneHeight));
        axisScrollPane.getVerticalScrollBar().setEnabled(false);
        axisScrollPane.setAutoscrolls(true);
        axisScrollPane.setWheelScrollingEnabled(false);
        axisScrollPane.getViewport().setBackground(new Color(240, 240, 240));

        GridBagConstraints axisGBC = new GridBagConstraints();
        axisGBC.gridx = 0;
        axisGBC.gridy = 0;
        axisGBC.weightx = 1;
        axisGBC.weighty = 0;
        axisGBC.fill = GridBagConstraints.HORIZONTAL;

        referencePanel = new ReferencePanel(selectedBlock);
        referenceScrollPane = new JScrollPane(referencePanel);
        referenceScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        referenceScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        referenceScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        referenceScrollPane.setMinimumSize(new Dimension(150, 0));
        referenceScrollPane.setMaximumSize(new Dimension(100000, 0));
        referenceScrollPane.setPreferredSize(new Dimension(100000, 0));
        referenceScrollPane.getVerticalScrollBar().setEnabled(false);
        referenceScrollPane.getViewport().setBackground(new Color(240, 240, 240));
        referenceScrollPane.setAutoscrolls(true);
        referenceScrollPane.setWheelScrollingEnabled(false);

        GridBagConstraints referenceGBC = new GridBagConstraints();
        referenceGBC.gridx = 0;
        referenceGBC.gridy = 1;
        referenceGBC.weightx = 1;
        referenceGBC.weighty = 0;
        referenceGBC.fill = GridBagConstraints.HORIZONTAL;

        consensusPanel = new ConsensusPanel(selectedBlock);
        consensusScrollPane = new JScrollPane(consensusPanel);
        consensusScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        consensusScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        consensusScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        consensusScrollPane.setMinimumSize(new Dimension(150, 0));
        consensusScrollPane.setMaximumSize(new Dimension(100000, 0));
        consensusScrollPane.setPreferredSize(new Dimension(100000, 0));
        consensusScrollPane.getVerticalScrollBar().setEnabled(false);
        consensusScrollPane.getViewport().setBackground(new Color(240, 240, 240));
        consensusScrollPane.setAutoscrolls(true);
        consensusScrollPane.setWheelScrollingEnabled(false);

        GridBagConstraints consensusGBC = new GridBagConstraints();
        consensusGBC.gridx = 0;
        consensusGBC.gridy = 3;
        consensusGBC.weightx = 1;
        consensusGBC.weighty = 0;
        consensusGBC.fill = GridBagConstraints.HORIZONTAL;

        alignmentPanel = new AlignmentPanel(selectedBlock);
        alignmentScrollPane = new JScrollPane(alignmentPanel);
        alignmentScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        alignmentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        alignmentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        alignmentScrollPane.getViewport().setBackground(new Color(240, 240, 240));
        alignmentScrollPane.setAutoscrolls(true);

        GridBagConstraints alignmentGBC = new GridBagConstraints();
        alignmentGBC.gridx = 0;
        alignmentGBC.gridy = 2;
        alignmentGBC.weightx = 1;
        alignmentGBC.weighty = 1;
        alignmentGBC.insets = new Insets(2, 0, 0, 0);
        alignmentGBC.fill = GridBagConstraints.BOTH;

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new GridBagLayout());
        rightPanel.add(axisScrollPane, axisGBC);
        rightPanel.add(referenceScrollPane, referenceGBC);
        rightPanel.add(alignmentScrollPane, alignmentGBC);
        rightPanel.add(consensusScrollPane, consensusGBC);

        splitPane = new JSplitPane();
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                super.componentResized(componentEvent);
                if (isShowAsMapping() && isNamesPanelVisible()) {
                    setNamesPanelVisible(false);
                    alignmentPanel.getScrollPane().revalidate();
                }
            }
        });
        mainPanel.add(splitPane);

        axisScrollPane.getHorizontalScrollBar().setModel(alignmentScrollPane.getHorizontalScrollBar().getModel());
        referenceScrollPane.getHorizontalScrollBar().setModel(alignmentScrollPane.getHorizontalScrollBar().getModel());
        namesScrollPane.getVerticalScrollBar().setModel(alignmentScrollPane.getVerticalScrollBar().getModel());
        consensusScrollPane.getHorizontalScrollBar().setModel(alignmentScrollPane.getHorizontalScrollBar().getModel());

        axisScrollPane.addMouseWheelListener(e -> {
        });
        referenceScrollPane.addMouseWheelListener(e -> {
        });
        consensusScrollPane.addMouseWheelListener(e -> {
        });
        namesScrollPane.addMouseWheelListener(e -> {
            if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                namesScrollPane.getVerticalScrollBar().setValue(alignmentScrollPane.getVerticalScrollBar().getValue() + e.getUnitsToScroll());
            }
        });

        alignmentPanel.addMouseWheelListener(e -> {
            if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                boolean doScaleHorizontal = !e.isMetaDown() && !e.isAltDown() && !e.isShiftDown();
                boolean doScaleVertical = !e.isMetaDown() && !e.isAltDown() && e.isShiftDown();
                boolean doScrollVertical = !e.isMetaDown() && e.isAltDown() && !e.isShiftDown();
                boolean doScrollHorizontal = !e.isMetaDown() && e.isAltDown() && e.isShiftDown();
                if (ProgramProperties.isMacOS()) // has two-dimensional scrolling
                {
                    boolean tmp = doScaleHorizontal;
                    doScaleHorizontal = doScaleVertical;
                    doScaleVertical = tmp;
                }
                if (doScrollVertical) { //scroll
                    alignmentScrollPane.getVerticalScrollBar().setValue(alignmentScrollPane.getVerticalScrollBar().getValue() + e.getUnitsToScroll());
                } else if (doScaleVertical) { //scale
                    double toScroll = 1.0 + (e.getUnitsToScroll() / 100.0);
                    double s = (toScroll > 0 ? 1.0 / toScroll : toScroll);
                    double scale = s * vScale;
                    if (scale > 0 && scale < 100) {
                        zoom("vertical", "" + scale, e.getPoint());
                    }
                } else if (doScrollHorizontal) {
                    alignmentScrollPane.getHorizontalScrollBar().setValue(alignmentScrollPane.getHorizontalScrollBar().getValue() + e.getUnitsToScroll());
                } else if (doScaleHorizontal) { //scale
                    double units = 1.0 + (e.getUnitsToScroll() / 100.0);
                    double s = (units > 0 ? 1.0 / units : units);
                    double scale = s * hScale;
                    if (scale > 0 && scale < 100) {
                        zoom("horizontal", "" + scale, e.getPoint());
                    }
                }
            }
        });
    }

    /**
     * connect or disconnect scrollbar of name panel with that of alignment panel
     *
     * @param connect
     */
    private void connectNamePanel2AlignmentPane(boolean connect) {
        if (connect)
            namesScrollPane.getVerticalScrollBar().setModel(alignmentScrollPane.getVerticalScrollBar().getModel());
        else
            namesScrollPane.getVerticalScrollBar().setModel(new DefaultBoundedRangeModel());
    }

    /**
     * get the alignment
     *
     * @return alignment
     */
    public Alignment getAlignment() {
        return alignmentPanel.getAlignment();
    }

    /**
     * set the alignment for this viewer
     *
     * @param alignment
     */
    public void setAlignment(Alignment alignment) {
        alignment.getGapColumnContractor().processAlignment(alignment);
        if (isShowAsMapping()) {
            alignment.getRowCompressor().update();
        } else {
            alignment.getRowCompressor().clear();
        }

        showRefButton.setEnabled(alignment.getReference().getLength() > 0);

        selectedBlock.setTotalRows(alignment.getNumberOfSequences());
        selectedBlock.setTotalCols(alignment.getLength());
        namesPanel.setAlignment(alignment);
        axisPanel.setAlignment(alignment);
        alignmentPanel.setAlignment(alignment);
        referencePanel.setAlignment(alignment);
        consensusPanel.setAlignment(alignment);

        switch (alignment.getSequenceType()) {
            case Alignment.PROTEIN:
                alignmentPanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColorScheme));
                consensusPanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColorScheme));
                break;
            case Alignment.DNA:
                alignmentPanel.setColorScheme(new ColorSchemeNucleotides(nuceoltidesColorScheme));
                consensusPanel.setColorScheme(new ColorSchemeNucleotides(nuceoltidesColorScheme));
                break;
            case Alignment.cDNA:
                alignmentPanel.setColorScheme(new ColorSchemeNucleotides(nuceoltidesColorScheme));
                consensusPanel.setColorScheme(new ColorSchemeNucleotides(nuceoltidesColorScheme));
                break;
            default:
                alignmentPanel.setColorScheme(new ColorSchemeText());
                consensusPanel.setColorScheme(new ColorSchemeText());
                break;
        }
        switch (alignment.getReferenceType()) {
            case Alignment.PROTEIN:
                referencePanel.setColorScheme(new ColorSchemeAminoAcids(aminoAcidColorScheme));
                break;
            case Alignment.DNA:
                referencePanel.setColorScheme(new ColorSchemeNucleotides(nuceoltidesColorScheme));
                break;
            case Alignment.cDNA:
                referencePanel.setColorScheme(new ColorSchemeNucleotides(nuceoltidesColorScheme));
                break;
            default:
                referencePanel.setColorScheme(new ColorSchemeText());
                break;
        }
        setShowConsensus(true);
    }

    public AlignmentPanel getSequencePanel() {
        return alignmentPanel;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public SelectedBlock getSelectedBlock() {
        return selectedBlock;
    }

    public void setShowReference(boolean showReference) {
        this.showReference = showReference;
        if (showReference) {
            emptyPanel.setMinimumSize(new Dimension(100, laneHeight + (int) referencePanel.getSize().getHeight()));
            emptyPanel.setPreferredSize(new Dimension(100, laneHeight + (int) referencePanel.getSize().getHeight()));
            emptyPanel.setMaximumSize(new Dimension(100000, laneHeight + (int) referencePanel.getSize().getHeight()));
            emptyPanel.revalidate();
            referenceScrollPane.setMinimumSize(new Dimension(150, (int) referencePanel.getSize().getHeight()));
            referenceScrollPane.setMaximumSize(new Dimension(100000, (int) referencePanel.getSize().getHeight()));
            referenceScrollPane.setPreferredSize(new Dimension(100000, (int) referencePanel.getSize().getHeight()));
            referenceScrollPane.revalidate();
            referencePanel.revalidateGrid();
        } else {
            emptyPanel.setMinimumSize(new Dimension(100, laneHeight));
            emptyPanel.setPreferredSize(new Dimension(100, laneHeight));
            emptyPanel.setMaximumSize(new Dimension(100000, laneHeight));
            emptyPanel.revalidate();
            referenceScrollPane.setMinimumSize(new Dimension(150, 0));
            referenceScrollPane.setMaximumSize(new Dimension(100000, 0));
            referenceScrollPane.setPreferredSize(new Dimension(100000, 0));
            referenceScrollPane.revalidate();
            referencePanel.revalidateGrid();
        }
    }

    public void setShowConsensus(boolean showConsensus) {
        this.showConsensus = showConsensus;
        if (showConsensus) {
            emptyPanel2.setMinimumSize(new Dimension(100, (int) consensusPanel.getSize().getHeight()));
            emptyPanel2.setPreferredSize(new Dimension(100, (int) consensusPanel.getSize().getHeight()));
            emptyPanel2.setMaximumSize(new Dimension(100000, (int) consensusPanel.getSize().getHeight()));
            emptyPanel2.revalidate();
            consensusScrollPane.setMinimumSize(new Dimension(150, (int) consensusPanel.getSize().getHeight()));
            consensusScrollPane.setMaximumSize(new Dimension(100000, (int) consensusPanel.getSize().getHeight()));
            consensusScrollPane.setPreferredSize(new Dimension(100000, (int) consensusPanel.getSize().getHeight()));
            consensusScrollPane.revalidate();
            consensusPanel.revalidateGrid();
        } else {
            emptyPanel2.setMinimumSize(new Dimension(100, 0));
            emptyPanel2.setPreferredSize(new Dimension(100, 0));
            emptyPanel2.setMaximumSize(new Dimension(100000, 0));
            emptyPanel2.revalidate();
            consensusScrollPane.setMinimumSize(new Dimension(150, 0));
            consensusScrollPane.setMaximumSize(new Dimension(100000, 0));
            consensusScrollPane.setPreferredSize(new Dimension(100000, 0));
            consensusScrollPane.revalidate();
            consensusPanel.revalidateGrid();
        }
    }


    public int getLaneHeight() {
        return laneHeight;
    }

    public JPanel getEmptyPanel() {
        return emptyPanel;
    }

    public AlignmentPanel getAlignmentPanel() {
        return alignmentPanel;
    }

    public NamesPanel getNamesPanel() {
        return namesPanel;
    }

    public AxisPanel getAxisPanel() {
        return axisPanel;
    }

    public ReferencePanel getReferencePanel() {
        return referencePanel;
    }

    public ConsensusPanel getConsensusPanel() {
        return consensusPanel;
    }

    public JScrollPane getNamesScrollPane() {
        return namesScrollPane;
    }

    public JScrollPane getReferenceScrollPane() {
        return referenceScrollPane;
    }

    public JScrollPane getAlignmentScrollPane() {
        return alignmentScrollPane;
    }

    public JScrollPane getAxisScrollPane() {
        return axisScrollPane;
    }

    public JLabel getBlastTypeLabel() {
        return blastTypeLabel;
    }

    public boolean isShowReference() {
        return showReference;
    }

    public boolean isShowConsensus() {
        return showConsensus;
    }


    public boolean isShowAsMapping() {
        return showAsMapping;
    }

    public void setShowAsMapping(boolean showAsMapping) {
        connectNamePanel2AlignmentPane(!showAsMapping);
        this.showAsMapping = showAsMapping;
    }

    public void revalidateGrid() {
        namesPanel.revalidateGrid();
        namesScrollPane.revalidate();
        axisPanel.revalidateGrid();
        axisScrollPane.revalidate();
        referencePanel.revalidateGrid();
        referenceScrollPane.revalidate();
        consensusPanel.revalidateGrid();
        consensusScrollPane.revalidate();
        alignmentPanel.revalidateGrid();
        alignmentScrollPane.revalidate();
    }

    /**
     * zoom either or both axes in or out, or reset to default scale
     *
     * @param axis        vertical, horizontal or both
     * @param what        in, out, reset or a number
     * @param centerPoint
     */
    public void zoom(String axis, String what, Point centerPoint) {
        final JScrollPane alignmentScrollPane = getAlignmentScrollPane();
        final AlignmentPanel alignmentPanel = getAlignmentPanel();
        final NamesPanel namesPanel = getNamesPanel();
        final ReferencePanel referencePanel = getReferencePanel();
        final ConsensusPanel consensusPanel = getConsensusPanel();
        final AxisPanel axisPanel = getAxisPanel();

        ScrollPaneAdjuster scrollPaneAdjuster = new ScrollPaneAdjuster(alignmentScrollPane, centerPoint);

        if (axis.equals("horizontal") || axis.equals("both")) {
            if (what.equals("in"))
                hScale *= 1.2;
            else if (what.equals("out"))
                hScale /= 1.2;
            else if (what.equals("reset"))
                hScale = DEFAULT_SCALE;
            else if (what.equals("selection"))
                hScale *= alignmentPanel.getHZoomToSelectionFactor();
            else if (what.equals("fit"))
                hScale *= alignmentPanel.getHZoomToFitFactor();
            else if (Basic.isDouble(what))
                hScale = Math.max(0.0001, Basic.parseDouble(what));
        }

        if (axis.equals("vertical") || axis.equals("both")) {
            if (what.equals("in"))
                vScale *= 1.2;
            else if (what.equals("out"))
                vScale /= 1.2;
            else if (what.equals("reset"))
                vScale = DEFAULT_SCALE;
            else if (what.equals("selection"))
                vScale *= alignmentPanel.getVZoomToSelectionFactor();
            else if (what.equals("fit"))
                vScale *= alignmentPanel.getVZoomToFitFactor();
            else if (Basic.isDouble(what))
                vScale = Math.max(0.0001, Basic.parseDouble(what));
        }

        int maxFontSize = 24;
        if (hScale > maxFontSize)
            hScale = maxFontSize;
        if (vScale > maxFontSize)
            vScale = maxFontSize;

        alignmentPanel.setScale(hScale, vScale);
        namesPanel.setScale(0, vScale);
        axisPanel.setScale(hScale, 0);
        referencePanel.setScale(hScale, 0);
        consensusPanel.setScale(hScale, 0);
        if (isShowReference())
            setShowReference(true); // update size of reference panel
        alignmentScrollPane.revalidate();
        if (!what.equals("selection"))
            scrollPaneAdjuster.adjust(axis.equals("horizontal") || axis.equals("both"), axis.equals("vertical") || axis.equals("both"));
        else {
            Point aPoint = new Point((int) Math.round(alignmentPanel.getX(selectedBlock.getFirstCol())),
                    (int) Math.round(alignmentPanel.getY(selectedBlock.getFirstRow() - 1)));
            alignmentScrollPane.getViewport().setViewPosition(aPoint);
        }
    }

    /**
     * copy selected alignment to clip-board
     */
    public boolean copyAlignment() {
        final SelectedBlock selectedBlock = getSelectedBlock();
        if (selectedBlock.isSelected()) {
            StringSelection ss = new StringSelection(getSelectedAlignment());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            return true;
        }
        return false;
    }

    /**
     * gets the selected alignment as a fastA string
     *
     * @return selected alignment or null
     */
    public String getSelectedAlignment() {
        final SelectedBlock selectedBlock = getSelectedBlock();
        if (selectedBlock.isSelected()) {
            return getAlignment().toFastA(getAlignmentPanel().isShowUnalignedChars(),
                    selectedBlock.getFirstRow(), selectedBlock.getFirstCol(), selectedBlock.getLastRow(), selectedBlock.getLastCol());
        }
        return null;
    }

    /**
     * copy selected consensus to clip-board
     */
    public boolean copyConsensus() {
        final SelectedBlock selectedBlock = getSelectedBlock();
        if (selectedBlock.isSelected()) {
            StringSelection ss = new StringSelection(getSelectedConsensus());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            return true;
        }
        return false;
    }

    /**
     * gets the selected consensus string
     *
     * @return selected consensus or null
     */
    public String getSelectedConsensus() {
        final SelectedBlock selectedBlock = getSelectedBlock();
        if (selectedBlock.isSelected()) {
            return getAlignment().getConsensusString(selectedBlock.getFirstCol(), selectedBlock.getLastCol());
        }
        return null;
    }

    /**
     * copy selected reference to clip-board
     */
    public boolean copyReference() {
        final SelectedBlock selectedBlock = getSelectedBlock();
        if (selectedBlock.isSelected()) {
            StringSelection ss = new StringSelection(getSelectedReference());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            return true;
        }
        return false;
    }

    /**
     * gets the selected reference string
     *
     * @return selected reference or null
     */
    public String getSelectedReference() {
        final SelectedBlock selectedBlock = getSelectedBlock();
        if (selectedBlock.isSelected()) {
            return getAlignment().getReferenceString(selectedBlock.getFirstCol(), selectedBlock.getLastCol());
        }
        return null;
    }

    public void setNamesPanelVisible(boolean namesPanelVisible) {
        if (namesPanelVisible) {
            splitPane.setDividerLocation(100);
        } else {
            splitPane.setDividerLocation(0);
            splitPane.setResizeWeight(0);
        }
    }

    private boolean isNamesPanelVisible() {
        return splitPane.getDividerLocation() > 0;
    }
}
