/*
 *  Copyright (C) 2015 Daniel H. Huson
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
package megan.fxdialogs.matchlayout;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import jloda.gui.MenuBar;
import jloda.gui.ToolBar;
import jloda.gui.commands.CommandManager;
import jloda.gui.director.IDirectableViewer;
import jloda.gui.director.ProjectManager;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.algorithms.AssignmentUsingLCAForLongReads;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.fx.Utilities;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * match layout viewer
 * Daniel Huson, 2.2017
 */
public class MatchLayoutViewer extends JFrame implements IDirectableViewer {
    private final Director dir;
    private boolean uptoDate = true;
    private boolean locked = false;

    private IReadBlock readBlock;

    private final MenuBar menuBar;

    private final CommandManager commandManager;

    private final SwingPanel4FX<MatchLayoutController> swingPanel4FX;

    private Runnable runOnDestroy;


    /**
     * constructor
     *
     * @param dir
     */
    public MatchLayoutViewer(JFrame parent, final Director dir, final IReadBlock readBlock) {
        this.dir = dir;
        setReadBlock(readBlock);

        commandManager = new CommandManager(dir, this,
                new String[]{"megan.commands", "megan.fxdialogs.matchlayout.commands"}, !ProgramProperties.isUseGUI());

        setTitle();

        setSize(640, 540);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        if (ProgramProperties.getProgramIcon() != null)
            setIconImage(ProgramProperties.getProgramIcon().getImage());

        menuBar = new MenuBar(GUIConfiguration.getMenuConfiguration(), commandManager);
        setJMenuBar(menuBar);

        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(new ToolBar(GUIConfiguration.getToolBarConfiguration(), commandManager), BorderLayout.NORTH);
        getFrame().getContentPane().add(mainPanel);

        swingPanel4FX = new SwingPanel4FX<>(this.getClass());
        swingPanel4FX.runLaterInSwing(new Runnable() {
            @Override
            public void run() {
                mainPanel.add(swingPanel4FX.getPanel(), BorderLayout.CENTER); // add panel once initialization complete
                mainPanel.validate();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        setupControls(swingPanel4FX.getController());
                        setupScene(swingPanel4FX.getController(), getReadBlock());
                        swingPanel4FX.getController().getTaxaTableView().sort();
                        swingPanel4FX.getController().getTaxaTableView().getSortOrder().clear();
                    }
                });
            }
        });

        commandManager.updateEnableState();
        getFrame().setLocationRelativeTo(parent);

        getFrame().setVisible(true);
    }

    public boolean isUptoDate() {
        return uptoDate;
    }

    public JFrame getFrame() {
        return this;
    }

    public void updateView(String what) {
        uptoDate = false;
        setTitle();
        commandManager.updateEnableState();
        uptoDate = true;

        if (what.equals(Director.ALL)) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (swingPanel4FX.getPanel() != null)
                        setupScene(swingPanel4FX.getController(), getReadBlock());
                }
            });
        }
    }

    public void lockUserInput() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        locked = true;
        commandManager.setEnableCritical(false);
    }

    public void unlockUserInput() {
        setCursor(Cursor.getDefaultCursor());
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
        if (runOnDestroy != null)
            runOnDestroy.run();
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        dir.removeViewer(this);
        dispose();
    }

    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * set the title of the window
     */
    public void setTitle() {
        String newTitle = "Alignment Layout - " + dir.getDocument().getTitle();

        /*
        if (dir.getDocument().isDirty())
            newTitle += "*";
          */

        if (dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + dir.getID() + "] - " + ProgramProperties.getProgramVersion();

        if (!getFrame().getTitle().equals(newTitle)) {
            getFrame().setTitle(newTitle);
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
        return "MatchLayoutViewer";
    }

    public IReadBlock getReadBlock() {
        return readBlock;
    }

    public void setReadBlock(IReadBlock readBlock) {
        this.readBlock = readBlock;
    }

    /**
     * setup all the controls
     */
    private void setupControls(MatchLayoutController controller) {
        controller.getCloseButton().setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        dir.execute("close;", commandManager);
                    }
                });
            }
        });
    }

    /**
     * setup the visualization
     *
     * @param controller
     * @param readBlock
     */
    private void setupScene(final MatchLayoutController controller, final IReadBlock readBlock) {
        final Pane centerPane = controller.getCenterPane();
        centerPane.getChildren().clear();
        controller.getTaxaTableView().getItems().clear();
        controller.getTaxaTableView().getColumns().clear();

        if (readBlock == null)
            return;

        final Group drawing = new Group();
        final Group selection = new Group();
        controller.getQueryNameTextField().setText(readBlock.getReadHeader());
        int minBitScore = Integer.MAX_VALUE;
        int maxBitScore = 0;

        for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            minBitScore = (int) Math.round(Math.min(matchBlock.getBitScore(), minBitScore));
            maxBitScore = (int) Math.round(Math.max(matchBlock.getBitScore(), maxBitScore));
        }

        final DoubleProperty widthFactor = new SimpleDoubleProperty();
        widthFactor.bind(centerPane.widthProperty().divide(readBlock.getReadLength()));

        if (minBitScore == maxBitScore) {
            maxBitScore++;
            minBitScore--;
        }

        final DoubleProperty heightFactor = new SimpleDoubleProperty();
        heightFactor.bind(centerPane.heightProperty().subtract(4).divide(maxBitScore - minBitScore));

        final TreeMap<Integer, java.util.List<Line>> taxon2Lines = new TreeMap<>();
        final Map<Integer, float[]> taxon2SumScoreMaxScoreNumberHits = new HashMap<>();

        for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            final int taxId = matchBlock.getTaxonId();

            final javafx.scene.paint.Color color;
            if (taxId <= 0) {
                color = javafx.scene.paint.Color.DARKGRAY;
            } else {
                String taxonName = ClassificationManager.get(Classification.Taxonomy, false).getName2IdMap().get(taxId);
                color = Utilities.getColorFX(dir.getDocument().getChartColorManager().getClassColor(taxonName));
            }

            float[] values = taxon2SumScoreMaxScoreNumberHits.get(taxId);
            if (values == null) {
                taxon2SumScoreMaxScoreNumberHits.put(taxId, new float[]{matchBlock.getBitScore(), matchBlock.getBitScore(), 1});
            } else {
                values[0] += matchBlock.getBitScore();
                values[1] = Math.max(values[1], matchBlock.getBitScore());
                values[2]++;
            }

            final Line line = new Line();
            line.startXProperty().bind(widthFactor.multiply(matchBlock.getAlignedQueryStart()));
            line.endXProperty().bind(widthFactor.multiply(matchBlock.getAlignedQueryEnd()));
            line.startYProperty().bind(heightFactor.multiply(maxBitScore - matchBlock.getBitScore()).add(2));
            line.endYProperty().bind(heightFactor.multiply(maxBitScore - matchBlock.getBitScore()).add(2));
            final Tooltip tooltip = new Tooltip(matchBlock.getText());
            tooltip.setFont(new javafx.scene.text.Font("Courier", 11));
            Tooltip.install(line, tooltip);

            line.setStroke(color);
            line.setStrokeWidth(8);
            line.setStrokeLineCap(StrokeLineCap.ROUND);

            line.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    System.err.println(tooltip.getText());
                    for (MatchItem item : controller.getTaxaTableView().getItems()) {
                        if (taxId == item.getTaxonId()) {
                            controller.getTaxaTableView().getSelectionModel().select(item);
                            break;
                        }
                    }
                }
            });

            java.util.List<Line> lines = taxon2Lines.get(taxId);
            if (lines == null) {
                lines = new ArrayList<>();
                taxon2Lines.put(taxId, lines);
            }
            lines.add(line);

            drawing.getChildren().add(line);
        }

        // setup table:
        final TableColumn<MatchItem, String> taxonCol = new TableColumn<>("Taxon");
        taxonCol.setCellValueFactory(new PropertyValueFactory<MatchItem, String>("taxonName"));
        controller.getTaxaTableView().getColumns().add(taxonCol);
        final TableColumn<MatchItem, Integer> hitsCol = new TableColumn<>("#Hits");
        hitsCol.setCellValueFactory(new PropertyValueFactory<MatchItem, Integer>("hits"));
        controller.getTaxaTableView().getColumns().add(hitsCol);
        final TableColumn<MatchItem, Float> disjointScoreCol = new TableColumn<>("Disjoint score");
        disjointScoreCol.setCellValueFactory(new PropertyValueFactory<MatchItem, Float>("disjointBitScore"));
        controller.getTaxaTableView().getColumns().add(disjointScoreCol);
        final TableColumn<MatchItem, Float> totalScoreCol = new TableColumn<>("Total score");
        totalScoreCol.setCellValueFactory(new PropertyValueFactory<MatchItem, Float>("totalBitScore"));
        controller.getTaxaTableView().getColumns().add(totalScoreCol);
        final TableColumn<MatchItem, Float> maxScoreCol = new TableColumn<>("Max score");
        maxScoreCol.setCellValueFactory(new PropertyValueFactory<MatchItem, Float>("maxBitScore"));
        controller.getTaxaTableView().getColumns().add(maxScoreCol);

        // compute the disjoint score
        AssignmentUsingLCAForLongReads assignmentUsingLCAForLongReads = new AssignmentUsingLCAForLongReads(Classification.Taxonomy, false, 100);
        final Map<Integer, Float> taxonId2disjointScore = assignmentUsingLCAForLongReads.computeTotalDisjointScore(null, readBlock);

        for (Integer taxId : taxon2Lines.keySet()) {
            String taxonName = ClassificationManager.get(Classification.Taxonomy, false).getName2IdMap().get(taxId);
            float[] values = taxon2SumScoreMaxScoreNumberHits.get(taxId);
            controller.getTaxaTableView().getItems().add(new MatchItem(taxId, taxonName, values[0], taxonId2disjointScore.get(taxId), values[1], (int) values[2]));
            controller.getTaxaTableView().getSelectionModel().selectedItemProperty().addListener(new ChangeListener<MatchItem>() {
                @Override
                public void changed(ObservableValue<? extends MatchItem> observable, MatchItem oldValue, MatchItem newValue) {
                    selection.getChildren().clear();
                    if (newValue != null) {
                        java.util.List<Line> list = taxon2Lines.get(newValue.getTaxonId());
                        if (list != null && list.size() > 0) {
                            for (Line line : list) {
                                final javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle(line.getStartX() - 5, line.getStartY() - 5, line.getEndX() - line.getStartX() + 10, 10);
                                rectangle.setFill(javafx.scene.paint.Color.TRANSPARENT);
                                rectangle.setStroke(javafx.scene.paint.Color.RED);
                                rectangle.xProperty().bind(line.startXProperty().subtract(5));
                                rectangle.widthProperty().bind(line.endXProperty().subtract(line.startXProperty()).add(10));
                                rectangle.yProperty().bind(line.startYProperty().subtract(5));
                                rectangle.heightProperty().set(10);
                                selection.getChildren().add(rectangle);
                            }
                        }
                    }
                }
            });
        }

        final NumberAxis queryAxis = new NumberAxis();
        queryAxis.setSide(Side.TOP);
        queryAxis.setAutoRanging(false);
        queryAxis.setLowerBound(0);
        int length = readBlock.getReadLength();
        queryAxis.setUpperBound(length);
        queryAxis.prefWidthProperty().bind(controller.getXAxisPane().widthProperty());
        queryAxis.prefHeightProperty().bind(controller.getXAxisPane().heightProperty());
        controller.getXAxisPane().getChildren().add(queryAxis);

        for (int i = 10; true; i *= 10) {
            if (length < 20 * i) {
                queryAxis.setTickUnit(i);
                break;
            }
        }
        Tooltip.install(queryAxis, new Tooltip("Query position"));


        final NumberAxis bitScoreAxis = new NumberAxis();
        bitScoreAxis.setSide(Side.LEFT);
        bitScoreAxis.setAutoRanging(false);
        bitScoreAxis.setLowerBound(5 * (minBitScore / 5)); // round to lower 5
        bitScoreAxis.setTickUnit(10);
        bitScoreAxis.setMinorTickCount(4);
        bitScoreAxis.setUpperBound(5 * (maxBitScore / 5));
        bitScoreAxis.prefWidthProperty().bind(controller.getYAxisPane().widthProperty());
        bitScoreAxis.prefHeightProperty().bind(controller.getYAxisPane().heightProperty());
        controller.getYAxisPane().getChildren().add(bitScoreAxis);
        Tooltip.install(bitScoreAxis, new Tooltip("Bit Score"));

        controller.getTaxaTableView().getSortOrder().add(taxonCol);

        controller.getCenterPane().getChildren().addAll(drawing, selection);
    }

    public void runOnDestroy(Runnable runnable) {
        this.runOnDestroy = runnable;
    }
}
