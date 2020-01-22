/*
 * FXSwingUtilities.java Copyright (C) 2020. Daniel H. Huson
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
 *
 */

package megan.fx;

import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;

/**
 * some Utilities to help integrate Swing and JavaFX
 */
public class FXSwingUtilities {
    /**
     * convert awt key code to KeyCode
     *
     * @param awtKeyCode
     * @return KeyCode
     */
    public static KeyCode getKeyCodeFX(int awtKeyCode) {
        switch (awtKeyCode) {
            case java.awt.event.KeyEvent.VK_A:
                return KeyCode.A;

            case java.awt.event.KeyEvent.VK_ACCEPT:
                return KeyCode.ACCEPT;

            case java.awt.event.KeyEvent.VK_ADD:
                return KeyCode.ADD;

            case java.awt.event.KeyEvent.VK_AGAIN:
                return KeyCode.AGAIN;

            case java.awt.event.KeyEvent.VK_ALL_CANDIDATES:
                return KeyCode.ALL_CANDIDATES;

            case java.awt.event.KeyEvent.VK_ALPHANUMERIC:
                return KeyCode.ALPHANUMERIC;

            case java.awt.event.KeyEvent.VK_ALT:
                return KeyCode.ALT;

            case java.awt.event.KeyEvent.VK_ALT_GRAPH:
                return KeyCode.ALT_GRAPH;

            case java.awt.event.KeyEvent.VK_AMPERSAND:
                return KeyCode.AMPERSAND;

            case java.awt.event.KeyEvent.VK_ASTERISK:
                return KeyCode.ASTERISK;

            case java.awt.event.KeyEvent.VK_AT:
                return KeyCode.AT;

            case java.awt.event.KeyEvent.VK_B:
                return KeyCode.B;

            case java.awt.event.KeyEvent.VK_BACK_QUOTE:
                return KeyCode.BACK_QUOTE;

            case java.awt.event.KeyEvent.VK_BACK_SLASH:
                return KeyCode.BACK_SLASH;

            case java.awt.event.KeyEvent.VK_BACK_SPACE:
                return KeyCode.BACK_SPACE;

            case java.awt.event.KeyEvent.VK_BEGIN:
                return KeyCode.BEGIN;

            case java.awt.event.KeyEvent.VK_BRACELEFT:
                return KeyCode.BRACELEFT;

            case java.awt.event.KeyEvent.VK_BRACERIGHT:
                return KeyCode.BRACERIGHT;

            case java.awt.event.KeyEvent.VK_C:
                return KeyCode.C;

            case java.awt.event.KeyEvent.VK_CANCEL:
                return KeyCode.CANCEL;

            case java.awt.event.KeyEvent.VK_CAPS_LOCK:
                return KeyCode.CAPS;

            case java.awt.event.KeyEvent.VK_CIRCUMFLEX:
                return KeyCode.CIRCUMFLEX;

            case java.awt.event.KeyEvent.VK_CLEAR:
                return KeyCode.CLEAR;

            case java.awt.event.KeyEvent.VK_CLOSE_BRACKET:
                return KeyCode.CLOSE_BRACKET;

            case java.awt.event.KeyEvent.VK_CODE_INPUT:
                return KeyCode.CODE_INPUT;

            case java.awt.event.KeyEvent.VK_COLON:
                return KeyCode.COLON;

            case java.awt.event.KeyEvent.VK_COMMA:
                return KeyCode.COMMA;

            case java.awt.event.KeyEvent.VK_COMPOSE:
                return KeyCode.COMPOSE;

            case java.awt.event.KeyEvent.VK_CONTEXT_MENU:
                return KeyCode.CONTEXT_MENU;

            case java.awt.event.KeyEvent.VK_CONTROL:
                return KeyCode.CONTROL;

            case java.awt.event.KeyEvent.VK_CONVERT:
                return KeyCode.CONVERT;

            case java.awt.event.KeyEvent.VK_COPY:
                return KeyCode.COPY;

            case java.awt.event.KeyEvent.VK_CUT:
                return KeyCode.CUT;

            case java.awt.event.KeyEvent.VK_D:
                return KeyCode.D;

            case java.awt.event.KeyEvent.VK_DEAD_ABOVEDOT:
                return KeyCode.DEAD_ABOVEDOT;

            case java.awt.event.KeyEvent.VK_DEAD_ABOVERING:
                return KeyCode.DEAD_ABOVERING;

            case java.awt.event.KeyEvent.VK_DEAD_ACUTE:
                return KeyCode.DEAD_ACUTE;

            case java.awt.event.KeyEvent.VK_DEAD_BREVE:
                return KeyCode.DEAD_BREVE;

            case java.awt.event.KeyEvent.VK_DEAD_CARON:
                return KeyCode.DEAD_CARON;

            case java.awt.event.KeyEvent.VK_DEAD_CEDILLA:
                return KeyCode.DEAD_CEDILLA;

            case java.awt.event.KeyEvent.VK_DEAD_CIRCUMFLEX:
                return KeyCode.DEAD_CIRCUMFLEX;

            case java.awt.event.KeyEvent.VK_DEAD_DIAERESIS:
                return KeyCode.DEAD_DIAERESIS;

            case java.awt.event.KeyEvent.VK_DEAD_DOUBLEACUTE:
                return KeyCode.DEAD_DOUBLEACUTE;

            case java.awt.event.KeyEvent.VK_DEAD_GRAVE:
                return KeyCode.DEAD_GRAVE;

            case java.awt.event.KeyEvent.VK_DEAD_IOTA:
                return KeyCode.DEAD_IOTA;

            case java.awt.event.KeyEvent.VK_DEAD_MACRON:
                return KeyCode.DEAD_MACRON;

            case java.awt.event.KeyEvent.VK_DEAD_OGONEK:
                return KeyCode.DEAD_OGONEK;

            case java.awt.event.KeyEvent.VK_DEAD_SEMIVOICED_SOUND:
                return KeyCode.DEAD_SEMIVOICED_SOUND;

            case java.awt.event.KeyEvent.VK_DEAD_TILDE:
                return KeyCode.DEAD_TILDE;

            case java.awt.event.KeyEvent.VK_DEAD_VOICED_SOUND:
                return KeyCode.DEAD_VOICED_SOUND;

            case java.awt.event.KeyEvent.VK_DECIMAL:
                return KeyCode.DECIMAL;

            case java.awt.event.KeyEvent.VK_DELETE:
                return KeyCode.DELETE;

            case java.awt.event.KeyEvent.VK_0:
                return KeyCode.DIGIT0;

            case java.awt.event.KeyEvent.VK_1:
                return KeyCode.DIGIT1;

            case java.awt.event.KeyEvent.VK_2:
                return KeyCode.DIGIT2;

            case java.awt.event.KeyEvent.VK_3:
                return KeyCode.DIGIT3;

            case java.awt.event.KeyEvent.VK_4:
                return KeyCode.DIGIT4;

            case java.awt.event.KeyEvent.VK_5:
                return KeyCode.DIGIT5;

            case java.awt.event.KeyEvent.VK_6:
                return KeyCode.DIGIT6;

            case java.awt.event.KeyEvent.VK_7:
                return KeyCode.DIGIT7;

            case java.awt.event.KeyEvent.VK_8:
                return KeyCode.DIGIT8;

            case java.awt.event.KeyEvent.VK_9:
                return KeyCode.DIGIT9;

            case java.awt.event.KeyEvent.VK_DIVIDE:
                return KeyCode.DIVIDE;

            case java.awt.event.KeyEvent.VK_DOLLAR:
                return KeyCode.DOLLAR;

            case java.awt.event.KeyEvent.VK_DOWN:
                return KeyCode.DOWN;

            case java.awt.event.KeyEvent.VK_E:
                return KeyCode.E;

            case java.awt.event.KeyEvent.VK_END:
                return KeyCode.END;

            case java.awt.event.KeyEvent.VK_ENTER:
                return KeyCode.ENTER;

            case java.awt.event.KeyEvent.VK_EQUALS:
                return KeyCode.EQUALS;

            case java.awt.event.KeyEvent.VK_ESCAPE:
                return KeyCode.ESCAPE;

            case java.awt.event.KeyEvent.VK_EURO_SIGN:
                return KeyCode.EURO_SIGN;

            case java.awt.event.KeyEvent.VK_EXCLAMATION_MARK:
                return KeyCode.EXCLAMATION_MARK;

            case java.awt.event.KeyEvent.VK_F:
                return KeyCode.F;

            case java.awt.event.KeyEvent.VK_F1:
                return KeyCode.F1;

            case java.awt.event.KeyEvent.VK_F10:
                return KeyCode.F10;

            case java.awt.event.KeyEvent.VK_F11:
                return KeyCode.F11;

            case java.awt.event.KeyEvent.VK_F12:
                return KeyCode.F12;

            case java.awt.event.KeyEvent.VK_F13:
                return KeyCode.F13;

            case java.awt.event.KeyEvent.VK_F14:
                return KeyCode.F14;

            case java.awt.event.KeyEvent.VK_F15:
                return KeyCode.F15;

            case java.awt.event.KeyEvent.VK_F16:
                return KeyCode.F16;

            case java.awt.event.KeyEvent.VK_F17:
                return KeyCode.F17;

            case java.awt.event.KeyEvent.VK_F18:
                return KeyCode.F18;

            case java.awt.event.KeyEvent.VK_F19:
                return KeyCode.F19;

            case java.awt.event.KeyEvent.VK_F2:
                return KeyCode.F2;

            case java.awt.event.KeyEvent.VK_F20:
                return KeyCode.F20;

            case java.awt.event.KeyEvent.VK_F21:
                return KeyCode.F21;

            case java.awt.event.KeyEvent.VK_F22:
                return KeyCode.F22;

            case java.awt.event.KeyEvent.VK_F23:
                return KeyCode.F23;

            case java.awt.event.KeyEvent.VK_F24:
                return KeyCode.F24;

            case java.awt.event.KeyEvent.VK_F3:
                return KeyCode.F3;

            case java.awt.event.KeyEvent.VK_F4:
                return KeyCode.F4;

            case java.awt.event.KeyEvent.VK_F5:
                return KeyCode.F5;

            case java.awt.event.KeyEvent.VK_F6:
                return KeyCode.F6;

            case java.awt.event.KeyEvent.VK_F7:
                return KeyCode.F7;

            case java.awt.event.KeyEvent.VK_F8:
                return KeyCode.F8;

            case java.awt.event.KeyEvent.VK_F9:
                return KeyCode.F9;

            case java.awt.event.KeyEvent.VK_FINAL:
                return KeyCode.FINAL;

            case java.awt.event.KeyEvent.VK_FIND:
                return KeyCode.FIND;

            case java.awt.event.KeyEvent.VK_FULL_WIDTH:
                return KeyCode.FULL_WIDTH;

            case java.awt.event.KeyEvent.VK_G:
                return KeyCode.G;

            case java.awt.event.KeyEvent.VK_GREATER:
                return KeyCode.GREATER;

            case java.awt.event.KeyEvent.VK_H:
                return KeyCode.H;

            case java.awt.event.KeyEvent.VK_HALF_WIDTH:
                return KeyCode.HALF_WIDTH;

            case java.awt.event.KeyEvent.VK_HELP:
                return KeyCode.HELP;

            case java.awt.event.KeyEvent.VK_HIRAGANA:
                return KeyCode.HIRAGANA;

            case java.awt.event.KeyEvent.VK_HOME:
                return KeyCode.HOME;

            case java.awt.event.KeyEvent.VK_I:
                return KeyCode.I;

            case java.awt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF:
                return KeyCode.INPUT_METHOD_ON_OFF;

            case java.awt.event.KeyEvent.VK_INSERT:
                return KeyCode.INSERT;

            case java.awt.event.KeyEvent.VK_J:
                return KeyCode.J;

            case java.awt.event.KeyEvent.VK_JAPANESE_HIRAGANA:
                return KeyCode.JAPANESE_HIRAGANA;

            case java.awt.event.KeyEvent.VK_JAPANESE_KATAKANA:
                return KeyCode.JAPANESE_KATAKANA;

            case java.awt.event.KeyEvent.VK_JAPANESE_ROMAN:
                return KeyCode.JAPANESE_ROMAN;

            case java.awt.event.KeyEvent.VK_K:
                return KeyCode.K;

            case java.awt.event.KeyEvent.VK_KANA:
                return KeyCode.KANA;

            case java.awt.event.KeyEvent.VK_KANA_LOCK:
                return KeyCode.KANA_LOCK;

            case java.awt.event.KeyEvent.VK_KANJI:
                return KeyCode.KANJI;

            case java.awt.event.KeyEvent.VK_KATAKANA:
                return KeyCode.KATAKANA;

            case java.awt.event.KeyEvent.VK_KP_DOWN:
                return KeyCode.KP_DOWN;

            case java.awt.event.KeyEvent.VK_KP_LEFT:
                return KeyCode.KP_LEFT;

            case java.awt.event.KeyEvent.VK_KP_RIGHT:
                return KeyCode.KP_RIGHT;

            case java.awt.event.KeyEvent.VK_KP_UP:
                return KeyCode.KP_UP;

            case java.awt.event.KeyEvent.VK_L:
                return KeyCode.L;

            case java.awt.event.KeyEvent.VK_LEFT:
                return KeyCode.LEFT;

            case java.awt.event.KeyEvent.VK_LEFT_PARENTHESIS:
                return KeyCode.LEFT_PARENTHESIS;

            case java.awt.event.KeyEvent.VK_LESS:
                return KeyCode.LESS;

            case java.awt.event.KeyEvent.VK_M:
                return KeyCode.M;

            case java.awt.event.KeyEvent.VK_META:
                return KeyCode.META;

            case java.awt.event.KeyEvent.VK_MINUS:
                return KeyCode.MINUS;

            case java.awt.event.KeyEvent.VK_MODECHANGE:
                return KeyCode.MODECHANGE;

            case java.awt.event.KeyEvent.VK_MULTIPLY:
                return KeyCode.MULTIPLY;

            case java.awt.event.KeyEvent.VK_N:
                return KeyCode.N;

            case java.awt.event.KeyEvent.VK_NONCONVERT:
                return KeyCode.NONCONVERT;

            case java.awt.event.KeyEvent.VK_NUMBER_SIGN:
                return KeyCode.NUMBER_SIGN;

            case java.awt.event.KeyEvent.VK_NUMPAD0:
                return KeyCode.NUMPAD0;

            case java.awt.event.KeyEvent.VK_NUMPAD1:
                return KeyCode.NUMPAD1;

            case java.awt.event.KeyEvent.VK_NUMPAD2:
                return KeyCode.NUMPAD2;

            case java.awt.event.KeyEvent.VK_NUMPAD3:
                return KeyCode.NUMPAD3;

            case java.awt.event.KeyEvent.VK_NUMPAD4:
                return KeyCode.NUMPAD4;

            case java.awt.event.KeyEvent.VK_NUMPAD5:
                return KeyCode.NUMPAD5;

            case java.awt.event.KeyEvent.VK_NUMPAD6:
                return KeyCode.NUMPAD6;

            case java.awt.event.KeyEvent.VK_NUMPAD7:
                return KeyCode.NUMPAD7;

            case java.awt.event.KeyEvent.VK_NUMPAD8:
                return KeyCode.NUMPAD8;

            case java.awt.event.KeyEvent.VK_NUMPAD9:
                return KeyCode.NUMPAD9;

            case java.awt.event.KeyEvent.VK_NUM_LOCK:
                return KeyCode.NUM_LOCK;

            case java.awt.event.KeyEvent.VK_O:
                return KeyCode.O;

            case java.awt.event.KeyEvent.VK_OPEN_BRACKET:
                return KeyCode.OPEN_BRACKET;

            case java.awt.event.KeyEvent.VK_P:
                return KeyCode.P;

            case java.awt.event.KeyEvent.VK_PAGE_DOWN:
                return KeyCode.PAGE_DOWN;

            case java.awt.event.KeyEvent.VK_PAGE_UP:
                return KeyCode.PAGE_UP;

            case java.awt.event.KeyEvent.VK_PASTE:
                return KeyCode.PASTE;

            case java.awt.event.KeyEvent.VK_PAUSE:
                return KeyCode.PAUSE;

            case java.awt.event.KeyEvent.VK_PERIOD:
                return KeyCode.PERIOD;

            case java.awt.event.KeyEvent.VK_PLUS:
                return KeyCode.PLUS;

            case java.awt.event.KeyEvent.VK_PREVIOUS_CANDIDATE:
                return KeyCode.PREVIOUS_CANDIDATE;

            case java.awt.event.KeyEvent.VK_PRINTSCREEN:
                return KeyCode.PRINTSCREEN;

            case java.awt.event.KeyEvent.VK_PROPS:
                return KeyCode.PROPS;

            case java.awt.event.KeyEvent.VK_Q:
                return KeyCode.Q;

            case java.awt.event.KeyEvent.VK_QUOTE:
                return KeyCode.QUOTE;

            case java.awt.event.KeyEvent.VK_QUOTEDBL:
                return KeyCode.QUOTEDBL;

            case java.awt.event.KeyEvent.VK_R:
                return KeyCode.R;

            case java.awt.event.KeyEvent.VK_RIGHT:
                return KeyCode.RIGHT;

            case java.awt.event.KeyEvent.VK_RIGHT_PARENTHESIS:
                return KeyCode.RIGHT_PARENTHESIS;

            case java.awt.event.KeyEvent.VK_ROMAN_CHARACTERS:
                return KeyCode.ROMAN_CHARACTERS;

            case java.awt.event.KeyEvent.VK_S:
                return KeyCode.S;

            case java.awt.event.KeyEvent.VK_SCROLL_LOCK:
                return KeyCode.SCROLL_LOCK;

            case java.awt.event.KeyEvent.VK_SEMICOLON:
                return KeyCode.SEMICOLON;

            case java.awt.event.KeyEvent.VK_SEPARATOR:
                return KeyCode.SEPARATOR;

            case java.awt.event.KeyEvent.VK_SHIFT:
                return KeyCode.SHIFT;

            case java.awt.event.KeyEvent.VK_SLASH:
                return KeyCode.SLASH;

            case java.awt.event.KeyEvent.VK_STOP:
                return KeyCode.STOP;

            case java.awt.event.KeyEvent.VK_SUBTRACT:
                return KeyCode.SUBTRACT;

            case java.awt.event.KeyEvent.VK_T:
                return KeyCode.T;

            case java.awt.event.KeyEvent.VK_TAB:
                return KeyCode.TAB;

            case java.awt.event.KeyEvent.VK_U:
                return KeyCode.U;

            case java.awt.event.KeyEvent.VK_UNDERSCORE:
                return KeyCode.UNDERSCORE;

            case java.awt.event.KeyEvent.VK_UNDO:
                return KeyCode.UNDO;

            case java.awt.event.KeyEvent.VK_UP:
                return KeyCode.UP;

            case java.awt.event.KeyEvent.VK_V:
                return KeyCode.V;

            case java.awt.event.KeyEvent.VK_W:
                return KeyCode.W;

            case java.awt.event.KeyEvent.VK_WINDOWS:
                return KeyCode.WINDOWS;

            case java.awt.event.KeyEvent.VK_X:
                return KeyCode.X;

            case java.awt.event.KeyEvent.VK_Y:
                return KeyCode.Y;

            case java.awt.event.KeyEvent.VK_Z:
                return KeyCode.Z;

            default:
                return KeyCode.UNDEFINED;
        }
    }

    /**
     * convert to AWT color
     *
     * @param colorFX
     * @return AWT color
     */
    public static java.awt.Color getColorAWT(javafx.scene.paint.Color colorFX) {
        return new java.awt.Color((float) colorFX.getRed(), (float) colorFX.getGreen(), (float) colorFX.getBlue());
    }

    /**
     * convert to FX color
     *
     * @param colorAWT
     * @return AWT color
     */
    public static javafx.scene.paint.Color getColorFX(java.awt.Color colorAWT) {
        return getColorFX(colorAWT, 1);
    }

    /**
     * convert to FX color
     *
     * @param colorAWT
     * @return AWT color
     */
    public static javafx.scene.paint.Color getColorFX(java.awt.Color colorAWT, double opacity) {
        if (colorAWT == null)
            return null;
        else
            return new javafx.scene.paint.Color(colorAWT.getRed() / 255.0, colorAWT.getGreen() / 255.0, colorAWT.getBlue() / 255.0, opacity);
    }

    public static java.awt.geom.Point2D asAWTPoint2D(Point2D center) {
        return new java.awt.geom.Point2D.Double(center.getX(), center.getY());
    }
}
