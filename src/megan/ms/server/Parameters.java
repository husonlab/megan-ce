/*
 * Copyright (C) 2020. Daniel H. Huson
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

package megan.ms.server;

import jloda.util.Basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parameters {
    public static boolean matchIgnoreCase(String[] parameters, String key, String value) {
        for (String param : parameters) {
            final String[] tokens = Basic.split(param, '=');
            if (tokens.length == 2 && tokens[0].equalsIgnoreCase(key) && Basic.contains(Basic.split(tokens[1].toLowerCase(), ','), value.toLowerCase()))
                return true;
        }
        return false;
    }

    public static String getValue(String[] parameters, String key) {
        for (String param : parameters) {
            final String[] tokens = Basic.split(param, '=');
            if (tokens.length == 2 && tokens[0].equalsIgnoreCase(key))
                return tokens[1];
        }
        return null;
    }


    public static boolean getValue(String[] parameters, String key, boolean defaultValue) {
        for (String param : parameters) {
            final String[] tokens = Basic.split(param, '=');
            if (tokens.length == 2 && tokens[0].equalsIgnoreCase(key) && Basic.isBoolean(tokens[1]))
                return Basic.parseBoolean(tokens[1]);
        }
        return defaultValue;
    }

    public static int getValue(String[] parameters, String key, int defaultValue) {
        for (String param : parameters) {
            final String[] tokens = Basic.split(param, '=');
            if (tokens.length == 2 && tokens[0].equalsIgnoreCase(key) && Basic.isInteger(tokens[1]))
                return Basic.parseInt(tokens[1]);
        }
        return defaultValue;
    }

    public static long getValue(String[] parameters, String key, long defaultValue) {
        for (String param : parameters) {
            final String[] tokens = Basic.split(param, '=');
            if (tokens.length == 2 && tokens[0].equalsIgnoreCase(key) && Basic.isLong(tokens[1]))
                return Basic.parseLong(tokens[1]);
        }
        return defaultValue;
    }

    public static double getValue(String[] parameters, String key, double defaultValue) {
        for (String param : parameters) {
            final String[] tokens = Basic.split(param, '=');
            if (tokens.length == 2 && tokens[0].equalsIgnoreCase(key) && Basic.isDouble(tokens[1]))
                return Basic.parseDouble(tokens[1]);
        }
        return defaultValue;
    }

    public static String[] getValues(String[] parameters, String key) {
        final ArrayList<String> values = new ArrayList<>();
        for (String param : parameters) {
            final String[] tokens = Basic.split(param, '=');
            if (tokens.length == 2 && tokens[0].equalsIgnoreCase(key)) {
                values.addAll(Arrays.asList(Basic.split(tokens[1], ',')));
            }
        }
        return values.toArray(new String[0]);
    }

    public static boolean hasKey(String[] parameters, String key) {
        for (String param : parameters) {
            final String[] tokens = Basic.split(param, '=');
            if (tokens.length == 2 && tokens[0].equalsIgnoreCase(key))
                return true;
        }
        return false;
    }

    public static List<Integer> getIntValues(String[] parameters, String key) {
        final ArrayList<Integer> list = new ArrayList<>();
        final String[] values = getValues(parameters, key);
        for (String value : values) {
            if (Basic.isInteger(value))
                list.add(Basic.parseInt(value));
        }
        return list;
    }
}
