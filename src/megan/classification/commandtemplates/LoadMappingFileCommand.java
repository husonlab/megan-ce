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
import jloda.swing.commands.ICommand;
import jloda.swing.util.ProgressDialog;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ProgressListener;
import jloda.util.ProgressPercentage;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * loads a mapping file for the given fViewer and mapType
 * Daniel Huson, 3.2014
 */
public class LoadMappingFileCommand extends CommandBase implements ICommand {
    /**
     * commandline syntax
     *
     * @return
     */
    @Override
    public String getSyntax() {
        return "load mapFile=<filename> mapType=<" + Basic.toString(IdMapper.MapType.values(), "|") + "> cName=<" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "> [parseTaxonNames={false|true}];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("load mapFile=");
        final String fileName = np.getWordFileNamePunctuation();
        np.matchIgnoreCase("mapType=");
        final IdMapper.MapType mapType = IdMapper.MapType.valueOf(np.getWordMatchesRespectingCase(Basic.toString(IdMapper.MapType.values(), " ")));
        np.matchIgnoreCase("cName=");
        final String cName = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        final boolean parseTaxonName;
        if (np.peekMatchIgnoreCase("parseTaxonNames")) {
            np.matchIgnoreCase("parseTaxonNames=");
            parseTaxonName = np.getBoolean();
            if (parseTaxonName) {
                if (!cName.equals(Classification.Taxonomy))
                    System.err.println("Warning: load mapFile: cName=" + cName + " is not Taxonomy, ignoring 'parseTaxonNames=true'");
            }
        } else
            parseTaxonName = false;
        np.matchIgnoreCase(";");

        try {
            final Classification classification = ClassificationManager.get(cName, true);
            classification.getIdMapper().setUseTextParsing(parseTaxonName);
            ProgressListener progressListener;
            if (ProgramProperties.isUseGUI())
                progressListener = new ProgressDialog("Loading file", "", (Component) getParent());
            else
                progressListener = new ProgressPercentage();
            try {
                final IdMapper mapper = classification.getIdMapper();
                mapper.loadMappingFile(fileName, mapType, true, progressListener);

            } finally {
                progressListener.close();
            }
            if (getParent() instanceof ImportBlastDialog) {
                ((ImportBlastDialog) getParent()).getCommandManager().execute("use cViewer=" + cName + " state=true;");
            }
            ProgramProperties.put(ClassificationManager.getMapFileKey(cName, mapType), fileName);
        } catch (Exception ex) {
            Basic.caught(ex);
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }


    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Load Mapping File";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Loads a mapping file";
    }
}
