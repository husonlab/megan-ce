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

package megan.dialogs.reads;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

/**
 * displays the read length distribution
 */
public class ReadLengthDistributionController {

    @FXML
    private BarChart<?, ?> barChart;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button cancelButton;

    @FXML
    private Button closeButton;

    @FXML
    private TextField textField;


    public BarChart<?, ?> getBarChart() {
        return barChart;
    }

    public Button getCloseButton() {
        return closeButton;
    }

    public TextField getTextField() {
        return textField;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public Button getCancelButton() {
        return cancelButton;
    }
}
