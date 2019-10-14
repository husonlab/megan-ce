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
package megan.stats;


import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;

/**
 * compare actions
 * Daniel Huson, DAT3.2007E
 */
public class ComparisonStatsActions {
    public final static String DEPENDS_ON_ONE_SELECTED = "ONE";
    private final static String DEPENDS_ON_TWO_SELECTED = "TWO";

    private final java.util.List<AbstractAction> all = new LinkedList<>();

    private final JComboBox comboBox1;
    private final JComboBox comboBox2;
    private final JComboBox methodCBox;

    private final ComparisonStatsWindow comparisonStatsWindow;

    /**
     * constructor
     */
    public ComparisonStatsActions(ComparisonStatsWindow comparisonStatsWindow) {
        this.comboBox1 = comparisonStatsWindow.dataCBox1;
        this.comboBox2 = comparisonStatsWindow.dataCBox2;
        this.methodCBox = comparisonStatsWindow.methodCBox;

        this.comparisonStatsWindow = comparisonStatsWindow;
    }


    public void updateEnableState() {
        InputDataItem item1 = (InputDataItem) this.comboBox1.getSelectedItem();
        InputDataItem item2 = (InputDataItem) this.comboBox2.getSelectedItem();

        getApplyAction().setEnabled(item1 != null &&
                item2 != null && item1.getPID() != item2.getPID()
                && methodCBox.getSelectedItem() != null);
    }


    private AbstractAction cancelAction; // cancel

    public AbstractAction getCancelAction() {
        AbstractAction action = cancelAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                comparisonStatsWindow.setVisible(false);
                comparisonStatsWindow.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Cancel");

        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Cancel");
        all.add(action);
        return cancelAction = action;
    }

    private AbstractAction applyAction; // ok

    public AbstractAction getApplyAction() {
        AbstractAction action = applyAction;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                comparisonStatsWindow.setVisible(false);
                comparisonStatsWindow.execute();
                comparisonStatsWindow.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Apply");
        action.putValue(DEPENDS_ON_TWO_SELECTED, Boolean.TRUE);
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply the chosen statistical comparison");
        all.add(action);
        return applyAction = action;
    }
}
