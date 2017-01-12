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

package megan.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Shows a popup menu to choose an object
 * Daniel 2.2016
 */
public class PopupChoice<T> extends JPopupMenu {
    /**
     * add choices to an existing menu
     *
     * @param popupMenu
     * @param choices
     * @param initialChoice
     * @param callBack
     */
    public static <T> void addToJMenu(JPopupMenu popupMenu, T[] choices, T initialChoice, final CallBack<T> callBack) {
        add(popupMenu, choices, initialChoice, callBack);
    }
    /**
     * constructor
     *
     * @param choices        null entries are represented by separators
     * @param initialChoice, can be null
     */
    public PopupChoice(T[] choices, T initialChoice, final CallBack<T> callBack) {
        add(this, choices, initialChoice, callBack);
    }

    /**
     * add choices
     *
     * @param choices
     * @param initialChoice
     * @param callBack
     */
    private static <T> void add(JPopupMenu menu, T[] choices, T initialChoice, final CallBack<T> callBack) {
        for (final T obj : choices) {
            if (obj != null) {
                final String name = obj.toString();
                JCheckBoxMenuItem checkBoxMenuItem = new JCheckBoxMenuItem(name);
                checkBoxMenuItem.setAction(new AbstractAction(name) {
                    public void actionPerformed(ActionEvent e) {
                        callBack.call(obj);
                    }
                });
                if (initialChoice != null && obj.equals(initialChoice))
                    checkBoxMenuItem.setSelected(true);
                menu.add(checkBoxMenuItem);
            } else
                menu.addSeparator();
        }
    }

    /**
     * show
     *
     * @param frame
     */
    public void showAtCurrentMouseLocation(JFrame frame) {
        final Point location = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(location, frame);
        show(frame, location.x, location.y);
    }
}
