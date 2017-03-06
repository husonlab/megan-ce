/*
 *  Copyright (C) 2015 Daniel H. Huson
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

package megan.dialogs.importcsv;

import megan.core.Director;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * runs the meganizer in MEGAN
 * Daniel Huson, 3.2016
 */
public class ParsersDialog extends ImportBlastDialog {
    private boolean pressedApply = false;

    /**
     * constructor
     *
     * @param parent
     * @param dir
     */
    public ParsersDialog(Component parent, Director dir, Collection<String> fNames) {
        super(parent, dir, fNames, "Setup Accession Parsing - MEGAN");
    }

    @Override
    public void addFilesTab(JTabbedPane tabbedPane) {
        // no files tab
    }

    @Override
    public void addLCATab(JTabbedPane tabbedPane) {
        // no lca tab
    }

    /**
     * apply meganize DAA file
     */
    public void apply() {
        pressedApply = true;
        setVisible(false);
    }

    @Override
    public void updateView(String what) {
    }

    /**
     * is  applicable?
     *
     * @return true, if applicable
     */
    public boolean isAppliable() {
        return true;
    }

    public boolean isPressedApply() {
        return pressedApply;
    }

}

