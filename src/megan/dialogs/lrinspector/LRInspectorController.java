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

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import jloda.swing.find.CompositeObjectSearcher;
import jloda.swing.find.IObjectSearcher;
import jloda.swing.util.ToolBar;
import jloda.util.ProgramProperties;
import jloda.util.Statistics;
import megan.chart.ChartColorManager;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static megan.dialogs.lrinspector.ReadLayoutPane.DEFAULT_LABELED_HEIGHT;

/**
 * controller class for visual read inspector
 * Created by huson on 2/21/17.
 */
public class LRInspectorController {
    private final TableItemService tableItemService = new TableItemService();
    private final SelectionGroup classificationLayoutGroup = new SelectionGroup();

    private LRInspectorViewer viewer;
    private String[] cNames;
    private ChartColorManager colorManager;
    private boolean usingHeatmap;

    @FXML
    private Button closeButton;

    @FXML
    private MenuButton layoutMenuButton;

    @FXML
    private MenuButton tableMenuButton;

    @FXML
    private TextField headerField;

    @FXML
    private CheckMenuItem overviewMenuItem;

    @FXML
    private MenuItem detailsMenuItem;

    @FXML
    private TableView<TableItem> tableView;

    @FXML
    private TableColumn<TableItem, String> readNameCol;

    @FXML
    private TableColumn<TableItem, Integer> readLengthCol;

    @FXML
    private TableColumn<TableItem, String> assignmentCol;

    @FXML
    private TableColumn<TableItem, Float> coverageCol;

    @FXML
    private TableColumn<TableItem, Integer> hitsCol;

    @FXML
    private TableColumn<TableItem, Node> layoutCol;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button cancelButton;

    @FXML
    private Slider panelHeightSlider;

    @FXML
    private Slider panelWidthSlider;

    @FXML
    private ChoiceBox<Integer> fontSize;

    /**
     * setup the controls
     *
     * @param viewer
     * @param toolBar
     */
    public void setupControls(final LRInspectorViewer viewer, ToolBar toolBar) throws IOException {
        this.viewer = viewer;
        colorManager = viewer.getDir().getDocument().getChartColorManager();

        cNames = viewer.getDir().getDocument().getConnector().getAllClassificationNames();

        closeButton.setOnAction(event -> SwingUtilities.invokeLater(() -> viewer.getDir().executeImmediately("close;", viewer.getCommandManager())));

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        progressBar.visibleProperty().bind(tableItemService.runningProperty());
        progressBar.progressProperty().bind(tableItemService.progressProperty());
        cancelButton.visibleProperty().bind(tableItemService.runningProperty());
        cancelButton.setOnAction(event -> tableItemService.cancel());

        tableItemService.setOnRunning(event -> headerField.textProperty().bind(tableItemService.messageProperty()));
        tableItemService.setOnCancelled(event -> {
            headerField.textProperty().unbind();
            headerField.setText(viewer.getClassIdDisplayName() + ": " + computeReadStats() + " (may be incomplete)");
            recolor();
            setupSearcher();
            viewer.setUptoDate(true);
        });
        tableItemService.setOnSucceeded(event -> {
            headerField.textProperty().unbind();
            headerField.setText(viewer.getClassIdDisplayName() + ": " + computeReadStats());
            tableView.sort();
            recolor();
            setupSearcher();
            viewer.setUptoDate(true);
        });
        tableItemService.setOnFailed(event -> {
            headerField.textProperty().unbind();
            headerField.setText("Failed to load reads");
            if (tableItemService.getException() != null)
                System.err.println("Exception: " + tableItemService.getException());
            recolor();
            setupSearcher();
            viewer.setUptoDate(true);
        });

        readNameCol.setContextMenu(createTableContextMenu());

        readNameCol.setCellValueFactory(new PropertyValueFactory<>("readName"));
        readLengthCol.setCellValueFactory(new PropertyValueFactory<>("readLength"));
        readLengthCol.setSortType(TableColumn.SortType.DESCENDING);

        assignmentCol.setCellValueFactory(new PropertyValueFactory<>("className"));
        coverageCol.setCellValueFactory(new PropertyValueFactory<>("percentCoverage"));
        coverageCol.setSortType(TableColumn.SortType.DESCENDING);
        hitsCol.setCellValueFactory(new PropertyValueFactory<>("hits"));
        hitsCol.setSortType(TableColumn.SortType.DESCENDING);
        layoutCol.setCellValueFactory(new PropertyValueFactory<>("pane"));

        // setup buttons that control column visibility
        for (final TableColumn col : tableView.getColumns()) {
            CheckMenuItem checkMenuItem = new CheckMenuItem("Show " + col.getText());
            if (!ProgramProperties.get(col.getText() + "ColVisible", true))
                col.setVisible(false);
            checkMenuItem.selectedProperty().bindBidirectional(col.visibleProperty());
            col.visibleProperty().addListener((observable, oldValue, newValue) -> ProgramProperties.put(col.getText() + "ColVisible", newValue));
            tableMenuButton.getItems().add(checkMenuItem);
        }

        tableView.widthProperty().addListener((observable, oldValue, newValue) -> {
            double newLayoutTableColumnWidth = newValue.doubleValue() - 20;
            for (TableColumn col : tableView.getColumns()) {
                if (col != layoutCol && col.isVisible())
                    newLayoutTableColumnWidth -= (col.getWidth() + 5);
            }
            if (newLayoutTableColumnWidth > layoutCol.getWidth())
                layoutCol.setPrefWidth(newLayoutTableColumnWidth);
        });

        layoutCol.setGraphic(createAxis(viewer.maxReadLengthProperty(), layoutCol.widthProperty()));

        overviewMenuItem.setOnAction(event -> {
            if (!overviewMenuItem.isSelected()) {
                overviewMenuItem.setSelected(true);
            }
            recolor();
        });
        overviewMenuItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                classificationLayoutGroup.selectAll(false);
        });

        detailsMenuItem.setOnAction(event -> {
            classificationLayoutGroup.selectAll(true);
            if (overviewMenuItem.isSelected())
                overviewMenuItem.setSelected(false);
            recolor();
        });

        {
            final RadioMenuItem[] classificationRadioMenuItems = new RadioMenuItem[cNames.length];

            if (cNames.length > 0) {
                layoutMenuButton.getItems().add(new SeparatorMenuItem());
                for (int cid = 0; cid < cNames.length; cid++) {
                    final String cName = cNames[cid];
                    final RadioMenuItem menuItem = new RadioMenuItem(cName);
                    menuItem.setOnAction(event -> {
                        overviewMenuItem.setSelected(!classificationLayoutGroup.isSelected());
                        recolor();
                    });
                    classificationRadioMenuItems[cid] = menuItem;
                    classificationLayoutGroup.getItems().add(menuItem);
                    layoutMenuButton.getItems().add(menuItem);
                }
            }

            tableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TableItem>) c -> {
                if (c.next() && (c.wasAdded() || c.wasRemoved())) {
                    for (int cid = 0; cid < cNames.length; cid++) {
                        String cName = cNames[cid];
                        boolean allVisible = true;
                        for (TableItem tableItem : tableView.getSelectionModel().getSelectedItems()) {
                            if (tableItem != null && tableItem.getPane() != null && !tableItem.getPane().isLabelsShowing(cName)) {
                                allVisible = false;
                                break;
                            }
                        }
                        classificationRadioMenuItems[cid].setSelected(allVisible);
                    }
                    viewer.updateEnableState();
                }
            });

            tableView.setOnKeyPressed(event -> {
                // this is here to prevent selection using Control-A
                if (event.getText().equals("a") && event.isShortcutDown() && !event.isShiftDown())
                    event.consume();
            });
        }

        overviewMenuItem.setSelected(true);

        tableView.getItems().addListener((ListChangeListener<TableItem>) c -> {
            while (c.next()) {
                if (c.wasPermutated()) {
                    setupSearcher();
                    break;
                }
            }
        });

        tableView.getSortOrder().add(readLengthCol);

        panelHeightSlider.setTooltip(new Tooltip("Change layout height"));
        panelHeightSlider.setValue(DEFAULT_LABELED_HEIGHT); // default value
        panelHeightSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (tableView.getSelectionModel().getSelectedItems().size() > 0) {
                for (TableItem tableItem : tableView.getSelectionModel().getSelectedItems()) {
                    ReadLayoutPane pane = tableItem.getPane();
                    pane.setPreferredHeightLabeled(newValue.intValue());
                }
            } else {
                for (TableItem tableItem : tableView.getItems()) {
                    ReadLayoutPane pane = tableItem.getPane();
                    pane.setPreferredHeightLabeled(newValue.intValue());
                }
            }
        });
        panelHeightSlider.disableProperty().bind(overviewMenuItem.selectedProperty());

        panelWidthSlider.setTooltip(new Tooltip("Change layout width"));
        panelWidthSlider.valueProperty().bindBidirectional(layoutCol.prefWidthProperty());
        panelWidthSlider.disableProperty().bind(getService().runningProperty());
        panelWidthSlider.maxProperty().bind(Bindings.min(1000000, viewer.maxReadLengthProperty().multiply(10)));

        fontSize.getItems().addAll(6, 8, 10, 12, 16, 18, 22, 24);
        fontSize.getSelectionModel().select((Integer) ProgramProperties.get("LongReadLabelFontSize", 10));
        fontSize.disableProperty().bind(overviewMenuItem.selectedProperty());
        fontSize.setOnAction(event -> {
            ReadLayoutPane.setFontSize(fontSize.getSelectionModel().getSelectedItem());
            for (TableItem tableItem : tableView.getItems()) {
                final ReadLayoutPane pane = tableItem.getPane();
                pane.layoutLabels();
            }
        });
    }

    private String computeReadStats() {
        ArrayList<Integer> lengths = new ArrayList<>(tableView.getItems().size());
        for (TableItem tableItem : tableView.getItems()) {
            lengths.add(tableItem.getReadLength());
        }
        Statistics statistics = new Statistics(lengths);
        return "Reads: " + statistics.getCount() + " mean: " + Math.round(statistics.getMean()) + " std: " + Math.round(statistics.getStdDev());

    }

    /**
     * update the scene
     *
     * @param viewer
     */
    public void updateScene(LRInspectorViewer viewer) {
        tableView.getItems().clear();

        tableItemService.configure(viewer.getDir().getDocument(), cNames, viewer.getClassificationName(), viewer.getClassificationIds(), viewer.maxReadLengthProperty(), tableView, layoutCol.widthProperty());
        tableItemService.restart();
    }

    /**
     * create a horizontal axis
     *
     * @param maxReadLength
     * @return axis
     */
    private static Pane createAxis(final ReadOnlyIntegerProperty maxReadLength, final ReadOnlyDoubleProperty widthProperty) {
        final Pane pane = new Pane();
        pane.prefWidthProperty().bind(widthProperty);

        final NumberAxis axis = new NumberAxis();
        axis.setSide(Side.TOP);
        axis.setAutoRanging(false);
        axis.setLowerBound(0);
        axis.prefHeightProperty().set(20);
        axis.prefWidthProperty().bind(widthProperty.subtract(60));
        axis.setTickLabelFont(Font.font("Arial", 10));

        final ChangeListener<Number> changeListener = (observable, oldValue, newValue) -> {
            int minX = Math.round(maxReadLength.get() / 2000.0f); // at most 2000 major ticks
            for (int x = 10; x < 10000000; x *= 10) {
                if (x >= minX && widthProperty.doubleValue() * x >= 50 * maxReadLength.doubleValue()) {
                    axis.setUpperBound(maxReadLength.get());
                    axis.setTickUnit(x);
                    return;
                }
            }
            axis.setTickUnit(maxReadLength.get());
            axis.setUpperBound(maxReadLength.get());
        };

        maxReadLength.addListener(changeListener);
        widthProperty.addListener(changeListener);

        pane.getChildren().add(axis);
        return pane;
    }

    public boolean isUsingHeatmap() {
        return usingHeatmap;
    }


    public TableItemService getService() {
        return tableItemService;
    }

    /**
     * recolor genes based on current layout selection
     */
    public void recolor() {
        if (overviewMenuItem.isSelected()) {
            usingHeatmap = true;
            for (final TableItem item : getSelectedOrAllTableItems()) {
                javafx.application.Platform.runLater(() -> item.getPane().colorByNormalizedBitScore(colorManager, getService().maxNormalizedBitScoreProperty().get()));
            }
            javafx.application.Platform.runLater(() -> updateSearcher());
        } else {
            usingHeatmap = false;
            final ArrayList<String> selectedCNames = new ArrayList<>();
            for (Toggle item : classificationLayoutGroup.getSelectedItems()) {
                if (item instanceof RadioMenuItem) {
                    selectedCNames.add(((RadioMenuItem) item).getText());
                }
            }
            for (final TableItem item : getSelectedOrAllTableItems()) {
                javafx.application.Platform.runLater(() -> item.getPane().colorByClassification(colorManager, selectedCNames, viewer.getClassificationName(), viewer.getClassId(), viewer.getDir().getDocument().getChartColorManager().isColorByPosition()));
            }
            javafx.application.Platform.runLater(() -> updateSearcher());
        }
    }

    /**
     * get selected table items, if any, otherwise, get all
     *
     * @return table items
     */
    private ObservableList<TableItem> getSelectedOrAllTableItems() {
        ObservableList<TableItem> items = tableView.getSelectionModel().getSelectedItems();
        if (items.size() == 0)
            items = tableView.getItems();
        return items;
    }

    private ContextMenu createTableContextMenu() {
        return new ContextMenu();
    }

    /**
     * setup the searcher
     */
    private void setupSearcher() {
        final List<IObjectSearcher> list = new ArrayList<>();
        for (final TableItem item : tableView.getItems()) {
            list.add(item.getPane().getSearcher().updateLists());
        }
        viewer.getSearcher().setSearchers(list.toArray(new IObjectSearcher[0]));
    }

    /**
     * update the searcher, e.g. after change of label
     */
    private void updateSearcher() {
        if (viewer != null) {
            final CompositeObjectSearcher tableSearcher = viewer.getSearcher();
            if (tableSearcher != null) {
                for (IObjectSearcher searcher : tableSearcher.getSearchers()) {
                    if (searcher instanceof ReadLayoutPaneSearcher)
                        ((ReadLayoutPaneSearcher) searcher).updateLists();
                }
            }
        }
    }


    public TableView<TableItem> getTableView() {
        return tableView;
    }
}
