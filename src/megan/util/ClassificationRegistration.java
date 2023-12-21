/*
 * ClassificationRegistration.java Copyright (C) 2023 Daniel H. Huson
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
package megan.util;

import jloda.swing.util.ProgramProperties;
import jloda.util.Basic;
import jloda.util.FileUtils;
import jloda.util.ResourceUtils;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.main.Megan6;
import megan.main.MeganProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * registers all available classifications
 * Daniel Huson, 5.2015
 */
public class ClassificationRegistration {
	/**
	 * register all known classifications
	 */
	public static void register(boolean verbose) {
		try {
			final Set<String> treeFiles = Arrays.stream(ResourceUtils.fetchResources(Megan6.class, "megan/resources/files")).filter(s -> s.endsWith(".tre")).collect(Collectors.toSet());
			final Set<String> mapFiles = Arrays.stream(ResourceUtils.fetchResources(Megan6.class, "megan/resources/files")).filter(s -> s.endsWith(".map")).collect(Collectors.toSet());

			// process added classifications:
			{
				var classificationFiles = ProgramProperties.get(MeganProperties.ADDITION_CLASSIFICATION_FILES, new String[0]);
				var existingFiles = new ArrayList<String>();
				for (var fileLocation : classificationFiles) {
					var treeFile = new File(fileLocation);
					var mapFile = new File(FileUtils.replaceFileSuffix(fileLocation, ".map"));
					if (FileUtils.fileExistsAndIsNonEmpty(treeFile) && FileUtils.fileExistsAndIsNonEmpty(mapFile)) {
						var classificationName = FileUtils.replaceFileSuffix(treeFile.getName(), "").toUpperCase();
						ClassificationManager.getAdditionalClassificationName2TreeFile().put(classificationName, treeFile.getPath());
						ClassificationManager.getAdditionalClassificationName2MapFile().put(classificationName, mapFile.getPath());
						treeFiles.add(treeFile.getName());
						mapFiles.add(mapFile.getName());
						existingFiles.add(fileLocation);
						if(verbose)
							System.err.println("Loading additional classification " + classificationName + " from: " + treeFile + " and " + mapFile);
					}
				}
				if (existingFiles.size() < classificationFiles.length) {
					ProgramProperties.put(MeganProperties.ADDITION_CLASSIFICATION_FILES, existingFiles.toArray(new String[0]));
				}
			}

			for (String treeFile : treeFiles) {
				if (mapFiles.contains(FileUtils.replaceFileSuffix(treeFile, ".map"))) {
					String nameUpperCase = FileUtils.replaceFileSuffix(treeFile, "").toUpperCase();
					if (nameUpperCase.equals("NCBI")) {
						ClassificationManager.getAllSupportedClassifications().add(Classification.Taxonomy);
					} else {
						ClassificationManager.getAllSupportedClassifications().add(nameUpperCase);
						ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy().add(nameUpperCase);
					}
				}
			}
		} catch (IOException ex) {
			Basic.caught(ex);
		}
	}

}
