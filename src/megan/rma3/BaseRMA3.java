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
package megan.rma3;

import megan.io.IInputReader;
import megan.io.IOutputWriter;
import megan.io.OutputWriterHumanReadable;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Base class for data blocks used by RMA3 format
 * Created by huson on 5/16/14.
 */
public abstract class BaseRMA3 {
    private String formatDef;

    /**
     * constructor
     *
     * @param formatDef
     */
    public BaseRMA3(String formatDef) {
        setFormatDef(formatDef);
    }

    public String toString() {
        final IOutputWriter w = new OutputWriterHumanReadable(new StringWriter());
        try {
            write(w);
        } catch (IOException ignored) {
        }
        return w.toString();
    }

    abstract public void read(IInputReader reader, long startPos) throws IOException;

    protected abstract void write(IOutputWriter writer) throws IOException;

    String getFormatDef() {
        return formatDef;
    }

    void setFormatDef(String formatDef) {
        this.formatDef = formatDef;
    }
}
