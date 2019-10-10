/*
 * SetAccessionMappingModeCommand.java Copyright (C) 2019. Daniel H. Huson
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

package megan.importblast.commands;

import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * set the fast mapping mode
 * daniel Huson, 9.2019
 */
public class SetFastModeCommand extends CommandBase implements ICheckBoxCommand {

    @Override
    public boolean isSelected() {
        return ClassificationManager.isUseFastAccessionMappingMode();
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set accessionMapMode=");
        final boolean fast=np.getWordMatchesIgnoringCase("fast extended").equalsIgnoreCase("fast");
        np.matchIgnoreCase(";");
        ClassificationManager.setUseFastAccessionMappingMode(fast);
        if(fast) {
            for(String cName:ClassificationManager.getAllSupportedClassifications()) {
                executeImmediately("use mapType=" + IdMapper.MapType.Accession + " cName=" + cName + " state=false;");
                executeImmediately("use mapType=" + IdMapper.MapType.Synonyms + " cName=" + cName + " state=false;");
                executeImmediately("set idParsing=false cName=" + cName + ";");
            }
            executeImmediately("set useParseTextTaxonomy=false;");
        }
        executeImmediately("update;");
    }

    public String getSyntax() {
        return "set accessionMapMode={extended|fast};";
    }

    public void actionPerformed(ActionEvent event) {
        execute("set accessionMapMode=fast;");

    }

    public static final String NAME="Fast Mode";

    public String getName() {
        return NAME;
    }


    public String getDescription() {
        return "Use fast accession mapping mode, only bulk mapping of the first word in each reference header.\nUse MEGAN mapping db file, no other mapping files or options.";
    }


    public ImageIcon getIcon() {
        return null;
    }


    public boolean isCritical() {
        return true;
    }

    public boolean isApplicable() {
        return ClassificationManager.getMeganMapDBFile()!=null;
    }


}
