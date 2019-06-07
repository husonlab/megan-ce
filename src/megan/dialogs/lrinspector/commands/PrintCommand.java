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
package megan.dialogs.lrinspector.commands;

import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.transform.Scale;
import jloda.fx.util.IHasJavaFXStageAndRoot;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.dialogs.lrinspector.LRInspectorViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class PrintCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=print;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        LRInspectorViewer viewer = ((LRInspectorViewer) getViewer());

        PrinterJob job = PrinterJob.createPrinterJob();

        if (job != null) {
            if (job.showPrintDialog(viewer.getJavaFXStage())) {
//        Paper customPaper = PrintHelper.createPaper("Custom", printImage.getLayoutBounds().getWidth(),
//                printImage.getLayoutBounds().getHeight(), Units.POINT);
//        PageLayout layout = pdfPrinter.createPageLayout(customPaper, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
                // TODO: 30.05.2017 improve or wait until bug JDK-8088509 is fixed. Alternatively:
//        job.showPageSetupDialog(new Stage());
//        Paper paper = PrintHelper.createPaper("CustomSize", 600,400, Units.POINT);
                PageLayout layout = job.getPrinter().createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
//        job.showPageSetupDialog(stage);
                job.getJobSettings().setPageLayout(layout);

                final Node node = viewer.getController().getTableView();
                // Scale image to paper size (A4)
                double scaleX = layout.getPrintableWidth() / node.getLayoutBounds().getWidth();
                double scaleY = layout.getPrintableHeight() / node.getLayoutBounds().getHeight();
                if (scaleX > scaleY) {
                    scaleX = scaleY;
                } else {
                    scaleY = scaleX;
                }
                node.getTransforms().add(new Scale(scaleX, scaleY));

                boolean printSpooled = job.printPage(layout, node);
                if (printSpooled) {
                    job.endJob();
                    System.err.println("Wrote Image to PDF successfully");
                } else {
                    throw new IOException("Error writing PDF.");
                }
                node.getTransforms().add(new Scale(1 / scaleX, 1 / scaleY));
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() instanceof IHasJavaFXStageAndRoot;
    }

    public String getName() {
        return "Print...";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
    }

    public String getDescription() {
        return "Print the main panel";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Print16.gif");
    }
}

