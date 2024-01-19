/*
 * AuxBlocksHeaderRMA3.java Copyright (C) 2024 Daniel H. Huson
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
package megan.rma3;

import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.IOException;

/**
 * Format description for an aux-block entry
 * Created by huson on 5/16/14.
 */
public class AuxBlocksHeaderRMA3 extends BaseRMA3 {

    /**
     * constructor
     */
    public AuxBlocksHeaderRMA3(boolean DEAD) {
        super("Name:String Data:Bytes");
    }

    @Override
    public void read(IInputReader reader, long startPos) throws IOException {
        setFormatDef(reader.readString());
    }

    @Override
    public void write(IOutputWriter writer) throws IOException {
        writer.writeString(getFormatDef());
    }
}
