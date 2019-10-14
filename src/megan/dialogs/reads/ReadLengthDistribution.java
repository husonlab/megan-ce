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

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import jloda.fx.util.ExtendedFXMLLoader;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Statistics;
import megan.data.IReadBlockIterator;
import megan.main.Version;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * displays the read length distribution for a node
 * Daniel Huson, 8.2018
 */
public class ReadLengthDistribution {
    private final JFrame frame;
    private final ReadLengthDistributionController controller;

    public ReadLengthDistribution(final ClassificationViewer viewer, final int classId) throws IOException {
        frame = new JFrame("Read length distribution - " + Version.SHORT_DESCRIPTION);

        frame.setLocationRelativeTo(viewer.getFrame());
        frame.setSize(800, 800);

        {
            final ExtendedFXMLLoader<ReadLengthDistributionController> extendedFXMLLoader = new ExtendedFXMLLoader<>(this.getClass());
            final JFXPanel panel = new JFXPanel();
            controller = extendedFXMLLoader.getController();
            panel.setScene(new Scene(extendedFXMLLoader.getRoot()));
            frame.setContentPane(panel);
        }

        controller.getTextField().setText("Read length distribution for: " + viewer.getClassification().getName2IdMap().get(classId) + " (" + classId + "):");

        final Service<Boolean> service = new Service<>() {
            @Override
            protected Task<Boolean> createTask() {
                return new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {

                        final Collection<Integer> classIds;
                        if (viewer.getANode(classId).getOutDegree() > 0)
                            classIds = Collections.singletonList(classId);
                        else
                            classIds = viewer.getClassification().getFullTree().getAllDescendants(classId);

                        final ArrayList<Integer> lengths = new ArrayList<>();
                        try (IReadBlockIterator it = viewer.getDocument().getConnector().getReadsIteratorForListOfClassIds(viewer.getClassification().getName(), classIds, 0, 10, true, false)) {
                            while (it.hasNext()) {
                                lengths.add(it.next().getReadLength());
                                updateProgress(it.getProgress(), it.getMaximumProgress());
                                if (isCancelled())
                                    return false;
                            }
                        }
                        final int numberOfBins = (Math.min(10, lengths.size()));

                        final Statistics statistics = new Statistics(lengths);
                        final int min = (int) statistics.getMin();
                        final int[] binnedCounts = Statistics.getBinnedCounts(lengths, min, (int) statistics.getMax(), numberOfBins);

                        final BarChart barChart = controller.getBarChart();

                        final XYChart.Series<String, Integer> series = new XYChart.Series<>();
                        series.setName("Read lengths");
                        System.err.println("Binned counts:");
                        final int add = (int) ((statistics.getMax() - statistics.getMin()) / (double) numberOfBins);
                        for (int i = 0; i < binnedCounts.length; i++) {
                            System.err.println("" + (min + i * add) + ": " + binnedCounts[i]);
                            series.getData().add(new XYChart.Data<>("" + (min + i * add), binnedCounts[i]));
                        }
                        Platform.runLater(() -> barChart.getData().add(series));
                        return true;
                    }
                };
            }
        };
        controller.getCloseButton().setOnAction(actionEvent -> {
            if (service.isRunning())
                service.cancel();
            frame.setVisible(false);
        });
        controller.getProgressBar().visibleProperty().bind(service.runningProperty());
        service.setOnRunning(workerStateEvent -> controller.getProgressBar().progressProperty().bind(service.progressProperty()));

        service.setOnCancelled(workerStateEvent -> NotificationsInSwing.showError("Plot failed: " + service.getException()));
        service.restart();
    }

    public void show() {
        frame.setVisible(true);
    }
}
