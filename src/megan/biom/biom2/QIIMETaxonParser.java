/*
 *  Copyright (C) 2015 Daniel H. Huson
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

package megan.biom.biom2;

import megan.classification.IdMapper;
import megan.viewer.TaxonomyData;

public class QIIMETaxonParser {
    /**
     * determines the taxon-id associated with a QIIME generated taxon path, which looks something like this:
     * k__Bacteria;p__Proteobacteria;c__Gammaproteobacteria;o__Enterobacteriales;f__Enterobacteriaceae;g__Escherichia;s__
     *
     * @param taxonPath
     * @param ignorePathAbove just used last assignable, ignoring whether the path above matches
     * @return NCBI taxon id
     */
    public static int parseTaxon(String[] taxonPath, boolean ignorePathAbove) {
        int bestId = IdMapper.UNASSIGNED_ID;

        String genus = null;

        for (String name : taxonPath) {
            if (name.indexOf("__") == 1) {
                if (name.startsWith("g")) {
                    genus = name.substring(3);
                    name = name.substring(3);
                } else if (name.startsWith("s") && genus != null)
                    name = genus + " " + name.substring(3);
                else
                    name = name.substring(3);
            }
            if (name.startsWith("[") && name.endsWith("]") || name.startsWith("(") && name.endsWith(")"))
                name = name.substring(1, name.length() - 1);
            name = name.replaceAll("_", " ");

            if (name.equals("Root"))
                name = "root";

            if (name.length() > 0) {
                final int taxonId = TaxonomyData.getName2IdMap().get(name);
                if (taxonId > 0 && (bestId == IdMapper.UNASSIGNED_ID || ignorePathAbove || TaxonomyData.getTree().isDescendant(bestId, taxonId)))
                    bestId = taxonId;

            }
        }
        return bestId;
    }
}
