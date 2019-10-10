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
package megan.importblast;

import jloda.swing.commands.CommandManager;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.commands.ICommand;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.commandtemplates.*;
import megan.importblast.commands.*;

import javax.swing.*;
import java.awt.*;

/**
 * panel for setting functional-viewer related stuff
 * Daniel Huson, 12.2012, 4.2015
 */
public class ViewerPanel extends JPanel {
    /**
     * construct the  panel
     *
     * @param commandManager
     * @param cName
     */
    public ViewerPanel(CommandManager commandManager, String cName) {
        setLayout(new BorderLayout());

        final JPanel centerPanel = new JPanel();
        centerPanel.setBorder(BorderFactory.createTitledBorder(cName + " analysis settings"));
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        {
            final JPanel aPanel = new JPanel();
            JCheckBox checkBox = (JCheckBox) commandManager.getButton(SetAnalyse4ViewerCommand.getName(cName));
            if (checkBox.isSelected() && !ClassificationManager.get(cName, true).getIdMapper().hasActiveAndLoaded())
                checkBox.setSelected(false);
            aPanel.add(checkBox);
            centerPanel.add(aPanel);
        }

        final JPanel panel1 = new JPanel();
        panel1.setBorder(BorderFactory.createTitledBorder("How MEGAN identifies " + cName + " classes"));
        panel1.setLayout(new BorderLayout());

        final JPanel aPanel = new JPanel(new GridLayout(6, 1));
        
        {
            final ICommand useFastMode = commandManager.getCommand(SetFastModeCommand.NAME);
            final AbstractButton useFastModeButton = commandManager.getButton(useFastMode);
            aPanel.add(useFastModeButton);

            final ICommand useExtendedMode = commandManager.getCommand(SetExtendedModeCommand.NAME);
            final AbstractButton usedExtendedModeButton = commandManager.getButton(useExtendedMode);
            aPanel.add(usedExtendedModeButton);
        }

        final ICommand useMapDBCommand = commandManager.getCommand(SetUseMapType4ViewerCommand.getAltName(cName, IdMapper.MapType.MeganMapDB));
        final AbstractButton useMapDBButton = commandManager.getButton(useMapDBCommand);
        if (useMapDBCommand instanceof ICheckBoxCommand)
            useMapDBButton.setSelected(((ICheckBoxCommand) useMapDBCommand).isSelected());
        useMapDBButton.setEnabled(useMapDBCommand.isApplicable());
        aPanel.add(useMapDBButton);

        {
            final JPanel butPanel = new JPanel();
            butPanel.setLayout(new BoxLayout(butPanel, BoxLayout.X_AXIS));
            butPanel.add(commandManager.getButton(LoadMappingFile4ViewerCommand.getAltName(cName, IdMapper.MapType.MeganMapDB)));
            butPanel.add(new JLabel(" " + LoadMappingFile4ViewerCommand.getName(cName, IdMapper.MapType.MeganMapDB)));
            butPanel.add(Box.createHorizontalGlue());
            aPanel.add(butPanel);
        }

        final ICommand useAccessionLookupCommand = commandManager.getCommand(SetUseMapType4ViewerCommand.getAltName(cName, IdMapper.MapType.Accession));
        final AbstractButton ueseAccessionButton = commandManager.getButton(useAccessionLookupCommand);
        if (useAccessionLookupCommand instanceof ICheckBoxCommand)
            ueseAccessionButton.setSelected(((ICheckBoxCommand) useAccessionLookupCommand).isSelected());
        ueseAccessionButton.setEnabled(useAccessionLookupCommand.isApplicable());
        aPanel.add(ueseAccessionButton);

        {
            final JPanel butPanel = new JPanel();
            butPanel.setLayout(new BoxLayout(butPanel, BoxLayout.X_AXIS));
            butPanel.add(commandManager.getButton(LoadMappingFile4ViewerCommand.getAltName(cName, IdMapper.MapType.Accession)));
            butPanel.add(new JLabel(" " + LoadMappingFile4ViewerCommand.getName(cName, IdMapper.MapType.Accession)));
            butPanel.add(Box.createHorizontalGlue());
            aPanel.add(butPanel);
        }


        final ICommand useSynonymsCommand = commandManager.getCommand(SetUseMapType4ViewerCommand.getAltName(cName, IdMapper.MapType.Synonyms));
        final AbstractButton useSynomymsButton = commandManager.getButton(useSynonymsCommand);
        if (useSynonymsCommand instanceof ICheckBoxCommand)
            useSynomymsButton.setSelected(((ICheckBoxCommand) useSynonymsCommand).isSelected());
        useSynomymsButton.setEnabled(useSynonymsCommand.isApplicable());
        aPanel.add(useSynomymsButton);

        {
            final JPanel butPanel = new JPanel();
            butPanel.setLayout(new BoxLayout(butPanel, BoxLayout.X_AXIS));
            butPanel.add(commandManager.getButton(LoadMappingFile4ViewerCommand.getAltName(cName, IdMapper.MapType.Synonyms)));
            butPanel.add(new JLabel(" " + LoadMappingFile4ViewerCommand.getName(cName, IdMapper.MapType.Synonyms)));
            butPanel.add(Box.createHorizontalGlue());
            aPanel.add(butPanel);
        }

        final ICommand useIDParsingCommand = commandManager.getCommand(SetUseIdParsing4ViewerCommand.getAltName(cName));
        final AbstractButton useIdParsingButton = commandManager.getButton(useIDParsingCommand);
        if (useIdParsingButton instanceof ICheckBoxCommand)
            useIdParsingButton.setSelected(((ICheckBoxCommand) useIDParsingCommand).isSelected());
        useIdParsingButton.setEnabled(useIDParsingCommand.isApplicable());
        aPanel.add(useIdParsingButton);

        if (cName.equals(Classification.Taxonomy)) {
            AbstractButton useParseTextButton = commandManager.getButton(SetUseTextTaxonomyCommand.NAME);
            aPanel.add(useParseTextButton);

            {
                aPanel.add(commandManager.getButton(UseContaminantsFilterCommand.NAME));
                final JPanel butPanel = new JPanel();
                butPanel.setLayout(new BoxLayout(butPanel, BoxLayout.X_AXIS));
                butPanel.add(commandManager.getButton(ChooseContaminantsFileCommand.NAME));
                butPanel.add(new JLabel(" " + ChooseContaminantsFileCommand.NAME));
                butPanel.add(commandManager.getButton(ListContaminantsCommand.NAME));
                butPanel.add(Box.createHorizontalGlue());

                aPanel.add(butPanel);
            }

        }

        if (!ClassificationManager.getDefaultClassificationsList().contains(cName) && ClassificationManager.getAllSupportedClassifications().contains(cName)) {
            final ICommand useLCACommand = commandManager.getCommand(SetUseLCA4ViewerCommand.getAltName(cName));
            final AbstractButton useLCAButton = commandManager.getButton(useLCACommand);
            if (useLCAButton instanceof ICheckBoxCommand)
                useLCAButton.setSelected(((ICheckBoxCommand) useLCACommand).isSelected());
            useLCAButton.setEnabled(useLCACommand.isApplicable());
            aPanel.add(useLCAButton);
        }

        panel1.add(aPanel, BorderLayout.CENTER);

        centerPanel.add(panel1);
        add(centerPanel, BorderLayout.CENTER);
    }
}
