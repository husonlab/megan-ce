/*
 * SetSampleColorCommand.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.samplesviewer.commands.samples;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseColorDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * draw sample node color
 * Daniel Huson, 11.2019
 */
public class SetSampleColorCommand extends CommandBase implements ICommand {
    /**
     * apply
     *
	 */
    public void apply(NexusStreamParser np) throws Exception {
        final Document doc = ((Director) getDir()).getDocument();

        np.matchIgnoreCase("set nodeColor=");
        Color color = null;
        if (np.peekMatchIgnoreCase("null"))
            np.matchIgnoreCase("null");
        else
            color = np.getColor();
        final List<String> samples = new LinkedList<>();
        if (np.peekMatchIgnoreCase("sample=")) {
            np.matchIgnoreCase("sample=");
            while (!np.peekMatchIgnoreCase(";")) {
                samples.add(np.getWordRespectCase());
            }
        }
        np.matchIgnoreCase(";");

        if (samples.size() == 0)
            samples.addAll(doc.getSampleSelection().getAll());

        if (samples.size() > 0) {
            for (String sample : samples) {
                doc.getSampleAttributeTable().putSampleColor(sample, color);
            }
            ((Director) getDir()).getDocument().setDirty(true);
        }

    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return null;
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    public boolean isApplicable() {
        return true;
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Color...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the sample color";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("YellowSquare16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        final SamplesViewer viewer = (SamplesViewer) getViewer();

        final Collection<String> selected = viewer.getSamplesTableView().getSelectedSamples();

        if (selected.size() > 0) {
            Color color = ChooseColorDialog.showChooseColorDialog(getViewer().getFrame(), "Choose sample color", null);

            if (color != null)
				execute("set nodeColor=" + color.getRed() + " " + color.getGreen() + " " + color.getBlue() + " sample='" + StringUtils.toString(selected, "' '") + "';");
        }
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
