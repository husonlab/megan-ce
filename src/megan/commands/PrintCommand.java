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
package megan.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.dialogs.lrinspector.LRInspectorViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

public class PrintCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show window=print;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        if (getViewer() instanceof LRInspectorViewer) {
            megan.dialogs.lrinspector.commands.PrintCommand command = new megan.dialogs.lrinspector.commands.PrintCommand();
            command.setDir(getDir());
            command.setViewer(getViewer());
            command.setCommandManager(getCommandManager());
            command.apply(np);
            return;
        }

        np.matchIgnoreCase(getSyntax());

        PrinterJob job = PrinterJob.getPrinterJob();
        if (ProgramProperties.getPageFormat() != null)
            job.setPrintable((Printable) getViewer(), ProgramProperties.getPageFormat());
        else
            job.setPrintable((Printable) getViewer());

        // Put up the dialog box
        if (job.printDialog()) {
            // Print the job if the user didn't cancel printing
            try {
                job.print();
            } catch (Exception ex) {
                Basic.caught(ex);
                NotificationsInSwing.showError(getViewer().getFrame(), "Print failed: " + ex);
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() instanceof Printable;
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

