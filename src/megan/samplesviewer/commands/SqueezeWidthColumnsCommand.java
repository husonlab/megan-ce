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
package megan.samplesviewer.commands;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import jloda.fx.control.table.MyTableView;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * * reorder samples in viewer
 * * Daniel Huson, 9.2012
 */
public class SqueezeWidthColumnsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "squeeze above=<number> to=<number>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("squeeze above=");
        final int threshold = np.getInt(1, 10000);
        np.matchIgnoreCase("to=");
        final int size = np.getInt(1, threshold);
        np.matchIgnoreCase(";");

        Platform.runLater(() -> {
            final SamplesViewer viewer = (SamplesViewer) getViewer();
            for (int col = 0; col < viewer.getSamplesTableView().getAttributeCount(); col++) {
                final TableColumn<MyTableView.MyTableRow, ?> column = viewer.getSamplesTableView().getAttribute(col);
                if (column != null && column.getWidth() > threshold)
                    column.setPrefWidth(size);
            }
        });
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("squeeze above=150 to=80;");
    }

    public boolean isApplicable() {
        final SamplesViewer viewer = (SamplesViewer) getViewer();
        return viewer != null && viewer.getDocument().getNumberOfSamples() > 0;
    }

    public String getName() {
        return "Squeeze Wide Columns";
    }

    public String getDescription() {
        return "Squeeze wide columns to narrow width";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

    }
}
