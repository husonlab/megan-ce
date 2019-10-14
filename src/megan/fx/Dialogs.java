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

package megan.fx;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import jloda.util.Basic;
import jloda.util.Single;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/**
 * shows a confirmation dialog in either swing thread or fx thread
 */
public class Dialogs {
    /**
     * show a confirmation dialog
     *
     * @param swingParent
     * @param title
     * @param message
     * @return
     */
    public static boolean showConfirmation(final Component swingParent, final String title, final String message) {
        final Single<Boolean> confirmed = new Single<>(false);

        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setContentText(message);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && (result.get() == ButtonType.OK))
                confirmed.set(true);
        } else if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            int result = JOptionPane.showConfirmDialog(swingParent, message);
            if (result == JOptionPane.YES_OPTION)
                confirmed.set(true);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> confirmed.set(showConfirmation(swingParent, title, message)));
            } catch (Exception e) {
                Basic.caught(e);
            }
        }
        return confirmed.get();
    }

    /**
     * show an input dialog
     *
     * @param swingParent
     * @param title
     * @param message
     * @param initialValue
     * @return
     */
    private static String showInput(final Component swingParent, final String title, final String message, final String initialValue) {
        final Single<String> input = new Single<>(null);

        if (Platform.isFxApplicationThread()) {
            final TextInputDialog dialog = new TextInputDialog(initialValue != null ? initialValue : "");
            dialog.setTitle(title);
            dialog.setHeaderText(message);

            final Optional<String> result = dialog.showAndWait();
            result.ifPresent(input::set);

        } else if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            input.set(JOptionPane.showInputDialog(swingParent, message, initialValue));
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> input.set(showInput(swingParent, title, message, initialValue)));
            } catch (Exception e) {
                Basic.caught(e);
            }
        }
        return input.get();
    }
}

