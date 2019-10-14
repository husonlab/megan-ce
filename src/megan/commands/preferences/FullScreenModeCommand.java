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
package megan.commands.preferences;

import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class FullScreenModeCommand extends CommandBase implements ICheckBoxCommand {
    private static final GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];

    @Override
    public boolean isSelected() {
        return isApplicable() && device.getFullScreenWindow() == getViewer().getFrame();
    }

    public String getSyntax() {
        return "set fullScreen={false|true};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set fullScreen=");
        boolean state = np.getBoolean();
        np.matchIgnoreCase(";");
        JFrame frame = (state ? getViewer().getFrame() : null);
        device.setFullScreenWindow(frame);
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("set fullScreen=" + (!isSelected()) + ";");
    }

    public boolean isApplicable() {
        return getViewer() != null && device.isFullScreenSupported();
    }

    public String getName() {
        return "Full Screen Mode";
    }

    public String getDescription() {
        return "Full Screen Mode";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("FullScreen16.gif");
    }

    public boolean isCritical() {
        return false;
    }
}
