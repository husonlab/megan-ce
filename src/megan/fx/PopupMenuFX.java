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
package megan.fx;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import jloda.swing.commands.CommandManager;
import jloda.swing.commands.ICommand;
import jloda.swing.commands.TeXGenerator;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;

import java.util.Objects;

/**
 * popup menu
 * Daniel Huson, 11.2010
 */
public class PopupMenuFX extends ContextMenu {
    /**
     * constructor
     *
     * @param configuration
     * @param commandManager
     */
    public PopupMenuFX(String configuration, CommandManagerFX commandManager) {
        this(configuration, commandManager, false);
    }

    /**
     * constructor
     *
     * @param configuration
     * @param commandManager
     */
    public PopupMenuFX(String configuration, CommandManagerFX commandManager, boolean showApplicableOnly) {
        super();
        if (configuration != null && configuration.length() > 0) {
            String[] tokens = configuration.split(";");

            for (String token : tokens) {
                if (token.equals("|")) {
                    getItems().add(new SeparatorMenuItem());
                } else {
                    MenuItem menuItem;
                    ICommand command = commandManager.getCommand(token);
                    if (command == null) {
                        if (showApplicableOnly)
                            continue;
                        menuItem = new MenuItem(token + "#");
                        menuItem.setDisable(true);
                        getItems().add(menuItem);
                    } else {
                        if (CommandManager.getCommandsToIgnore().contains(command.getName()))
                            continue;
                        if (showApplicableOnly && !command.isApplicable())
                            continue;
                        menuItem = commandManager.getMenuItemFX(command);
                        menuItem.setAccelerator(null);
                        getItems().add(menuItem);
                    }
                    if (menuItem.getGraphic() == null)
                        menuItem.setGraphic(CommandManagerFX.asImageViewFX(Objects.requireNonNull(ResourceManager.getIcon("Empty16.gif"))));
                }
            }
        }
        if (ProgramProperties.get("showtex", false)) {
            System.out.println(TeXGenerator.getPopupMenuLaTeX(configuration, commandManager));
        }
        try {
            commandManager.updateEnableState();
        } catch (Exception ignored) {
        }
    }
}
