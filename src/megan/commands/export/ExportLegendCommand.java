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

import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IViewerWithLegend;
import jloda.swing.export.*;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Document;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class ExportLegendCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "exportLegend file=<filename> [format={bmp|eps|gif|jpg|pdf|png|svg}]" +
                " [replace={false|true}] [textAsShapes={false|true}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        IDirectableViewer viewer = getViewer();

        np.matchIgnoreCase("exportLegend file=");

        String cname = np.getWordFileNamePunctuation();
        if (cname == null)
            throw new IOException("exportimage: Must specify FILE=filename");

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
        boolean text2shapes = np.findIgnoreCase(tokens, "textasShapes=", "true false", "false").equals("true");
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
            if (viewer instanceof IViewerWithLegend)
                graphicsType.writeToFile(file, ((IViewerWithLegend) viewer).getLegendPanel(), ((IViewerWithLegend) viewer).getLegendScrollPane(), true);
            else
                throw new IOException("Export legend: not imported for this type of viewer");
            System.err.println("Exported legend image to file: " + file);

        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public void actionPerformed(ActionEvent event) {
        IDirectableViewer viewer = getViewer();

        // setup a good default name: the file name plus .eps:
        String fileName = "Untitled";
        Document doc = getDir().getDocument();
        if (doc.getMeganFile().getFileName() != null)
            fileName = doc.getMeganFile().getFileName();
        fileName = Basic.replaceFileSuffix(fileName, "-legend.pdf");
        ExportImageDialog saveImageDialog;
        if (viewer instanceof IViewerWithLegend) {
            saveImageDialog = new ExportImageDialog(viewer.getFrame(), fileName, false, true, true, event);
            String command = saveImageDialog.displayDialog();
            if (command != null) {
                command = command.replaceAll("(?i)exportimage", "exportLegend");
                execute(command);
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() != null && getViewer() instanceof IViewerWithLegend && !((IViewerWithLegend) getViewer()).getShowLegend().equals("none");
    }

    public String getName() {
        return "Export Legend...";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Export content of legend window";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

