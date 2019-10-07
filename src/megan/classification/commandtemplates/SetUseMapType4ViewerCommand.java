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
package megan.classification.commandtemplates;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Set maptype to use for specific fViewer
 * Daniel Huson, 4.2015
 */
public class SetUseMapType4ViewerCommand extends CommandBase implements ICheckBoxCommand {
    final private String cName;
    final private IdMapper.MapType mapType;

    /**
     * constructor
     *
     * @param cName
     * @param mapType
     */
    public SetUseMapType4ViewerCommand(String cName, IdMapper.MapType mapType) {
        this.cName = cName;
        this.mapType = mapType;
    }

    /**
     * is selected?
     */
    @Override
    public boolean isSelected() {
        if(ClassificationManager.isUseFastAccessionMappingMode() && mapType== IdMapper.MapType.MeganMapDB)
            return ClassificationManager.canUseMeganMapDBFile();
        else
            return ClassificationManager.isActiveMapper(cName, mapType);
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("use mapType=" + mapType + " cName=" + cName + " state=" + (!isSelected()) + ";");
    }

    public boolean isApplicable() {
        return ClassificationManager.isLoaded(cName, mapType) && !ClassificationManager.isUseFastAccessionMappingMode();
    }

    public static String getAltName(String cName, IdMapper.MapType mapType) {
        return "Use " + mapType + " For " + cName;
    }

    public String getAltName() {
        return getAltName(cName, mapType);
    }

    public String getName() {
        return "Use " + mapType;
    }

    public String getDescription() {
        return "Use " + mapType + " map to identify classes when parsing " + cName + " data";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
