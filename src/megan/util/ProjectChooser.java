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
package megan.util;

import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import megan.core.Director;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * gui for
 * Daniel Huson, a small dialog for selecting one or more projects
 */
public class ProjectChooser extends JDialog {
    private boolean allowSummaryProjects = true;
    private boolean allowComparisonProjects = true;
    private final JList jlist;
    private final DefaultListModel model;
    private List<Integer> result;
    private final Map<Integer, Integer> listIndex2projectIndex;

    /**
     * constructor
     *
     * @param parent
     */
    public ProjectChooser(Component parent) {
        super();
        setLocationRelativeTo(parent);
        setModal(true);
        jlist = new JList();
        model = new DefaultListModel();
        jlist.setModel(model);
        jlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listIndex2projectIndex = new HashMap<>();
    }

    /**
     * show the dialog and choose the projects
     *
     * @return list of project ids or null
     */
    public List<Integer> showDialog(Director defaultDir) {
        setupDialog(defaultDir);
        getContentPane().validate();
        setSize(getPreferredSize());
        setVisible(true);
        return result;

    }

    private void setupDialog(Director defaultDir) {
        Container main = getContentPane();
        main.setLayout(new BorderLayout());
        main.add(new JLabel("Choose document(s):"), BorderLayout.NORTH);
        setupList(defaultDir);
        main.add(new JScrollPane(jlist), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.add(new JButton(getAllAction()));
        buttonPanel.add(new JButton(getCancelAction()));
        buttonPanel.add(new JButton(getOkAction()));
        main.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupList(Director defaultDir) {
        int count = 0;
        for (IDirector idir : ProjectManager.getProjects()) {
            if (idir instanceof Director) {
                Director dir = (Director) idir;
                int pid = dir.getID();

                model.addElement(dir.getTitle());
                if (defaultDir != null && dir.getID() == defaultDir.getID())
                    jlist.setSelectedIndex(count);
                listIndex2projectIndex.put(count++, pid);
            }
        }
    }

    private AbstractAction allAction;

    private AbstractAction getAllAction() {
        AbstractAction action = allAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                jlist.setSelectionInterval(0, model.getSize());
            }
        };
        action.putValue(AbstractAction.NAME, "All");
        return allAction = action;
    }

    private AbstractAction cancelAction;

    private AbstractAction getCancelAction() {
        AbstractAction action = cancelAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        };
        action.putValue(AbstractAction.NAME, "Cancel");
        return cancelAction = action;
    }

    private AbstractAction okAction;

    private AbstractAction getOkAction() {
        AbstractAction action = okAction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                result = new LinkedList<>();
                for (int t = 0; t < model.size(); t++) {
                    if (jlist.isSelectedIndex(t))
                        result.add(listIndex2projectIndex.get(t));
                }
                setVisible(false);
            }
        };
        action.putValue(AbstractAction.NAME, "Ok");
        return okAction = action;
    }


    public boolean isAllowSummaryProjects() {
        return allowSummaryProjects;
    }

    public void setAllowSummaryProjects(boolean allowSummaryProjects) {
        this.allowSummaryProjects = allowSummaryProjects;
    }

    public boolean isAllowComparisonProjects() {
        return allowComparisonProjects;
    }

    public void setAllowComparisonProjects(boolean allowComparisonProjects) {
        this.allowComparisonProjects = allowComparisonProjects;
    }
}
