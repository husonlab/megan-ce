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

package megan.blastclient;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * simple Blast GUI
 * Created by huson on 1/18/17.
 */
public class BlastProgram extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        final TextField queryField = new TextField();
        queryField.setFont(Font.font("Courier New"));
        queryField.setPromptText("Query");
        queryField.setPrefColumnCount(80);
        final ChoiceBox<String> programChoice = new ChoiceBox<>();
        programChoice.setMinWidth(100);
        final ChoiceBox<String> databaseChoice = new ChoiceBox<>();
        databaseChoice.setMinWidth(100);

        programChoice.setOnAction(e -> {
            databaseChoice.getItems().setAll(RemoteBlastClient.getDatabaseNames(
                    RemoteBlastClient.BlastProgram.valueOf(programChoice.getValue())));
            databaseChoice.getSelectionModel().select(databaseChoice.getItems().get(0));
        });
        for (RemoteBlastClient.BlastProgram program : RemoteBlastClient.BlastProgram.values()) {
            programChoice.getItems().add(program.toString());
        }
        programChoice.getSelectionModel().select(RemoteBlastClient.BlastProgram.blastx.toString());
        final HBox topPane = new HBox();
        topPane.setSpacing(5);
        topPane.getChildren().addAll(new VBox(new Label("Query:"), queryField),
                new VBox(new Label("Program:"), programChoice), new VBox(new Label("Database:"), databaseChoice));

        final TextArea alignmentArea = new TextArea();
        alignmentArea.setFont(Font.font("Courier New"));
        alignmentArea.setPromptText("Enter a query and then press Apply to compute alignments");
        alignmentArea.setEditable(false);

        final ProgressBar progressBar = new ProgressBar();
        progressBar.setVisible(false);
        final Button loadExampleButton = new Button("Load Example");
        loadExampleButton.setOnAction(e -> queryField.setText("TAATTAGCCATAAAGAAAAACTGGCGCTGGAGAAAGATATTCTCTGGAGCGTCGGGCGAGCGATAATTCAGCTGATTATTGTCGGCTATGTGCTGAAGTAT"));
        loadExampleButton.disableProperty().bind(queryField.textProperty().isNotEmpty());

        final Button cancelButton = new Button("Cancel");
        final Button applyButton = new Button("Apply");
        final ButtonBar bottomBar = new ButtonBar();
        bottomBar.getButtons().addAll(progressBar, loadExampleButton, new Label("        "), cancelButton, applyButton);

        final BorderPane root = new BorderPane();
        root.setTop(topPane);
        root.setCenter(alignmentArea);
        root.setBottom(bottomBar);
        root.setPadding(new Insets(5, 5, 5, 5));

        final BlastService blastService = new BlastService();
        blastService.messageProperty().addListener((observable, oldValue, newValue) -> alignmentArea.setText(alignmentArea.getText() + newValue + "\n"));
        blastService.setOnScheduled(e -> {
            progressBar.setVisible(true);
            progressBar.progressProperty().bind(blastService.progressProperty());
            alignmentArea.clear();
        });
        blastService.setOnSucceeded(e -> {
            if (blastService.getValue().length() > 0)
                alignmentArea.setText(blastService.getValue());
            else
                alignmentArea.setText(alignmentArea.getText() + "*** No hits found ***\n");
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
        });
        blastService.setOnCancelled(e -> {
            progressBar.progressProperty().unbind();
            alignmentArea.setText(alignmentArea.getText() + "*** Canceled ***\n");
            progressBar.setVisible(false);
        });
        blastService.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            alignmentArea.setText(alignmentArea.getText() + "*** Failed ***\n");
            progressBar.setVisible(false);
        });

        queryField.disableProperty().bind(blastService.runningProperty());
        cancelButton.setOnAction(e -> blastService.cancel());
        cancelButton.disableProperty().bind(blastService.runningProperty().not());

        applyButton.setOnAction(e -> {
            blastService.setProgram(RemoteBlastClient.BlastProgram.valueOf(programChoice.getValue()));
            blastService.setDatabase(databaseChoice.getValue());
            blastService.setQuery("read", queryField.getText());
            blastService.restart();
        });
        applyButton.disableProperty().bind(queryField.textProperty().isEmpty().or(blastService.runningProperty()));

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NCBI Blast Client");
        primaryStage.show();
    }
}
