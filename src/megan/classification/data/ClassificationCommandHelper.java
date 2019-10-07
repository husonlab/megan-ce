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
package megan.classification.data;

import jloda.swing.commands.ICommand;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.commandtemplates.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * generates open viewer commands
 * Daniel Huson, 4.2015
 */
public class ClassificationCommandHelper {

    /**
     * gets the menu string for opening all registered viewers
     *
     * @return menu string
     */
    public static String getOpenViewerMenuString() {
        final StringBuilder buf = new StringBuilder();
        for (String name : ClassificationManager.getDefaultClassificationsListExcludingNCBITaxonomy()) {
            buf.append((new OpenFViewerCommand(name)).getName()).append(";");
        }
        for (String name : ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy()) {
            if (!ClassificationManager.getDefaultClassificationsListExcludingNCBITaxonomy().contains(name))
                buf.append((new OpenFViewerCommand(name)).getName()).append(";");
        }
        return buf.toString();
    }

    /**
     * get commands needed by all viewers
     *
     * @return list of globally used commands
     */
    public static Collection<ICommand> getGlobalCommands() {
        final List<ICommand> commands = new LinkedList<>();
        for (String name : ClassificationManager.getAllSupportedClassifications()) {
            commands.add(new OpenFViewerCommand(name));
        }
        commands.add(new SetUseMapTypeCommand());
        commands.add(new SetAnalyseCommand());
        commands.add(new LoadMappingFileCommand());
        commands.add(new SetUseIdParsingCommand());
        commands.add(new SetUseLCACommand());
        return commands;
    }

    /**
     * get all commands needed by the import blast dialog
     *
     * @return import blast commands
     */
    public static Collection<ICommand> getImportBlastCommands(Collection<String> cNames) {
        final List<ICommand> commands = new LinkedList<>();

        for (String cName : cNames) {
            commands.add(new SetAnalyse4ViewerCommand(cName));
            commands.add(new SetUseMapType4ViewerCommand(cName, IdMapper.MapType.Accession));
            commands.add(new LoadMappingFile4ViewerCommand(cNames,cName, IdMapper.MapType.Accession));
            commands.add(new SetUseMapType4ViewerCommand(cName, IdMapper.MapType.MeganMapDB));
            commands.add(new LoadMappingFile4ViewerCommand(cNames,cName, IdMapper.MapType.MeganMapDB));
            commands.add(new SetUseMapType4ViewerCommand(cName, IdMapper.MapType.Synonyms));
            commands.add(new LoadMappingFile4ViewerCommand(cNames,cName, IdMapper.MapType.Synonyms));
            commands.add(new SetUseIdParsing4ViewerCommand(cName));
            commands.add(new SetUseLCA4ViewerCommand(cName));
        }
        return commands;
    }
}
