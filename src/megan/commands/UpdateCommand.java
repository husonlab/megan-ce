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

import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirectableViewer;
import jloda.gui.director.IDirector;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * rescan some of the computation
 */
public class UpdateCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "update [reProcess={false|true}] [reset={false|true}] [reInduce={false|true}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("update");
        IDirectableViewer viewer = getViewer();

        boolean reInduce = np.peekMatchIgnoreCase(";"); // if nothing specified, assume reinduce requested


        boolean reprocess = false;
        if (np.peekMatchIgnoreCase("reProcess"))  // reprocess all hits
        {
            np.matchIgnoreCase("reProcess=");
            reprocess = np.getBoolean();
        }
        boolean reset = false;
        if (np.peekMatchIgnoreCase("reset")) // result the tree
        {
            np.matchIgnoreCase("reset=");
            reset = np.getBoolean();
        }
        if (np.peekMatchIgnoreCase("reInduce")) // reinduce the tree
        {
            np.matchIgnoreCase("reInduce=");
            reInduce = np.getBoolean();
        }
        np.matchIgnoreCase(";");

        if (viewer instanceof MainViewer) {
            if (reprocess) {
                getDoc().processReadHits();
            }
            ((MainViewer) viewer).setDoReset(reset);
            ((MainViewer) viewer).setDoReInduce(reInduce);
            if (reInduce) {
                getDoc().setLastRecomputeTime(System.currentTimeMillis());
            }
        }
        viewer.updateView(IDirector.ALL);
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("update reprocess=true reset=true reInduce=true;");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Update";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Update data";
    }

    public boolean isCritical() {
        return true;
    }
}

