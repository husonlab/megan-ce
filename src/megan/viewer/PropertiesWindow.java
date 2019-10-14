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
package megan.viewer;

import jloda.swing.util.BasicSwing;
import jloda.util.ProgramProperties;
import megan.core.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * the document properties window
 * 5.2005 Daniel Huson
 */
public class PropertiesWindow extends JDialog {
    private final JEditorPane textArea = new JEditorPane("text/html", "");

    public PropertiesWindow(JFrame parent, Document doc) {
        super(parent, "Properties - " + doc.getTitle() + " - " + ProgramProperties.getProgramVersion());
        setSize(420, 350);
        setModal(true);
        setup();
        setContent(doc);
        BasicSwing.centerDialogInParent(this, parent);
        setVisible(true);
    }

    private void setContent(Document doc) {
        StringBuilder buf = new StringBuilder();
        buf.append("<font face=\"Arial\">");

        if (doc.getMeganFile().getFileName() != null) {
            buf.append("<b>File: </b>").append(doc.getMeganFile().getFileName()).append("<br>");
            buf.append("<b>File type: </b>").append(doc.getMeganFile().getFileType()).append("<br>");
            buf.append("<b>Number of reads:</b> ").append(doc.getNumberOfReads()).append("<br><p>");
            try {
                buf.append(doc.getDataTable().getHeaderPrettyPrint()).append("<p>");
            } catch (IOException ignored) {
            }
        } else {
            buf.append("<b>Content type:</b> NCBI taxonomy<br>");
        }
        buf.append("<b>Taxonomy:</b> ").append(TaxonomyData.getTree().getNumberOfNodes()).append(" nodes, ")
                .append(TaxonomyData.getTree().getNumberOfEdges()).append(" edges<br>");
        buf.append("<b>Induced taxonomy:</b> ").append(doc.getDir().getMainViewer().getTree().getNumberOfNodes())
                .append(" nodes, ").append(doc.getDir().getMainViewer().getTree().getNumberOfEdges()).append(" edges<br>");
        if (!doc.getMeganFile().hasDataConnector() && doc.getNumberOfReads() > 0) {
            buf.append("<br> Note: This data was generated and saved as a summary<br>" +
                    "and so you cannot modify analysis parameters or inspect matches.<br>");
        }
        buf.append("<br>");
        for (String key : Document.getVersionInfo().keySet()) {
            buf.append("<b>").append(key).append(":</b> ");
            buf.append(Document.getVersionInfo().get(key)).append("<br>");
        }
        buf.append("</font>");
        textArea.setText(buf.toString());
    }

    private void setup() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        textArea.setEditable(false);
        topPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        JButton okButton = new JButton(getOkAction());
        bottomPanel.add(okButton, BorderLayout.EAST);
        getRootPane().setDefaultButton(okButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    }

    private AbstractAction okaction;

    private AbstractAction getOkAction() {
        final PropertiesWindow me = this;
        AbstractAction action = okaction;
        if (action != null)
            return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                me.setVisible(false);
                me.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "OK");
        return okaction = action;
    }
}

