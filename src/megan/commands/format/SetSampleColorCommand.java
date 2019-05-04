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
package megan.commands.format;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseColorDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;

/**
 * * selection command
 * * Daniel Huson, 11.2010
 */
public class SetSampleColorCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set nodeColor=<color> [sample=<name ...>];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set nodeColor=");
        final Color color = np.getColor();

        final java.util.List<String> samples = new LinkedList<>();
        if (np.peekMatchIgnoreCase("sample=")) {
            np.matchIgnoreCase("sample=");
            while (!np.peekMatchIgnoreCase(";")) {
                samples.add(np.getWordRespectCase());
            }
        }
        np.matchIgnoreCase(";");

        final Document doc = ((Director) getDir()).getDocument();

        if (samples.size() == 0)
            samples.addAll(doc.getSampleSelection().getAll());

        if (samples.size() > 0) {
            for (String sample : samples) {
                doc.getSampleAttributeTable().putSampleColor(sample, color);
            }
            doc.setDirty(true);
        }
    }

    public void actionPerformed(ActionEvent event) {
        final Color color = ChooseColorDialog.showChooseColorDialog(getViewer().getFrame(), "Choose sample color", ProgramProperties.get("NodeFillColor", Color.WHITE));
        if (color != null) {
            execute("set nodeColor=" + color.getRed() + " " + color.getGreen() + " " + color.getBlue() + ";");
            ProgramProperties.put("NodeFillColor", color);
        }
    }

    public boolean isApplicable() {
        final Document doc = ((Director) getDir()).getDocument();
        return doc.getSampleSelection().size() > 0;
    }

    public String getName() {
        return "Set Color...";
    }

    public String getDescription() {
        return "Set node color";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("YellowSquare16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}
