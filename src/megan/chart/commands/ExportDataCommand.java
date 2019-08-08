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
package megan.chart.commands;


import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * export data command
 * Daniel Huson, 11.2010
 */
public class ExportDataCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export what=chartData file=<filename>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=chartData file=");
        String fileName = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");

        try {
            ChartViewer chartViewer = (ChartViewer) getViewer();
            FileWriter w = new FileWriter(fileName);
            chartViewer.getChartDrawer().writeData(w);
            w.close();
        } catch (IOException e) {
            NotificationsInSwing.showError("Export Data failed: " + e.getMessage());
        }
    }

    public void actionPerformed(ActionEvent event) {
        ChartViewer viewer = (ChartViewer) getViewer();
        String name = Basic.toCleanName(viewer.getChartData().getDataSetName()) + "-chart";

        String lastOpenFile = ProgramProperties.get("DataFile", "");
        if (lastOpenFile == null)
            lastOpenFile = name + ".txt";
        else
            lastOpenFile = (new File((new File(lastOpenFile)).getParent(), name)).getPath();

        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), new File(lastOpenFile), new TextFileFilter(), new TextFileFilter(), event, "Save data file", ".txt");

        if (file != null) {
            if (Basic.getFileSuffix(file.getName()) == null)
                file = Basic.replaceFileSuffix(file, ".txt");
            ProgramProperties.put("DataFile", file.getPath());
            execute("export what=chartData file='" + file.getPath() + "';");
        }
    }

    // if in ask to save, modify event source to tell calling method can see that user has canceled

    void replyUserHasCanceledInAskToSave(ActionEvent event) {
        ((Boolean[]) event.getSource())[0] = true;
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Export Data...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/SaveAs16.gif");
    }

    public String getDescription() {
        return "Export data to a file";
    }

    public boolean isCritical() {
        return false;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
