/*
 *  TestMyTableView.java Copyright (C) 2019 Daniel H. Huson
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
package megan.xtra;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jloda.fx.control.table.MyTableView;
import jloda.fx.control.table.MyTableViewSearcher;
import jloda.fx.find.FindToolBar;
import jloda.fx.util.ResourceManagerFX;
import jloda.util.Basic;

import java.util.ArrayList;
import java.util.Arrays;

public class TestMyTableView extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        final BorderPane root = new BorderPane();


        final MyTableView tableView = new MyTableView();
        tableView.setAllowDeleteCol(true);
        tableView.setAllowAddCol(true);
        tableView.setAllowRenameCol(true);
        tableView.setAllAddRow(true);
        tableView.setAllowDeleteRow(true);
        tableView.setAllowRenameRow(true);

        root.setCenter(tableView);

        final MyTableViewSearcher searcher = new MyTableViewSearcher(tableView);
        final FindToolBar findToolBar = new FindToolBar(searcher);
        root.setTop(findToolBar);


        tableView.setAdditionColHeaderMenuItems((col) -> Arrays.asList(new MenuItem("Color samples by attribute '" + col + "'")));

        tableView.setAdditionRowHeaderMenuItems((rows) -> {
            final MenuItem moveSamplesUp = new MenuItem("Move Samples Up");
            moveSamplesUp.setOnAction((e) ->
            {
                final ArrayList<String> list = new ArrayList<>(tableView.getSelectedRows());
                System.err.println("Moving up: " + Basic.toString(list, " "));

                for (String sample : list) {
                    final int oldPos = tableView.getRowIndex(sample);
                    final int newPos = oldPos - 1;

                    System.err.println(sample + ": " + oldPos + " -> " + newPos);

                    if (newPos >= 0 && newPos < tableView.getRowCount())
                        tableView.swapRows(oldPos, newPos);
                }
            });
            final MenuItem moveSamplesDown = new MenuItem("Move Samples Down");
            moveSamplesDown.setOnAction((e) ->
            {
                final ArrayList<String> list = Basic.reverse(tableView.getSelectedRows());
                System.err.println("Moving down:" + Basic.toString(list, " "));
                for (String sample : list) {
                    final int oldPos = tableView.getRowIndex(sample);
                    final int newPos = oldPos + 1;

                    System.err.println(sample + ": " + oldPos + " -> " + newPos);

                    if (newPos >= 0 && newPos < tableView.getRowCount())
                        tableView.swapRows(oldPos, newPos);
                }
            });

            return Arrays.asList(moveSamplesUp, moveSamplesDown);
        });

        final ToggleButton findButton = new ToggleButton("Find");
        findButton.selectedProperty().addListener((c, o, n) -> findToolBar.setShowFindToolBar(n));

        final ToggleButton editableButton = new ToggleButton("Editable");
        tableView.editableProperty().bind(editableButton.selectedProperty());

        final Button scrollToTopButton = new Button("Scroll to top");
        scrollToTopButton.setOnAction((e) -> tableView.scrollToRow(0));

        Button copyButton = new Button("Copy");
        copyButton.setOnAction((e) -> tableView.copyToClipboard());
        copyButton.disableProperty().bind(Bindings.isEmpty(tableView.getSelectedCells()));

        Button listButton = new Button("List");
        listButton.setOnAction((e) -> System.err.println(tableView));


        final Label updateLabel = new Label();
        updateLabel.textProperty().bind(tableView.updateProperty().asString());

        root.setBottom(new ToolBar(findButton, editableButton, copyButton, scrollToTopButton, listButton, updateLabel));

        final Label selectedRowCount = new Label();
        selectedRowCount.textProperty().bind(Bindings.concat("Selected rows: ", tableView.countSelectedRowsProperty().asString(), " "));

        final Label selectedColCount = new Label();
        selectedColCount.textProperty().bind(Bindings.concat("Selected cols: ", tableView.countSelectedColsProperty().asString(), " "));

        root.setRight(new VBox(selectedRowCount, selectedColCount));


        tableView.addCol("name");
        tableView.addCol("age");
        tableView.addCol("height");

        for (int i = 0; i < 3; i++) {
            tableView.addRow("first-first" + i, "cindy", 5, 5.0);
            tableView.addRow("second" + i, "bob", 10, 25.0);
            tableView.addRow("third" + i, "alice", 15, 125.0);
            tableView.addRow("four" + i, "elke", 15, 125.0);
            tableView.addRow("five" + i, "joe", 15, 125.0);
            tableView.addRow("sixz" + i, "dave", 150, 225.0);
        }

        final ImageView imageView = new ImageView(ResourceManagerFX.getIcon("Algorithm16.gif"));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(16);
        root.setLeft(new VBox(imageView));

        Scene scene = new Scene(root, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("my table");
        primaryStage.sizeToScene();
        primaryStage.show();
    }
}
