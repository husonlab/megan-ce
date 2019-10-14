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
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import jloda.fx.util.ExtendedFXMLLoader;
import jloda.util.Single;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A JavaFX panel that can be used in swing and is setup from an fxml file
 * Daniel Huson, 2.2017
 */
public class SwingPanel4FX<C> {
    private final Class viewerClass;
    private JFXPanel jFXPanel;
    private final Single<Boolean> initialized = new Single<>(false);
    private C controller;
    private final List<Runnable> toRunLaterInSwing = new ArrayList<>();

    /**
     * constructor
     *
     * @param viewerClass there must exist a file with the same name and path with suffix .fxml
     */
    public SwingPanel4FX(final Class viewerClass) {
        this.viewerClass = viewerClass;

        if (SwingUtilities.isEventDispatchThread())
            initSwingLater();
        else {
            SwingUtilities.invokeLater(() -> initSwingLater());
        }
    }

    /**
     * get the panel
     *
     * @return panel
     */
    public JFXPanel getPanel() {
        return jFXPanel;
    }

    /**
     * initialize swing
     */
    private void initSwingLater() {
        jFXPanel = new JFXPanel();
        Platform.runLater(() -> initFxLater());
    }

    /**
     * initialize JavaFX
     */
    private void initFxLater() {
        synchronized (initialized) {
            if (!initialized.get()) {
                try {
                    final ExtendedFXMLLoader<C> extendedFXMLLoader = new ExtendedFXMLLoader<>(viewerClass);
                    controller = extendedFXMLLoader.getController();
                    jFXPanel.setScene(new Scene(extendedFXMLLoader.getRoot()));
                } finally {
                    initialized.set(true);
                    for (Runnable runnable : toRunLaterInSwing) {
                        SwingUtilities.invokeLater(runnable);
                    }
                }
            }
        }
    }

    /**
     * schedule to be run in swing thread once initialization is complete
     *
     * @param runnable
     */
    public void runLaterInSwing(Runnable runnable) {
        synchronized (initialized) {
            if (!initialized.get()) {
                toRunLaterInSwing.add(runnable);
            } else
                SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * gets the controller
     *
     * @return controller
     */
    public C getController() {
        return controller;
    }
}

