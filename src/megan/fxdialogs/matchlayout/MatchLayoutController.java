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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

/**
 * controller class
 * Created by huson on 2/11/17.
 */


public class MatchLayoutController {

    @FXML
    private Button closeButton;

    @FXML
    private Pane centerPane;

    @FXML
    private TextField queryNameTextField;

    @FXML
    private Pane xAxisPane;

    @FXML
    private Pane yAxisPane;

    @FXML
    private TableView<MatchItem> taxaTableView;


    public Button getCloseButton() {
        return closeButton;
    }

    public Pane getCenterPane() {
        return centerPane;
    }

    public TextField getQueryNameTextField() {
        return queryNameTextField;
    }

    public TableView<MatchItem> getTaxaTableView() {
        return taxaTableView;
    }

    public Pane getXAxisPane() {
        return xAxisPane;
    }

    public Pane getYAxisPane() {
        return yAxisPane;
    }


}

