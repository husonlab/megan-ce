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
package megan.commands.clipboard;

import jloda.swing.util.ResourceManager;
import megan.commands.CommandBase;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * base for cut, copy and paste
 * Daniel Huson, 11.2010
 */
public abstract class ClipboardBase extends CommandBase {

    // need an instance to get default textComponent Actions
    static private DefaultEditorKit kit;

    /**
     * find the action
     *
     * @param name
     * @return action
     */
    static protected Action findAction(String name) {
        if (kit == null)
            kit = new DefaultEditorKit();

        Action[] actions = kit.getActions();
        for (int i = 0; i < kit.getActions().length; i++) {
            Action action = actions[i];
            if (action.getValue(AbstractAction.NAME).equals(name))
                return action;
        }
        return null;
    }

    static public Action getCutDefaultKit() {
        Action action = findAction(DefaultEditorKit.cutAction);

        if (action == null)
            return null;

        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

        action.putValue(Action.SHORT_DESCRIPTION, "Cut");

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Cut16.gif"));

        return action;
    }

    static public Action getCopyDefaultKit() {
        Action action = findAction(DefaultEditorKit.copyAction);
        if (action == null)
            return null;

        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(Action.SHORT_DESCRIPTION, "Copy");

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Copy16.gif"));

        return action;
    }


    static public Action getPasteDefaultKit() {
        Action action = findAction(DefaultEditorKit.pasteAction);
        if (action == null)
            return null;

        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(Action.SHORT_DESCRIPTION, "Paste");

        action.putValue(AbstractAction.SMALL_ICON, ResourceManager.getIcon("sun/Paste16.gif"));

        return action;
    }

    static public Action getSelectAllDefaultEditorKit() {
        Action action = findAction(DefaultEditorKit.selectAllAction);
        if (action == null)
            return null;
        action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        action.putValue(Action.SHORT_DESCRIPTION, "Select All");

        return action;
    }
}
