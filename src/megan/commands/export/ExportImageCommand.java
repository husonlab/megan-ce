/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.commands.export;

import jloda.export.*;
import jloda.graphview.GraphView;
import jloda.graphview.NodeView;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirectableViewer;
import jloda.util.Basic;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;
import megan.clusteranalysis.ClusterViewer;
import megan.commands.CommandBase;
import megan.core.Document;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExportImageCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "exportImage file=<filename> [descriptionFile=<filename>] [format={bmp|eps|gif|jpg|pdf|png|svg}] [replace={false|true}]\n" +
                "\t[visibleOnly]={false|true}] [textAsShapes={false|true}] [title=<string>];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        if (getViewer() != null) {
            IDirectableViewer viewer = getViewer();

            np.matchIgnoreCase("exportImage file=");

            String cname = np.getWordFileNamePunctuation();
            if (cname == null)
                throw new IOException("exportImage: Must specify FILE=filename");

            String nodeLabelDescriptionFile = null;
            if (np.peekMatchIgnoreCase("descriptionFile")) {
                np.matchIgnoreCase("descriptionFile=");
                nodeLabelDescriptionFile = np.getAbsoluteFileName();
            }

            java.util.List<String> tokens;
            try {
                np.pushPunctuationCharacters("=;"); // filename punctuation
                tokens = np.getTokensRespectCase(null, ";");
            } finally {
                np.popPunctuationCharacters();
            }
            String format = np.findIgnoreCase(tokens, "format=", "svg gif png jpg pdf eps bmp file-suffix", "file-suffix");
            boolean visibleOnly = np.findIgnoreCase(tokens, "visibleOnly=", "true false", "false").equals("true");
            boolean text2shapes = np.findIgnoreCase(tokens, "textAsShapes=", "true false", "false").equals("true");
            String title = np.findIgnoreCase(tokens, "title=", null, "none");
            boolean replace = np.findIgnoreCase(tokens, "replace=", "true false", "false").equals("true");
            np.checkFindDone(tokens);

            File file = new File(cname);
            if (!replace && file.exists())
                throw new IOException("File exists: " + cname + ", use REPLACE=true to overwrite");

            try {
                ExportGraphicType graphicsType;
                if (format.equalsIgnoreCase("eps")) {
                    graphicsType = new EPSExportType();
                    ((EPSExportType) graphicsType).setDrawTextAsOutlines(!text2shapes);
                } else if (format.equalsIgnoreCase("png"))
                    graphicsType = new PNGExportType();
                else if (format.equalsIgnoreCase("pdf"))
                    graphicsType = new PDFExportType();
                else if (format.equalsIgnoreCase("svg"))
                    graphicsType = new SVGExportType();
                else if (format.equalsIgnoreCase("gif"))
                    graphicsType = new GIFExportType();
                else if (format.equalsIgnoreCase("bmp"))
                    graphicsType = new RenderedExportType();
                else if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg"))
                    graphicsType = new JPGExportType();
                else
                    throw new IOException("Unsupported graphics format: " + format);

                JPanel panel;
                JScrollPane scrollPane = null;

                if (viewer instanceof ViewerBase) {
                    panel = ((ViewerBase) viewer).getPanel();
                    scrollPane = ((ViewerBase) viewer).getScrollPane();
                } else if (viewer instanceof ChartViewer) {
                    panel = ((ChartViewer) viewer).getContentPanel();
                    scrollPane = ((ChartViewer) viewer).getScrollPane();
                } else if (viewer instanceof ClusterViewer) {
                    panel = ((ClusterViewer) viewer).getPanel();
                    GraphView graphView = ((ClusterViewer) viewer).getGraphView();
                    if (graphView != null)
                        scrollPane = graphView.getScrollPane();
                } else {
                    final JFrame frame = viewer.getFrame();
                    panel = new JPanel() {
                        public void paint(Graphics gc) {
                            frame.paint(gc);
                        }
                    };
                    panel.setSize(frame.getContentPane().getSize());
                }
                if (!visibleOnly && scrollPane != null) {
                    if (!panel.getBounds().contains(scrollPane.getBounds())) {
                        visibleOnly = true;
                    }
                }

                NodeView.descriptionWriter = null;
                if (nodeLabelDescriptionFile != null) {
                    try {
                        NodeView.descriptionWriter = new FileWriter(nodeLabelDescriptionFile);
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                }
                try {
                    graphicsType.writeToFile(file, panel, scrollPane, !visibleOnly);
                } finally {
                    if (NodeView.descriptionWriter != null) {
                        NodeView.descriptionWriter.close();
                        NodeView.descriptionWriter = null;
                    }
                }
                System.err.println("Exported image to file: " + file);

            } catch (Exception ex) {
                throw new IOException(ex);
            }
        } else {
            np.matchWordIgnoreCase("exportImage");
            throw new IOException("ExportImage not implemented for this type of window: " + (Basic.getShortName(getParent().getClass())));
        }
    }

    public void actionPerformed(ActionEvent event) {
        IDirectableViewer viewer = getViewer();

        // setup a good default name: the file name plus .eps:
        String fileName = "Untitled";
        Document doc = getDir().getDocument();
        if (doc.getMeganFile().getFileName() != null)
            fileName = doc.getMeganFile().getFileName();
        ExportImageDialog saveImageDialog;
        if (viewer instanceof ViewerBase || viewer instanceof ChartViewer) {
            saveImageDialog = new ExportImageDialog(viewer.getFrame(), fileName, true, true, true, event);
        } else {
            saveImageDialog = new ExportImageDialog(viewer.getFrame(), fileName, true, false, true, event);
        }
        String command = saveImageDialog.displayDialog();
        if (command != null)
            executeImmediately(command);
    }

    public boolean isApplicable() {
        return getViewer() != null;
    }

    public String getName() {
        return "Export Image...";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    public String getDescription() {
        return "Export content of window to an image file";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Export16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

