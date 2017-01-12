/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.samplesviewer.commands.attributes;

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * * delete command
 * * Daniel Huson, 9.2015
 */
public class HideColumnsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "hide attribute=<name> [<name>...];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("hide attribute=");

        Set<String> attributes = new HashSet<>();
        while (!np.peekMatchIgnoreCase(";")) {
            String attribute = np.getWordRespectCase();
            attributes.add(attribute);
        }
        np.matchIgnoreCase(";");

        if (attributes.size() > 0) {
            final SamplesViewer viewer = ((SamplesViewer) getViewer());
            viewer.getSamplesTable().hideColumns(attributes.toArray(new String[attributes.size()]));
        }
    }

    public void actionPerformed(ActionEvent event) {
        final SamplesViewer viewer = ((SamplesViewer) getViewer());
        final Collection<String> attributes = viewer.getSamplesTable().getSelectedAttributes();

        if (attributes.size() > 0) {
            final StringBuilder buf = new StringBuilder();
            buf.append("hide attribute=");
            for (String attributeName : attributes) {
                buf.append(" '").append(attributeName).append("'");
            }
            buf.append(";");
            execute(buf.toString());
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTable().getNumberOfSelectedCols() > 0;
    }

    public String getName() {
        return "Hide";
    }


    public String getDescription() {
        return "Hide selected columns";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
