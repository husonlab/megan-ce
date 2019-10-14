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
package megan.importblast;

import megan.parsers.blast.BlastFileFormat;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * combo box for choosing blast format
 * Daniel Huson, 6.2013
 */
public class FormatCBox extends JComboBox<String> {

    /**
     * constructor
     */
    public FormatCBox() {
        setEditable(false);
        for (BlastFileFormat value : BlastFileFormat.values()) {
            addItem(value.toString());
        }
        setMaximumSize(new Dimension(150, 20));
        setPreferredSize(new Dimension(150, 20));
        setToolTipText("Select format of input files");
    }

    /**
     * get the selected format
     *
     * @return selected format
     */
    public String getSelectedFormat() {
        return Objects.requireNonNull(getSelectedItem()).toString();
    }

    /**
     * set the selected format
     *
     * @param name
     */
    public void setSelectedFormat(String name) {
        for (int i = 0; i < getItemCount(); i++) {
            final String item = getItemAt(i);
            if (item.equalsIgnoreCase(name)) {
                setSelectedIndex(i);
                break;
            }
        }
    }
}
