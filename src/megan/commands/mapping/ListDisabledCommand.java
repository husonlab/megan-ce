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
package megan.commands.mapping;

import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Set;

public class ListDisabledCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "list taxa=disabled;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final Set<Integer> disabledTopLevelTaxa = TaxonomyData.getDisabledInternalTaxa();

        System.out.println(String.format("Total disabled taxa:%,12d", TaxonomyData.getDisabledTaxa().size()));
        System.out.println(String.format("Disabled top-level taxa:%,8d", TaxonomyData.getDisabledInternalTaxa().size()));

        for (Integer taxId : disabledTopLevelTaxa) {
            String taxName = TaxonomyData.getName2IdMap().get(taxId);
            System.out.println("[" + taxId + "] " + taxName);
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("show window=message;");
        execute(getSyntax());
    }

    public boolean isApplicable() {
        return getViewer() instanceof MainViewer;
    }

    public String getName() {
        return "List Disabled...";
    }

    public String getDescription() {
        return "List all disabled taxa";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}


