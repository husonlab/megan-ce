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
package megan.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * * add command
 * * Daniel Huson, 9.2012
 */
public class AddSampleFromExistingDocumentCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void actionPerformed(ActionEvent event) {
        Chooser chooser = new Chooser(getViewer().getFrame());
        Set<String> result = chooser.getResult();

        StringBuilder buf = new StringBuilder();

        if (result.size() > 0) {
            for (String line : result) {
                String[] tokens = line.split("] ");
                if (tokens.length == 2) {
                    int pid = Integer.parseInt(tokens[0].substring(1));
                    String sample = tokens[1].trim();
                    Director sourceDir = (Director) ProjectManager.getProject(pid);
                    if (sourceDir != null) {
                        Document sourceDoc = sourceDir.getDocument();
                        if (sourceDoc.getSampleNames().contains(sample)) {
                            buf.append(" sample='").append(sample).append("' source=").append(pid);
                        }
                    }
                }
            }
            if (buf.toString().length() > 0)
                execute("add " + buf.toString() + ";");
        }
    }

    public boolean isApplicable() {
        return ((Director) getDir()).getDocument().getMeganFile().isMeganSummaryFile();
    }

    public String getName() {
        return "Add...";
    }

    public String getAltName() {
        return "Add Samples From Document...";
    }


    public String getDescription() {
        return "Add samples from open document";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    private int loadList(JList list) {
        for (IDirector iDir : ProjectManager.getProjects()) {
            Director sourceDir = (Director) iDir;

            if (sourceDir != getDir()) {
                Document doc = sourceDir.getDocument();
                int pid = sourceDir.getID();
                for (String name : doc.getSampleNames())
                    ((DefaultListModel) list.getModel()).addElement("[" + pid + "] " + name);
            }
        }
        return list.getModel().getSize();
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
    }

    private class Chooser extends JDialog {
        private final JList list = new JList();
        final Set<String> result = new HashSet<>();

        Chooser(JFrame parent) {
            setSize(300, 400);
            setLocationRelativeTo(parent);
            setModal(true);

            list.setModel(new DefaultListModel());

            getContentPane().setLayout(new BorderLayout());
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));
            getContentPane().add(mainPanel, BorderLayout.CENTER);

            mainPanel.add(new JLabel("Select samples to add"), BorderLayout.NORTH);
            mainPanel.add(new JScrollPane(list), BorderLayout.CENTER);
            JPanel bottom = new JPanel();
            bottom.setBorder(BorderFactory.createEtchedBorder());
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
            bottom.add(Box.createHorizontalGlue());
            JButton cancelButton = new JButton(new AbstractAction("Cancel") {
                public void actionPerformed(ActionEvent actionEvent) {
                    Chooser.this.setVisible(false);
                }
            });
            bottom.add(cancelButton);
            final JButton applyButton = new JButton(new AbstractAction("Apply") {
                public void actionPerformed(ActionEvent actionEvent) {
                    for (int i : list.getSelectedIndices()) {
                        result.add(list.getModel().getElementAt(i).toString());
                    }
                    Chooser.this.setVisible(false);
                }
            });
            applyButton.setEnabled(false);
            bottom.add(applyButton);
            mainPanel.add(bottom, BorderLayout.SOUTH);

            list.getSelectionModel().addListSelectionListener(listSelectionEvent -> applyButton.setEnabled(list.getSelectionModel().getMaxSelectionIndex() >= 0));

            if (loadList(list) > 0)
                setVisible(true);
        }

        Set<String> getResult() {
            return result;
        }
    }
}
