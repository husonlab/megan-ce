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
package megan.parsers.sam;

/**
 * SAM alignment flag
 * Daniel Huson, DATE
 */
public class Flag {
    private final int value;
    /*
    0x1	template having multiple fragments in sequencing
0x2	each fragment properly aligned according to the aligner
0x4	fragment unmapped
0x8	next fragment in the template unmapped
0x10	SEQ being reverse complemented
0x20	SEQ of the next fragment in the template being reversed
0x40	the first fragment in the template
0x80	the last fragment in the template
0x100	secondary alignment
0x200	not passing quality controls
0x400	PCR or optical duplicate
     */

    public Flag(int value) {
        this.value = value;
    }

    public boolean isHasMate() {
        return (value & 0x1) != 0;
    }

    public boolean isUnmapped() {
        return (value & 0x4) != 0;
    }

    public boolean isReverseComplemented() {
        return (value & 0x10) != 0;
    }

    public boolean isFirstFragment() {
        return (value & 0x40) != 0;
    }

    public boolean isLastFragment() {
        return (value & 0x80) != 0;
    }

    public boolean isSecondaryAlignment() {
        return (value & 0x100) != 0;
    }

    public boolean isNotPassedQualityControl() {
        return (value & 0x200) != 0;
    }
}
