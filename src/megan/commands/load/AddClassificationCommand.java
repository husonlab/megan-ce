/*
 * AddClassificationCommand.java Copyright (C) 2022. Daniel H. Huson
 *
 *  No usage, copying or distribution without explicit permission.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */
package megan.commands.load;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.CommandManager;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.FileUtils;
import jloda.util.ProgramProperties;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * add classification command
 * Daniel Huson, 4.2021
 */
public class AddClassificationCommand extends CommandBase implements ICommand {
	public AddClassificationCommand() {
	}

	/**
	 * constructor
	 */
	public AddClassificationCommand(CommandManager commandManager) {
		super(commandManager);
	}

	/**
	 * parses the given command and executes it
	 *
	 * @param np
	 * @throws java.io.IOException
	 */
	@Override
	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase("addClassification file=");
		var treeFileName = np.getWordFileNamePunctuation();
		np.matchWordIgnoreCase(";");
		if (!(treeFileName.endsWith(".tre")))
			throw new IOException("addClassification: file must have suffix .tre");
		if (!FileUtils.fileExistsAndIsNonEmpty(treeFileName))
			throw new IOException("addClassification: specified .tre file does not exist or is empty");
		var mapFile = FileUtils.replaceFileSuffixKeepGZ(treeFileName, ".map");
		if (!FileUtils.fileExistsAndIsNonEmpty(mapFile))
			throw new IOException("addClassification: corresponding .map file does not exist or is empty");

		var addedClassificationFiles = Arrays.asList(ProgramProperties.get(MeganProperties.ADDITION_CLASSIFICATION_FILES, new String[0]));

		var treeFile = new File(treeFileName);
		var found = false;
		for (var other : addedClassificationFiles) {
			if (treeFile.equals(new File(other))) {
				found = true;
				NotificationsInSwing.showWarning("Classification has already been added, no action will be taken");
				break;
			}
		}
		if (!found) {
			addedClassificationFiles = new ArrayList<>(addedClassificationFiles);
			addedClassificationFiles.add(0, treeFileName);
			ProgramProperties.put(MeganProperties.ADDITION_CLASSIFICATION_FILES, addedClassificationFiles.toArray(new String[0]));
			System.err.println("All added classification files: " + StringUtils.toString(addedClassificationFiles, ", "));
			NotificationsInSwing.showInformation("Classification file has been added, restart MEGAN to use");
		}
	}

	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return "Add Classification...";
	}

	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Adds a classification to MEGAN";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Import16.gif");
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
	 * action to be performed
	 *
	 * @param ev
	 */
	public void actionPerformed(ActionEvent ev) {
		var addedClassificationFiles = Arrays.asList(ProgramProperties.get(MeganProperties.ADDITION_CLASSIFICATION_FILES, new String[0]));

		var lastOpenFile = (addedClassificationFiles.size() > 0 ? new File(addedClassificationFiles.get(0)) : null);

		File file = ChooseFileDialog.chooseFileToOpen(getViewer().getFrame(), lastOpenFile, new TextFileFilter("tre", false), new TextFileFilter("tre", false), ev, "Open Classification Tree File");

		if (file != null) {
			executeImmediately("addClassification file='" + file.getPath() + "';");
		}
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
	 * is the command currently applicable? Used to set enable state of command
	 *
	 * @return true, if command can be applied
	 */
	public boolean isApplicable() {
		return true;
	}

	/**
	 * get command-line usage description
	 *
	 * @return usage
	 */
	@Override
	public String getSyntax() {
		return "addClassification file=<file-name>;";
	}
}
