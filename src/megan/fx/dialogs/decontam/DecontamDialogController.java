/*
 * DecontamDialogController.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.fx.dialogs.decontam;

import javafx.fxml.FXML;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class DecontamDialogController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Tab setupTab;

    @FXML
    private ChoiceBox<String> taxonomicRankCBox;

    @FXML
    private CheckBox projectCheckBox;

    @FXML
    private RadioButton frequencyMethodRadioButton;

    @FXML
    private ChoiceBox<String> concentrationAttributeCBox;

    @FXML
    private RadioButton prevelanceMethodRadioButton;

    @FXML
    private ChoiceBox<String> negativeControlCBox;

    @FXML
    private TextField negativeControlTextField;

    @FXML
    private RadioButton combinedRadioButton;

    @FXML
    private RadioButton minimumRadioButton;

    @FXML
    private RadioButton eitherRadioButton;

    @FXML
    private RadioButton bothRadioButton;

    @FXML
    private CheckBox batchesCheckBox;

    @FXML
    private ChoiceBox<String> batchesCBox;

    @FXML
    private ChoiceBox<?> batchedCombineCBox;

    @FXML
    private TextField thresholdTextField;

    @FXML
    private Tab frequencyTab;

    @FXML
    private ChoiceBox<String> frequencyPlotTaxonCBox;

    @FXML
    private ScatterChart<Double, Double> frequencyScatterPlot;

    @FXML
    private Button addFrequencyTabButton;

    @FXML
    private Tab prevalenceTab;

    @FXML
    private ChoiceBox<String> prevalencePlotTaxonCBox;

    @FXML
    private ScatterChart<Double, Double> prevalenceScatterPlot;

    @FXML
    private Button addPrevalenceTabButton;

    @FXML
    private Tab saveTab;

    @FXML
    private CheckBox saveToFileCheckBox;

    @FXML
    private TextField outputFileTextField;

    @FXML
    private Button outputFileBrowseButton;

    @FXML
    private CheckBox saveReportCheckBox;

    @FXML
    private TextField reportFileTextField;

    @FXML
    private Button reportFileBrowseButton;

    @FXML
    private CheckBox previewCheckBox;

    @FXML
    private Button closeButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button applyButton;

    @FXML
    void initialize() {
        System.err.println("INIT!");

    }

    public Tab getSetupTab() {
        return setupTab;
    }

    public ChoiceBox<String> getTaxonomicRankCBox() {
        return taxonomicRankCBox;
    }

    public CheckBox getProjectCheckBox() {
        return projectCheckBox;
    }

    public RadioButton getFrequencyMethodRadioButton() {
        return frequencyMethodRadioButton;
    }

    public ChoiceBox<String> getConcentrationAttributeCBox() {
        return concentrationAttributeCBox;
    }

    public RadioButton getPrevelanceMethodRadioButton() {
        return prevelanceMethodRadioButton;
    }

    public ChoiceBox<String> getNegativeControlCBox() {
        return negativeControlCBox;
    }

    public TextField getNegativeControlTextField() {
        return negativeControlTextField;
    }

    public RadioButton getCombinedRadioButton() {
        return combinedRadioButton;
    }

    public RadioButton getMinimumRadioButton() {
        return minimumRadioButton;
    }

    public RadioButton getEitherRadioButton() {
        return eitherRadioButton;
    }

    public RadioButton getBothRadioButton() {
        return bothRadioButton;
    }

    public CheckBox getBatchesCheckBox() {
        return batchesCheckBox;
    }

    public ChoiceBox<String> getBatchesCBox() {
        return batchesCBox;
    }

    public ChoiceBox<?> getBatchedCombineCBox() {
        return batchedCombineCBox;
    }

    public TextField getThresholdTextField() {
        return thresholdTextField;
    }

    public Tab getFrequencyTab() {
        return frequencyTab;
    }

    public ChoiceBox<String> getFrequencyPlotTaxonCBox() {
        return frequencyPlotTaxonCBox;
    }

    public ScatterChart<Double, Double> getFrequencyScatterPlot() {
        return frequencyScatterPlot;
    }

    public Button getAddFrequencyTabButton() {
        return addFrequencyTabButton;
    }

    public Tab getPrevalenceTab() {
        return prevalenceTab;
    }

    public ChoiceBox<String> getPrevalencePlotTaxonCBox() {
        return prevalencePlotTaxonCBox;
    }

    public ScatterChart<Double, Double> getPrevalenceScatterPlot() {
        return prevalenceScatterPlot;
    }

    public Button getAddPrevalenceTabButton() {
        return addPrevalenceTabButton;
    }

    public Tab getSaveTab() {
        return saveTab;
    }

    public CheckBox getSaveToFileCheckBox() {
        return saveToFileCheckBox;
    }

    public TextField getOutputFileTextField() {
        return outputFileTextField;
    }

    public Button getOutputFileBrowseButton() {
        return outputFileBrowseButton;
    }

    public CheckBox getSaveReportCheckBox() {
        return saveReportCheckBox;
    }

    public TextField getReportFileTextField() {
        return reportFileTextField;
    }

    public Button getReportFileBrowseButton() {
        return reportFileBrowseButton;
    }

    public CheckBox getPreviewCheckBox() {
        return previewCheckBox;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getApplyButton() {
        return applyButton;
    }
}
