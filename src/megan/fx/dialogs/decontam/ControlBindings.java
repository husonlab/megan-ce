/*
 *  SetupControlBindings.java Copyright (C) 2021. Daniel H. Huson GPL
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package megan.fx.dialogs.decontam;

import jloda.swing.util.ToolBar;

public class ControlBindings {
    public static void setup(final DecontamDialogController controller, final DecontamDialog viewer, ToolBar toolBar) {
        controller.getCloseButton().setOnAction(e -> {
            viewer.getDir().execute("close;", viewer.getCommandManager());
        });
    }

    /**
     * update the scene
     *
     * @param viewer
     */
    public static void updateScene(final DecontamDialogController controller, DecontamDialog viewer) {


    }
}
