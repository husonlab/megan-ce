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
package megan.commands.preferences;

import jloda.gui.commands.ICheckBoxCommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.fx.NotificationsInSwing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ShowNotificationsCommand extends CommandBase implements ICheckBoxCommand {
    static final GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];

    @Override
    public boolean isSelected() {
        return NotificationsInSwing.isShowNotifications();
    }

    public String getSyntax() {
        return "set showNotifications={true|false};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set showNotifications=");
        boolean state = np.getBoolean();
        np.matchIgnoreCase(";");
        NotificationsInSwing.setShowNotifications(state);
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("set showNotifications=" + (!NotificationsInSwing.isShowNotifications()) + ";");
    }

    public boolean isApplicable() {
        return getViewer() != null && device.isFullScreenSupported();
    }

    public String getName() {
        return "Show Notifications";
    }

    public String getDescription() {
        return "Show notifications";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Preferences16.gif");

    }

    public boolean isCritical() {
        return false;
    }
}
