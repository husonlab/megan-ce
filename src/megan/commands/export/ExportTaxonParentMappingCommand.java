/*
 * ExportTaxonParentMappingCommand.java Copyright (C) 2022 Daniel H. Huson
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

package megan.commands.export;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.ProgramProperties;
import jloda.util.Single;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * exports the parent to node mapping
 * Daniel Huson. 7.2021
 */
public class ExportTaxonParentMappingCommand extends CommandBase implements ICommand {
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=taxonParentMap file=");
        var file=np.getWordFileNamePunctuation();
        boolean top;
        if(np.peekMatchIgnoreCase("toTop")) {
            np.matchIgnoreCase("toTop=");
            top=np.getBoolean();
        }
        else
            top=true;
        boolean names;
        if(np.peekMatchIgnoreCase("names")) {
            np.matchIgnoreCase("names=");
            names=np.getBoolean();
        }
        else
            names=false;
        np.matchIgnoreCase(";");

        var exception=new Single<Exception>();

        var progress=((Director)getDir()).getDocument().getProgressListener();
        progress.setTasks("Export","Taxon-parent mapping");
        progress.setProgress(0);
        progress.setMaximum(TaxonomyData.getTree().getNumberOfNodes());
        try(var writer=new BufferedWriter(new FileWriter(file))) {
            var root=TaxonomyData.getTree().getRoot();
            var cellularOrganisms=131567;
            var topNode=new Single<>(root);

            TaxonomyData.getTree().preorderTraversal(v -> {
				if (exception.isNull() && v.getParent() != null) {
					try {
						progress.incrementProgress();
						var id = (int) v.getInfo();
						int other;
						if (top) {
							if (v.getParent() == root || (int) v.getParent().getInfo() == cellularOrganisms)
								topNode.set(v);
							other = (int) topNode.get().getInfo();
						} else {
                            other = (int) v.getParent().getInfo();
                        }
                        if(names)
                            writer.write(String.format("%s\t%s%n", TaxonomyData.getName2IdMap().get(id), TaxonomyData.getName2IdMap().get(other)));
                        writer.write(String.format("%d\t%d%n", id, other));
                    }
                    catch(IOException ex) {
                        exception.setIfCurrentValueIsNull(ex);
                    }
                }
            } );
            progress.reportTaskCompleted();
        }
        catch(IOException ex) {
            exception.setIfCurrentValueIsNull(ex);
        }
        if(exception.isNotNull()) {
            NotificationsInSwing.showError("Write failed: "+exception.get().getMessage());
        }
    }

    @Override
    public String getSyntax() {
        return "export what=taxonParentMap file=<output-file> [toTop={false|true}] [names={false|true}];";
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        var lastOpenFile = new File(ProgramProperties.get("TaxonParentFile","taxon-parent.txt"));

        var file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), ev, "Save", ".txt");

        if (file != null) {
            execute("export what=taxonParentMap file='" + file.getPath() + "' toTop=true names=false;");
            ProgramProperties.put("TaxonParentFile", file);
        }
    }

    @Override
    public String getName() {
        return "Taxon-Parent Mapping...";
    }

    @Override
    public String getDescription() {
        return "Export taxonomy as node-to-parent mapping";
    }

    @Override
    public ImageIcon getIcon() {
            return ResourceManager.getIcon("sun/Export16.gif");
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    @Override
    public boolean isCritical() {
        return true;
    }

    @Override
    public boolean isApplicable() {
        return true;
    }
}
