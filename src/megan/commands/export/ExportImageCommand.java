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
package megan.commands.export;

import jloda.fx.util.IHasJavaFXStageAndRoot;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.export.*;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.NodeView;
import jloda.swing.util.IViewerWithJComponent;
import jloda.swing.util.JPanelWithFXStageAndRoot;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
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
            final IDirectableViewer viewer = getViewer();

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
                final ExportGraphicType graphicsType;
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
                } else if (viewer instanceof IHasJavaFXStageAndRoot) {
                    final IHasJavaFXStageAndRoot component = (IHasJavaFXStageAndRoot) viewer;
                    panel = new JPanelWithFXStageAndRoot(component.getJavaFXRoot(), component.getJavaFXStage()) {
                        @Override
                        public void paint(Graphics g) {
                            if (viewer instanceof IViewerWithJComponent)
                                ((IViewerWithJComponent) viewer).getComponent().paint(g);
                        }
                    };
                } else if (viewer instanceof IViewerWithJComponent) {
                    final JComponent component = ((IViewerWithJComponent) viewer).getComponent();
                    System.err.println("Size: " + component.getSize());
                    panel = new JPanel() {
                        public void paint(Graphics gc) {
                            component.paint(gc);
                        }
                    };
                    panel.setSize(component.getSize());
                } else {
                    final JFrame frame = viewer.getFrame();
                    panel = new JPanel() {
                        public void paint(Graphics gc) {
                            frame.paint(gc);
                        }
                    };
                    panel.setSize(frame.getContentPane().getSize());
                }
                if (!visibleOnly && scrollPane != null) { // need to adjust bounds and color background
                    if (!panel.getBounds().contains(scrollPane.getBounds())) {
                        final JPanel fpanel = panel;
                        final JScrollPane fScrollPane = scrollPane;

                        panel = new JPanel() {
                            @Override
                            public void paint(Graphics g) {
                                Rectangle rectangle = new Rectangle(getBounds());
                                final Point apt = fScrollPane.getViewport().getViewPosition();
                                rectangle.x += apt.x;
                                rectangle.y += apt.y;
                                g.setColor(Color.WHITE);
                                ((Graphics2D) g).fill(rectangle);
                                fpanel.paint(g);
                            }
                        };
                        panel.setBounds(fpanel.getBounds().x, fpanel.getBounds().y, Math.max(fpanel.getBounds().width, scrollPane.getBounds().width),
                                Math.max(fpanel.getBounds().height, scrollPane.getBounds().height));
                    }
                }
                ensureReasonableBounds(panel);
                ensureReasonableBounds(scrollPane);

                NodeView.descriptionWriter = null;
                if (nodeLabelDescriptionFile != null) {
                    try {
                        NodeView.descriptionWriter = new FileWriter(nodeLabelDescriptionFile);
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                }


                try {
                    /* todo: find out why no printer is found...
                    if(graphicsType instanceof PDFExportType && panel instanceof IHasJavaFXStageAndRoot) {
                            JavaFX2PDF javaFX2PDF = new JavaFX2PDF(((IHasJavaFXStageAndRoot) panel).getJavaFXRoot(), ((IHasJavaFXStageAndRoot) panel).getJavaFXStage());
                            javaFX2PDF.print();
                    }
                    else
                    */
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

    /**
     * if width or height unreasonably big (>100000) or small (<=0), sets to 1000
     *
     * @param panel
     */
    private static void ensureReasonableBounds(JPanel panel) {
        panel.setSize(new Dimension((panel.getWidth() <= 0 || panel.getWidth() > 100000 ? 1000 : panel.getWidth()), (panel.getHeight() <= 0 || panel.getHeight() > 100000 ? 1000 : panel.getHeight())));
    }

    /**
     * if width or height unreasonably big or small, sets to 1000
     *
     * @param panel
     */
    private void ensureReasonableBounds(JScrollPane panel) {
        if (panel != null) {
            panel.setSize(new Dimension((panel.getWidth() <= 0 || panel.getWidth() > 100000 ? 1000 : panel.getWidth()), (panel.getHeight() <= 0 || panel.getHeight() > 100000 ? 1000 : panel.getHeight())));
            final JViewport viewPort = panel.getViewport();
            viewPort.setExtentSize(new Dimension((int) (viewPort.getExtentSize().getWidth() <= 0 || viewPort.getExtentSize().getWidth() > 100000 ? 1000 : viewPort.getExtentSize().getWidth()),
                    (int) (viewPort.getExtentSize().getHeight() <= 0 || viewPort.getExtentSize().getHeight() > 100000 ? 1000 : viewPort.getExtentSize().getHeight())));
        }
    }

    public void actionPerformed(ActionEvent event) {
        final IDirectableViewer viewer = getViewer();

        // setup a good default name: the file name plus .eps:
        String fileName = "Untitled";
        Document doc = getDir().getDocument();
        if (doc.getMeganFile().getFileName() != null)
            fileName = doc.getMeganFile().getFileName();

        final boolean allowWhole = (viewer instanceof ViewerBase || viewer instanceof ChartViewer || (viewer instanceof ClusterViewer && ((ClusterViewer) viewer).isSwingPanel()));

        final ExportImageDialog saveImageDialog = new ExportImageDialog(viewer.getFrame(), fileName, true, allowWhole, true, event);
        final String command = saveImageDialog.displayDialog();
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public String getDescription() {
        return "Export content of window to an image file";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

