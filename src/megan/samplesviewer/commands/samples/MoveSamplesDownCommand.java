/*
 *  MoveSamplesDownCommand.java Copyright (C) 2019 Daniel H. Huson
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
package megan.samplesviewer.commands.samples;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

/**
 * * delete command
 * * Daniel Huson, 9.2015
 */
public class MoveSamplesDownCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        final SamplesViewer viewer = ((SamplesViewer) getViewer());
        final Collection<String> samples = viewer.getSamplesTableView().getSelectedSamples();

        if (samples.size() > 0) {
            final StringBuilder buf = new StringBuilder();
            buf.append("move sample=");
            for (String attributeName : samples) {
                buf.append(" '").append(attributeName).append("'");
            }
            buf.append(" direction=down;");
            executeImmediately(buf.toString());
        }
    }

    public boolean isApplicable() {
        if (getViewer() instanceof SamplesViewer) {
            SamplesViewer viewer = (SamplesViewer) getViewer();
            return viewer.getSamplesTableView().getCountSelectedSamples() > 0 && !viewer.getSamplesTableView().getSelectedSamplesIndices().contains(Math.max(0, viewer.getSamplesTableView().getSampleCount() - 1));
        } else
            return false;
    }

    public String getName() {
        return "Move Down";
    }


    public String getDescription() {
        return "Move selected samples";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Down16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

    }
}