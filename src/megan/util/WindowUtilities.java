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

package megan.util;

import javax.swing.*;
import java.awt.*;

/**
 * some Swing window utilities
 */
public class WindowUtilities {
    /**
     * bring to the front (using the swing thread)
     *
     * @param window
     */
    public static void toFront(final Window window) {
        if (window != null) {
            Runnable runnable = () -> {
                window.setVisible(true);
                window.toFront();
            };
            if (SwingUtilities.isEventDispatchThread())
                runnable.run();
            else
                SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * bring to the front (using the swing thread)
     *
     * @param frame
     */
    public static void toFront(final JFrame frame) {
        // if (SwingUtilities.isEventDispatchThread())
        //    System.err.println("HellO!");

        if (frame != null) {
            final Runnable runnable = () -> {
                frame.setVisible(true);
                frame.setState(JFrame.NORMAL);
                frame.toFront();
                frame.requestFocus();
            };
            if (SwingUtilities.isEventDispatchThread())
                runnable.run();
            else
                SwingUtilities.invokeLater(runnable);
        }

    }
}
