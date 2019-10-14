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
package megan.stats;

import jloda.util.CanceledException;
import jloda.util.ProgressListener;

import javax.swing.*;
import java.io.IOException;
import java.util.Map;

/**
 * an item that manages a statistical method
 * Daniel Huson, 2.2008
 */
interface IMethodItem {
    String NAME = "";

    JPanel getOptionsPanel();

    boolean isApplicable();

    String getOptionsString();

    String getName();

    void parseOptionString(String string) throws IOException;

    void setInput(Map<Integer, Float> m1, Map<Integer, Float> m2);

    void apply(ProgressListener progressListener) throws CanceledException;

    Map<Integer, Double> getOutput();

    boolean getOptionUseUnassigned();

    void setOptionUseUnassigned(boolean optionUseHoHits);

    boolean getOptionUseInternal();

    void setOptionUseInternal(boolean optionUseInternal);
}
