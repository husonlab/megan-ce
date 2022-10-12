/*
 * FXSwingUtilities.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
     * @return KeyCode
     */
    public static KeyCode getKeyCodeFX(int awtKeyCode) {
        return switch (awtKeyCode) {
            case java.awt.event.KeyEvent.VK_A -> KeyCode.A;
            case java.awt.event.KeyEvent.VK_ACCEPT -> KeyCode.ACCEPT;
            case java.awt.event.KeyEvent.VK_ADD -> KeyCode.ADD;
            case java.awt.event.KeyEvent.VK_AGAIN -> KeyCode.AGAIN;
            case java.awt.event.KeyEvent.VK_ALL_CANDIDATES -> KeyCode.ALL_CANDIDATES;
            case java.awt.event.KeyEvent.VK_ALPHANUMERIC -> KeyCode.ALPHANUMERIC;
            case java.awt.event.KeyEvent.VK_ALT -> KeyCode.ALT;
            case java.awt.event.KeyEvent.VK_ALT_GRAPH -> KeyCode.ALT_GRAPH;
            case java.awt.event.KeyEvent.VK_AMPERSAND -> KeyCode.AMPERSAND;
            case java.awt.event.KeyEvent.VK_ASTERISK -> KeyCode.ASTERISK;
            case java.awt.event.KeyEvent.VK_AT -> KeyCode.AT;
            case java.awt.event.KeyEvent.VK_B -> KeyCode.B;
            case java.awt.event.KeyEvent.VK_BACK_QUOTE -> KeyCode.BACK_QUOTE;
            case java.awt.event.KeyEvent.VK_BACK_SLASH -> KeyCode.BACK_SLASH;
            case java.awt.event.KeyEvent.VK_BACK_SPACE -> KeyCode.BACK_SPACE;
            case java.awt.event.KeyEvent.VK_BEGIN -> KeyCode.BEGIN;
            case java.awt.event.KeyEvent.VK_BRACELEFT -> KeyCode.BRACELEFT;
            case java.awt.event.KeyEvent.VK_BRACERIGHT -> KeyCode.BRACERIGHT;
            case java.awt.event.KeyEvent.VK_C -> KeyCode.C;
            case java.awt.event.KeyEvent.VK_CANCEL -> KeyCode.CANCEL;
            case java.awt.event.KeyEvent.VK_CAPS_LOCK -> KeyCode.CAPS;
            case java.awt.event.KeyEvent.VK_CIRCUMFLEX -> KeyCode.CIRCUMFLEX;
            case java.awt.event.KeyEvent.VK_CLEAR -> KeyCode.CLEAR;
            case java.awt.event.KeyEvent.VK_CLOSE_BRACKET -> KeyCode.CLOSE_BRACKET;
            case java.awt.event.KeyEvent.VK_CODE_INPUT -> KeyCode.CODE_INPUT;
            case java.awt.event.KeyEvent.VK_COLON -> KeyCode.COLON;
            case java.awt.event.KeyEvent.VK_COMMA -> KeyCode.COMMA;
            case java.awt.event.KeyEvent.VK_COMPOSE -> KeyCode.COMPOSE;
            case java.awt.event.KeyEvent.VK_CONTEXT_MENU -> KeyCode.CONTEXT_MENU;
            case java.awt.event.KeyEvent.VK_CONTROL -> KeyCode.CONTROL;
            case java.awt.event.KeyEvent.VK_CONVERT -> KeyCode.CONVERT;
            case java.awt.event.KeyEvent.VK_COPY -> KeyCode.COPY;
            case java.awt.event.KeyEvent.VK_CUT -> KeyCode.CUT;
            case java.awt.event.KeyEvent.VK_D -> KeyCode.D;
            case java.awt.event.KeyEvent.VK_DEAD_ABOVEDOT -> KeyCode.DEAD_ABOVEDOT;
            case java.awt.event.KeyEvent.VK_DEAD_ABOVERING -> KeyCode.DEAD_ABOVERING;
            case java.awt.event.KeyEvent.VK_DEAD_ACUTE -> KeyCode.DEAD_ACUTE;
            case java.awt.event.KeyEvent.VK_DEAD_BREVE -> KeyCode.DEAD_BREVE;
            case java.awt.event.KeyEvent.VK_DEAD_CARON -> KeyCode.DEAD_CARON;
            case java.awt.event.KeyEvent.VK_DEAD_CEDILLA -> KeyCode.DEAD_CEDILLA;
            case java.awt.event.KeyEvent.VK_DEAD_CIRCUMFLEX -> KeyCode.DEAD_CIRCUMFLEX;
            case java.awt.event.KeyEvent.VK_DEAD_DIAERESIS -> KeyCode.DEAD_DIAERESIS;
            case java.awt.event.KeyEvent.VK_DEAD_DOUBLEACUTE -> KeyCode.DEAD_DOUBLEACUTE;
            case java.awt.event.KeyEvent.VK_DEAD_GRAVE -> KeyCode.DEAD_GRAVE;
            case java.awt.event.KeyEvent.VK_DEAD_IOTA -> KeyCode.DEAD_IOTA;
            case java.awt.event.KeyEvent.VK_DEAD_MACRON -> KeyCode.DEAD_MACRON;
            case java.awt.event.KeyEvent.VK_DEAD_OGONEK -> KeyCode.DEAD_OGONEK;
            case java.awt.event.KeyEvent.VK_DEAD_SEMIVOICED_SOUND -> KeyCode.DEAD_SEMIVOICED_SOUND;
            case java.awt.event.KeyEvent.VK_DEAD_TILDE -> KeyCode.DEAD_TILDE;
            case java.awt.event.KeyEvent.VK_DEAD_VOICED_SOUND -> KeyCode.DEAD_VOICED_SOUND;
            case java.awt.event.KeyEvent.VK_DECIMAL -> KeyCode.DECIMAL;
            case java.awt.event.KeyEvent.VK_DELETE -> KeyCode.DELETE;
            case java.awt.event.KeyEvent.VK_0 -> KeyCode.DIGIT0;
            case java.awt.event.KeyEvent.VK_1 -> KeyCode.DIGIT1;
            case java.awt.event.KeyEvent.VK_2 -> KeyCode.DIGIT2;
            case java.awt.event.KeyEvent.VK_3 -> KeyCode.DIGIT3;
            case java.awt.event.KeyEvent.VK_4 -> KeyCode.DIGIT4;
            case java.awt.event.KeyEvent.VK_5 -> KeyCode.DIGIT5;
            case java.awt.event.KeyEvent.VK_6 -> KeyCode.DIGIT6;
            case java.awt.event.KeyEvent.VK_7 -> KeyCode.DIGIT7;
            case java.awt.event.KeyEvent.VK_8 -> KeyCode.DIGIT8;
            case java.awt.event.KeyEvent.VK_9 -> KeyCode.DIGIT9;
            case java.awt.event.KeyEvent.VK_DIVIDE -> KeyCode.DIVIDE;
            case java.awt.event.KeyEvent.VK_DOLLAR -> KeyCode.DOLLAR;
            case java.awt.event.KeyEvent.VK_DOWN -> KeyCode.DOWN;
            case java.awt.event.KeyEvent.VK_E -> KeyCode.E;
            case java.awt.event.KeyEvent.VK_END -> KeyCode.END;
            case java.awt.event.KeyEvent.VK_ENTER -> KeyCode.ENTER;
            case java.awt.event.KeyEvent.VK_EQUALS -> KeyCode.EQUALS;
            case java.awt.event.KeyEvent.VK_ESCAPE -> KeyCode.ESCAPE;
            case java.awt.event.KeyEvent.VK_EURO_SIGN -> KeyCode.EURO_SIGN;
            case java.awt.event.KeyEvent.VK_EXCLAMATION_MARK -> KeyCode.EXCLAMATION_MARK;
            case java.awt.event.KeyEvent.VK_F -> KeyCode.F;
            case java.awt.event.KeyEvent.VK_F1 -> KeyCode.F1;
            case java.awt.event.KeyEvent.VK_F10 -> KeyCode.F10;
            case java.awt.event.KeyEvent.VK_F11 -> KeyCode.F11;
            case java.awt.event.KeyEvent.VK_F12 -> KeyCode.F12;
            case java.awt.event.KeyEvent.VK_F13 -> KeyCode.F13;
            case java.awt.event.KeyEvent.VK_F14 -> KeyCode.F14;
            case java.awt.event.KeyEvent.VK_F15 -> KeyCode.F15;
            case java.awt.event.KeyEvent.VK_F16 -> KeyCode.F16;
            case java.awt.event.KeyEvent.VK_F17 -> KeyCode.F17;
            case java.awt.event.KeyEvent.VK_F18 -> KeyCode.F18;
            case java.awt.event.KeyEvent.VK_F19 -> KeyCode.F19;
            case java.awt.event.KeyEvent.VK_F2 -> KeyCode.F2;
            case java.awt.event.KeyEvent.VK_F20 -> KeyCode.F20;
            case java.awt.event.KeyEvent.VK_F21 -> KeyCode.F21;
            case java.awt.event.KeyEvent.VK_F22 -> KeyCode.F22;
            case java.awt.event.KeyEvent.VK_F23 -> KeyCode.F23;
            case java.awt.event.KeyEvent.VK_F24 -> KeyCode.F24;
            case java.awt.event.KeyEvent.VK_F3 -> KeyCode.F3;
            case java.awt.event.KeyEvent.VK_F4 -> KeyCode.F4;
            case java.awt.event.KeyEvent.VK_F5 -> KeyCode.F5;
            case java.awt.event.KeyEvent.VK_F6 -> KeyCode.F6;
            case java.awt.event.KeyEvent.VK_F7 -> KeyCode.F7;
            case java.awt.event.KeyEvent.VK_F8 -> KeyCode.F8;
            case java.awt.event.KeyEvent.VK_F9 -> KeyCode.F9;
            case java.awt.event.KeyEvent.VK_FINAL -> KeyCode.FINAL;
            case java.awt.event.KeyEvent.VK_FIND -> KeyCode.FIND;
            case java.awt.event.KeyEvent.VK_FULL_WIDTH -> KeyCode.FULL_WIDTH;
            case java.awt.event.KeyEvent.VK_G -> KeyCode.G;
            case java.awt.event.KeyEvent.VK_GREATER -> KeyCode.GREATER;
            case java.awt.event.KeyEvent.VK_H -> KeyCode.H;
            case java.awt.event.KeyEvent.VK_HALF_WIDTH -> KeyCode.HALF_WIDTH;
            case java.awt.event.KeyEvent.VK_HELP -> KeyCode.HELP;
            case java.awt.event.KeyEvent.VK_HIRAGANA -> KeyCode.HIRAGANA;
            case java.awt.event.KeyEvent.VK_HOME -> KeyCode.HOME;
            case java.awt.event.KeyEvent.VK_I -> KeyCode.I;
            case java.awt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF -> KeyCode.INPUT_METHOD_ON_OFF;
            case java.awt.event.KeyEvent.VK_INSERT -> KeyCode.INSERT;
            case java.awt.event.KeyEvent.VK_J -> KeyCode.J;
            case java.awt.event.KeyEvent.VK_JAPANESE_HIRAGANA -> KeyCode.JAPANESE_HIRAGANA;
            case java.awt.event.KeyEvent.VK_JAPANESE_KATAKANA -> KeyCode.JAPANESE_KATAKANA;
            case java.awt.event.KeyEvent.VK_JAPANESE_ROMAN -> KeyCode.JAPANESE_ROMAN;
            case java.awt.event.KeyEvent.VK_K -> KeyCode.K;
            case java.awt.event.KeyEvent.VK_KANA -> KeyCode.KANA;
            case java.awt.event.KeyEvent.VK_KANA_LOCK -> KeyCode.KANA_LOCK;
            case java.awt.event.KeyEvent.VK_KANJI -> KeyCode.KANJI;
            case java.awt.event.KeyEvent.VK_KATAKANA -> KeyCode.KATAKANA;
            case java.awt.event.KeyEvent.VK_KP_DOWN -> KeyCode.KP_DOWN;
            case java.awt.event.KeyEvent.VK_KP_LEFT -> KeyCode.KP_LEFT;
            case java.awt.event.KeyEvent.VK_KP_RIGHT -> KeyCode.KP_RIGHT;
            case java.awt.event.KeyEvent.VK_KP_UP -> KeyCode.KP_UP;
            case java.awt.event.KeyEvent.VK_L -> KeyCode.L;
            case java.awt.event.KeyEvent.VK_LEFT -> KeyCode.LEFT;
            case java.awt.event.KeyEvent.VK_LEFT_PARENTHESIS -> KeyCode.LEFT_PARENTHESIS;
            case java.awt.event.KeyEvent.VK_LESS -> KeyCode.LESS;
            case java.awt.event.KeyEvent.VK_M -> KeyCode.M;
            case java.awt.event.KeyEvent.VK_META -> KeyCode.META;
            case java.awt.event.KeyEvent.VK_MINUS -> KeyCode.MINUS;
            case java.awt.event.KeyEvent.VK_MODECHANGE -> KeyCode.MODECHANGE;
            case java.awt.event.KeyEvent.VK_MULTIPLY -> KeyCode.MULTIPLY;
            case java.awt.event.KeyEvent.VK_N -> KeyCode.N;
            case java.awt.event.KeyEvent.VK_NONCONVERT -> KeyCode.NONCONVERT;
            case java.awt.event.KeyEvent.VK_NUMBER_SIGN -> KeyCode.NUMBER_SIGN;
            case java.awt.event.KeyEvent.VK_NUMPAD0 -> KeyCode.NUMPAD0;
            case java.awt.event.KeyEvent.VK_NUMPAD1 -> KeyCode.NUMPAD1;
            case java.awt.event.KeyEvent.VK_NUMPAD2 -> KeyCode.NUMPAD2;
            case java.awt.event.KeyEvent.VK_NUMPAD3 -> KeyCode.NUMPAD3;
            case java.awt.event.KeyEvent.VK_NUMPAD4 -> KeyCode.NUMPAD4;
            case java.awt.event.KeyEvent.VK_NUMPAD5 -> KeyCode.NUMPAD5;
            case java.awt.event.KeyEvent.VK_NUMPAD6 -> KeyCode.NUMPAD6;
            case java.awt.event.KeyEvent.VK_NUMPAD7 -> KeyCode.NUMPAD7;
            case java.awt.event.KeyEvent.VK_NUMPAD8 -> KeyCode.NUMPAD8;
            case java.awt.event.KeyEvent.VK_NUMPAD9 -> KeyCode.NUMPAD9;
            case java.awt.event.KeyEvent.VK_NUM_LOCK -> KeyCode.NUM_LOCK;
            case java.awt.event.KeyEvent.VK_O -> KeyCode.O;
            case java.awt.event.KeyEvent.VK_OPEN_BRACKET -> KeyCode.OPEN_BRACKET;
            case java.awt.event.KeyEvent.VK_P -> KeyCode.P;
            case java.awt.event.KeyEvent.VK_PAGE_DOWN -> KeyCode.PAGE_DOWN;
            case java.awt.event.KeyEvent.VK_PAGE_UP -> KeyCode.PAGE_UP;
            case java.awt.event.KeyEvent.VK_PASTE -> KeyCode.PASTE;
            case java.awt.event.KeyEvent.VK_PAUSE -> KeyCode.PAUSE;
            case java.awt.event.KeyEvent.VK_PERIOD -> KeyCode.PERIOD;
            case java.awt.event.KeyEvent.VK_PLUS -> KeyCode.PLUS;
            case java.awt.event.KeyEvent.VK_PREVIOUS_CANDIDATE -> KeyCode.PREVIOUS_CANDIDATE;
            case java.awt.event.KeyEvent.VK_PRINTSCREEN -> KeyCode.PRINTSCREEN;
            case java.awt.event.KeyEvent.VK_PROPS -> KeyCode.PROPS;
            case java.awt.event.KeyEvent.VK_Q -> KeyCode.Q;
            case java.awt.event.KeyEvent.VK_QUOTE -> KeyCode.QUOTE;
            case java.awt.event.KeyEvent.VK_QUOTEDBL -> KeyCode.QUOTEDBL;
            case java.awt.event.KeyEvent.VK_R -> KeyCode.R;
            case java.awt.event.KeyEvent.VK_RIGHT -> KeyCode.RIGHT;
            case java.awt.event.KeyEvent.VK_RIGHT_PARENTHESIS -> KeyCode.RIGHT_PARENTHESIS;
            case java.awt.event.KeyEvent.VK_ROMAN_CHARACTERS -> KeyCode.ROMAN_CHARACTERS;
            case java.awt.event.KeyEvent.VK_S -> KeyCode.S;
            case java.awt.event.KeyEvent.VK_SCROLL_LOCK -> KeyCode.SCROLL_LOCK;
            case java.awt.event.KeyEvent.VK_SEMICOLON -> KeyCode.SEMICOLON;
            case java.awt.event.KeyEvent.VK_SEPARATOR -> KeyCode.SEPARATOR;
            case java.awt.event.KeyEvent.VK_SHIFT -> KeyCode.SHIFT;
            case java.awt.event.KeyEvent.VK_SLASH -> KeyCode.SLASH;
            case java.awt.event.KeyEvent.VK_STOP -> KeyCode.STOP;
            case java.awt.event.KeyEvent.VK_SUBTRACT -> KeyCode.SUBTRACT;
            case java.awt.event.KeyEvent.VK_T -> KeyCode.T;
            case java.awt.event.KeyEvent.VK_TAB -> KeyCode.TAB;
            case java.awt.event.KeyEvent.VK_U -> KeyCode.U;
            case java.awt.event.KeyEvent.VK_UNDERSCORE -> KeyCode.UNDERSCORE;
            case java.awt.event.KeyEvent.VK_UNDO -> KeyCode.UNDO;
            case java.awt.event.KeyEvent.VK_UP -> KeyCode.UP;
            case java.awt.event.KeyEvent.VK_V -> KeyCode.V;
            case java.awt.event.KeyEvent.VK_W -> KeyCode.W;
            case java.awt.event.KeyEvent.VK_WINDOWS -> KeyCode.WINDOWS;
            case java.awt.event.KeyEvent.VK_X -> KeyCode.X;
            case java.awt.event.KeyEvent.VK_Y -> KeyCode.Y;
            case java.awt.event.KeyEvent.VK_Z -> KeyCode.Z;
            default -> KeyCode.UNDEFINED;
        };
    }

    /**
     * convert to AWT color
     *
     * @return AWT color
     */
    public static java.awt.Color getColorAWT(javafx.scene.paint.Color colorFX) {
        return new java.awt.Color((float) colorFX.getRed(), (float) colorFX.getGreen(), (float) colorFX.getBlue());
    }

    /**
     * convert to FX color
     *
     * @return AWT color
     */
    public static javafx.scene.paint.Color getColorFX(java.awt.Color colorAWT) {
        return getColorFX(colorAWT, 1);
    }

    /**
     * convert to FX color
     *
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
