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

import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import jloda.swing.commands.CommandManager;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * manages commands in context of FX
 * Daniel Huson, 2.2016
 */
public class CommandManagerFX extends CommandManager {
    private final Map<MenuItem, ICommand> menuItem2CommandFX = new HashMap<>();
    private final Map<javafx.scene.control.ButtonBase, ICommand> button2CommandFX = new HashMap<>();
    private final static ActionEvent ACTION_EVENT_FROM_FX = new ActionEvent("CommandManagerWithFX", 0, null);

    /**
     * construct a parser
     *
     * @param dir
     */
    public CommandManagerFX(IDirector dir, List<ICommand> commands) {
        super(dir, commands);
    }


    /**
     * construct a parser and load all commands found for the given path
     */
    public CommandManagerFX(IDirector dir, IDirectableViewer viewer, String commandsPath) {
        this(dir, viewer, new String[]{commandsPath}, false);
    }

    /**
     * construct a parser and load all commands found for the given paths
     *
     * @param viewer usually an IDirectableViewer, but sometimes a JDialog
     */
    public CommandManagerFX(IDirector dir, Object viewer, String[] commandsPaths) {
        this(dir, viewer, commandsPaths, false);
    }

    /**
     * construct a parser and load all commands found for the given path
     */
    public CommandManagerFX(IDirector dir, IDirectableViewer viewer, String commandsPath, boolean returnOnCommandNotFound) {
        this(dir, viewer, new String[]{commandsPath}, returnOnCommandNotFound);
    }

    /**
     * construct a parser and load all commands found for the given paths
     *
     * @param viewer usually an IDirectableViewer, but sometimes a JDialog
     */
    public CommandManagerFX(IDirector dir, Object viewer, String[] commandsPaths, boolean returnOnCommandNotFound) {
        super(dir, viewer, commandsPaths, returnOnCommandNotFound);
    }

    /**
     * update the enable state
     */
    public void updateEnableState() {
        if (SwingUtilities.isEventDispatchThread())
            super.updateEnableState();

        for (MenuItem menuItem : menuItem2CommandFX.keySet()) {
            ICommand command = menuItem2CommandFX.get(menuItem);
            menuItem.setDisable(!command.isApplicable());
            if (command instanceof ICheckBoxCommand) {
                ((CheckMenuItem) menuItem).setSelected(((ICheckBoxCommand) command).isSelected());
            }
        }
    }

    /**
     * update the enable state for only the FX menu items
     */
    public void updateEnableStateFXItems() {
        for (MenuItem menuItem : menuItem2CommandFX.keySet()) {
            ICommand command = menuItem2CommandFX.get(menuItem);
            menuItem.setDisable(!command.isApplicable());
            if (command instanceof ICheckBoxCommand) {
                ((CheckMenuItem) menuItem).setSelected(((ICheckBoxCommand) command).isSelected());
            }
        }
    }

    /**
     * update the enable state for only the Swing menu items
     */
    public void updateEnableStateSwingItems() {
        if (SwingUtilities.isEventDispatchThread())
            super.updateEnableState();
    }

    /**
     * update the enable state
     */
    public void updateEnableState(String commandName) {
        if (SwingUtilities.isEventDispatchThread())
            super.updateEnableState(commandName);

        for (MenuItem menuItem : menuItem2CommandFX.keySet()) {
            ICommand command = menuItem2CommandFX.get(menuItem);
            if (command.getName().equals(commandName)) {
                menuItem.setDisable(!command.isApplicable());
                if (command instanceof ICheckBoxCommand) {
                    ((CheckMenuItem) menuItem).setSelected(((ICheckBoxCommand) command).isSelected());
                }
            }
        }
    }

    /**
     * get a menu item for the named command
     *
     * @param commandName
     * @return menu item
     */
    public MenuItem getMenuItemFX(String commandName) {
        ICommand command = getCommand(commandName);
        MenuItem item = getMenuItemFX(command);
        if (item != null && command != null)
            item.setDisable(!command.isApplicable());
        return item;
    }

    /**
     * get a menu item for the named command
     *
     * @param commandName
     * @param enabled
     * @return menu item
     */
    public MenuItem getMenuItemFX(String commandName, boolean enabled) {
        ICommand command = getCommand(commandName);
        MenuItem item = getMenuItemFX(command);
        if (item != null)
            item.setDisable(!enabled || !command.isApplicable());
        return item;
    }

    /**
     * creates a menu item for the given command
     *
     * @param command
     * @return menu item
     */
    public MenuItem getMenuItemFX(final ICommand command) {
        if (command == null) {
            MenuItem nullItem = new MenuItem("Null");
            nullItem.setDisable(true);
            return nullItem;
        }
        if (command instanceof ICheckBoxCommand) {
            final ICheckBoxCommand checkBoxCommand = (ICheckBoxCommand) command;

            final CheckMenuItem menuItem = new CheckMenuItem(command.getName());
            menuItem.setOnAction(event -> {
                checkBoxCommand.setSelected(menuItem.isSelected());
                if (command.getAutoRepeatInterval() > 0)
                    command.actionPerformedAutoRepeat(ACTION_EVENT_FROM_FX);
                else
                    command.actionPerformed(ACTION_EVENT_FROM_FX);
            });
            if (command.getDescription() != null) {
                menuItem.setUserData(command.getDescription());
            }
            if (command.getIcon() != null)
                menuItem.setGraphic(asImageViewFX(command.getIcon()));
            else
                menuItem.setGraphic((asImageViewFX(Objects.requireNonNull(ResourceManager.getIcon("Empty16.gif")))));
            if (command.getDescription() != null && menuItem.getGraphic() != null) {
                Tooltip.install(menuItem.getGraphic(), new Tooltip(command.getDescription()));
            }
            if (command.getAcceleratorKey() != null)
                menuItem.setAccelerator(translateAccelerator(command.getAcceleratorKey()));
            menuItem.setSelected(checkBoxCommand.isSelected());
            menuItem2CommandFX.put(menuItem, checkBoxCommand);
            return menuItem;
        } else {
            final MenuItem menuItem = new MenuItem(command.getName());
            menuItem.setOnAction(event -> SwingUtilities.invokeLater(() -> {
                if (command.getAutoRepeatInterval() > 0)
                    command.actionPerformedAutoRepeat(ACTION_EVENT_FROM_FX);
                else
                    command.actionPerformed(ACTION_EVENT_FROM_FX);
            }));
            if (command.getDescription() != null)
                menuItem.setUserData(command.getDescription());
            if (command.getIcon() != null)
                menuItem.setGraphic(asImageViewFX(command.getIcon()));
            else
                menuItem.setGraphic((asImageViewFX(Objects.requireNonNull(ResourceManager.getIcon("Empty16.gif")))));
            if (command.getDescription() != null && menuItem.getGraphic() != null) {
                Tooltip.install(menuItem.getGraphic(), new Tooltip(command.getDescription()));
            }
            if (command.getAcceleratorKey() != null)
                menuItem.setAccelerator(translateAccelerator(command.getAcceleratorKey()));
            menuItem2CommandFX.put(menuItem, command);
            return menuItem;
        }
    }

    /**
     * creates a button for the command
     *
     * @param commandName
     * @return button
     */
    public javafx.scene.control.ButtonBase getButtonFX(String commandName) {
        return getButtonFX(commandName, true);
    }

    private static boolean warned = false;

    /**
     * creates a button for the command
     *
     * @param commandName
     * @param enabled
     * @return button
     */
    private javafx.scene.control.ButtonBase getButtonFX(String commandName, boolean enabled) {
        javafx.scene.control.ButtonBase button = getButtonFX(getCommand(commandName));
        button.setDisable(!enabled);
        if (button.getText() != null && button.getText().equals("Null")) {
            System.err.println("Failed to create button for command '" + commandName + "'");
            if (!warned) {
                warned = true;
                System.err.println("Table of known commands:");
                for (String name : name2Command.keySet()) {
                    System.err.print(" '" + name + "'");
                }
                System.err.println();
            }
        }
        return button;
    }

    /**
     * creates a button for the command
     *
     * @param command
     * @return button
     */
    private javafx.scene.control.ButtonBase getButtonFX(final ICommand command) {
        if (command == null) {
            javafx.scene.control.Button nullButton = new javafx.scene.control.Button("Null");
            nullButton.setDisable(true);
            return nullButton;
        }
        if (command instanceof ICheckBoxCommand) {
            final ICheckBoxCommand checkBoxCommand = (ICheckBoxCommand) command;

            final CheckBox cbox = new CheckBox(command.getName());
            cbox.setOnAction(event -> {
                checkBoxCommand.setSelected(cbox.isSelected());
                if (command.getAutoRepeatInterval() > 0)
                    command.actionPerformedAutoRepeat(ACTION_EVENT_FROM_FX);
                else
                    command.actionPerformed(ACTION_EVENT_FROM_FX);
            });
            if (command.getDescription() != null) {
                cbox.setUserData(command.getDescription());
                Tooltip.install(cbox, new Tooltip(command.getDescription()));
            }
            if (command.getIcon() != null) {
                cbox.setGraphic(asImageViewFX(command.getIcon()));
            }
            cbox.setSelected(checkBoxCommand.isSelected());
            button2CommandFX.put(cbox, checkBoxCommand);
            return cbox;
        } else {
            final Button button = new Button(command.getName());
            button.setOnAction(event -> {
                if (command.getAutoRepeatInterval() > 0)
                    command.actionPerformedAutoRepeat(ACTION_EVENT_FROM_FX);
                else
                    command.actionPerformed(ACTION_EVENT_FROM_FX);
            });
            if (command.getDescription() != null) {
                button.setUserData(command.getDescription());
                Tooltip.install(button, new Tooltip(command.getDescription()));
            }
            if (command.getIcon() != null) {
                button.setGraphic(asImageViewFX(command.getIcon()));
            }
            button2CommandFX.put(button, command);
            return button;
        }
    }

    /**
     * creates a button for the command
     *
     * @param command
     * @return button
     */
    public javafx.scene.control.RadioButton getRadioButtonFX(final ICommand command) {
        if (command == null) {
            RadioButton nullButton = new RadioButton("Null");
            nullButton.setDisable(true);
            return nullButton;
        }
        if (command instanceof ICheckBoxCommand) {
            final ICheckBoxCommand checkBoxCommand = (ICheckBoxCommand) command;

            final RadioButton button = new RadioButton(command.getName());
            button.setOnAction(event -> {
                checkBoxCommand.setSelected(button.isSelected());
                if (command.getAutoRepeatInterval() > 0)
                    command.actionPerformedAutoRepeat(ACTION_EVENT_FROM_FX);
                else
                    command.actionPerformed(ACTION_EVENT_FROM_FX);
            });
            if (command.getDescription() != null) {
                button.setUserData(command.getDescription());
                Tooltip.install(button, new Tooltip(command.getDescription()));
            }
            if (command.getIcon() != null) {
                button.setGraphic(asImageViewFX(command.getIcon()));
            }
            button.setSelected(checkBoxCommand.isSelected());
            button2CommandFX.put(button, checkBoxCommand);
            return button;
        } else
            return null;
    }

    /**
     * get the director
     *
     * @return
     */
    public IDirector getDir() {
        return dir;
    }

    /**
     * convert AWT imageIcon to JavaFX image
     *
     * @param imageIcon
     * @return javaFX image
     */
    public static ImageView asImageViewFX(ImageIcon imageIcon) {
        java.awt.Image awtImage = imageIcon.getImage();
        if (awtImage != null) {
            final BufferedImage bImg;
            if (awtImage instanceof BufferedImage) {
                bImg = (BufferedImage) awtImage;
            } else {
                bImg = new BufferedImage(awtImage.getWidth(null), awtImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = bImg.createGraphics();
                graphics.drawImage(awtImage, 0, 0, null);
                graphics.dispose();
            }
            return new ImageView(SwingFXUtils.toFXImage(bImg, null));
        } else
            return null;
    }

    /**
     * converts a swing accelerator key to a JavaFX key combination
     *
     * @param acceleratorKey
     * @return key combination
     */
    private static KeyCombination translateAccelerator(KeyStroke acceleratorKey) {
        final List<KeyCombination.Modifier> modifiers = new ArrayList<>();

        if ((acceleratorKey.getModifiers() & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0)
            modifiers.add(KeyCombination.SHIFT_DOWN);
        if ((acceleratorKey.getModifiers() & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0)
            modifiers.add(KeyCombination.CONTROL_DOWN);
        if ((acceleratorKey.getModifiers() & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0)
            modifiers.add(KeyCombination.ALT_DOWN);
        if ((acceleratorKey.getModifiers() & InputEvent.META_DOWN_MASK) != 0)
            modifiers.add(KeyCombination.META_DOWN);

        KeyCode keyCode = FXSwingUtilities.getKeyCodeFX(acceleratorKey.getKeyCode());
        return new KeyCodeCombination(keyCode, modifiers.toArray(new KeyCombination.Modifier[0]));
    }
}
