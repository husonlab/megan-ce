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
package megan.dialogs.attributes;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.StatusBar;
import jloda.swing.util.ToolBar;
import jloda.swing.window.MenuBar;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import megan.core.Director;
import megan.dialogs.attributes.commands.ShowInNCBIWebPageCommand;
import megan.main.MeganProperties;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Daniel Huson, 2010
 */
public class AttributesWindow implements IDirectableViewer, Printable {
    private boolean uptodate = true;
    private boolean locked = false;
    private final JFrame frame;
    private final StatusBar statusBar;
    private final Director dir;

    private final CommandManager commandManager;
    private final MenuBar menuBar;


    private JTree tree;
    private DefaultMutableTreeNode root = null;
    private boolean doClear = false; // if this is set, window will be cleared on next rescan
    public boolean doSortByNrOfReads = false;
    public boolean doSortByAlpha = true;

    private final AttributeData attData;
    private int dividerLocation = 360;
    public String selectedTaxon = "No Taxon Selected";

    private JSplitPane splitPane = null;
    private JEditorPane descEditor = null;
    private JEditorPane previewEditor = null;
    private JEditorPane helpEditor = null;
    private StringBuilder helpText = null;
    private Map<String, String> prop2Explanation = new Hashtable<>();
    private final Map<String, Map<String, Number>> attribute2taxa2value;
    //same data like attribute2taxa2value but not sorted by number of reads per taxon
    private final Map<String, Pair> attribute2SortedTaxValPair = new TreeMap<>(); //e.g. <Gram Stain:Positive, sortedPair[Taxa,Value]>

    /**
     * constructor
     *
     * @param dir
     */
    public AttributesWindow(Director dir, JFrame parent) {
        this.dir = dir;
        commandManager = new CommandManager(dir, this,
                new String[]{"megan.commands", "megan.dialogs.attributes.commands"}, !ProgramProperties.isUseGUI());

        frame = new JFrame();

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        frame.setJMenuBar(menuBar);
        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        statusBar = new StatusBar();
        String dataset = Basic.getFileBaseName(dir.getDocument().getTitle());
        attData = AttributeData.getInstance();

        //fill attribute2taxa2value
        attribute2taxa2value = new TreeMap<>(); //<Gram Stain:Yes, <Taxa, NrReads>>
        final Map<String, Map<String, Number>> label2series2valueForTaxa = new TreeMap<>();
        dir.getDocument().getTaxonName2DataSet2SummaryCount(label2series2valueForTaxa);
        AttributeData.getAttributes2Taxa2Values(label2series2valueForTaxa, attribute2taxa2value);
        generateSortedAttribute2taxa2values(); //if the user wants to sort by nr of reads
        generateHelpText();

        setTitle();
        frame.setIconImages(ProgramProperties.getProgramIconImages());
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setJMenuBar(menuBar);

        JToolBar toolBar = new ToolBar(this, GUIConfiguration.getToolBarConfiguration(), commandManager);
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setTopComponent(getTreePanel());
        splitPane.setBottomComponent(getDescriptionPanel());
        splitPane.setDividerLocation(dividerLocation);
        splitPane.setOneTouchExpandable(true);
        splitPane.setEnabled(true);
        Dimension preferredSize = new Dimension(800, 700);
        splitPane.setPreferredSize(preferredSize);

        //ComponentListener for the splitpane divider
        splitPane.getTopComponent().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                dividerLocation = splitPane.getDividerLocation();
            }
        });
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.getContentPane().add(statusBar, BorderLayout.SOUTH);
        frame.setSize(preferredSize);
        frame.setLocationRelativeTo(parent);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
        setTotalAmountTaxaInStatusBar();
    }

    /**
     * Fills a new sorted map with the sorted set of taxa2values for each
     * attribute.
     */
    private void generateSortedAttribute2taxa2values() {
        String[] taxa;
        int[] values;
        for (String att_kind : attribute2taxa2value.keySet()) {
            Map<String, Number> taxa2values = attribute2taxa2value.get(att_kind);
            taxa = new String[taxa2values.size()];
            values = new int[taxa2values.size()];
            int i = 0;
            for (String taxname : taxa2values.keySet()) {
                taxa[i] = taxname;
                values[i] = taxa2values.get(taxname).intValue();
                i++;
            }
            doInsertionSort(taxa, values);
            attribute2SortedTaxValPair.put(att_kind, new Pair<>(taxa, values));
        }
    }

    /**
     * Reads in the html page with infos on the ncbi attributes
     */
    private void generateHelpText() {
        String filename = "resources/files/attributehelp.txt";
        helpText = new StringBuilder(3000);
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(filename));
            while (true) {
                line = br.readLine();
                if (line == null)
                    break;
                helpText.append(line);
            }
        } catch (IOException e1) {
            System.out.println("Could not find/parse " + filename);
            e1.printStackTrace();
        }
    }


    /**
     * Sets the number of identified taxa in the status bar
     */
    private void setTotalAmountTaxaInStatusBar() {
        int count = 0;
        for (String kind : attData.getAttributes2Properties().get("Endospores")) {
            if (attribute2taxa2value.containsKey("Endospores:" + kind)) {
                count += attribute2taxa2value.get("Endospores:" + kind).size();
            }
        }
        statusBar.setText2("Taxa identified: " + count);
    }

    /**
     * Depending of the selected taxon in the tree this method
     * returns the panel with the taxon information.
     *
     * @return the description panel
     */
    private JScrollPane getDescriptionPanel() {
        if (descEditor == null) {
            descEditor = new JEditorPane("text/html", "");
            descEditor.setEditable(false);
            descEditor.setText("<p align=\"center\"><i>No classified taxon selected</i></p>");
        }
        if (attData.getTaxaName2Attributes2Properties().get(selectedTaxon) != null) {
            Hashtable<String, String> attributes2Properties = this.attData.getTaxaName2Attributes2Properties().get(this.selectedTaxon);
            StringBuilder markup = new StringBuilder();
            markup.append("<h2><u><font face=\"Arial\">").append(this.selectedTaxon).append("</font></u></h2>");
            markup.append("<font face=\"Arial\">TaxId: <b>").append(TaxonomyData.getName2IdMap().get(this.selectedTaxon)).append("</b></font><br>");
            markup.append("<font face=\"Arial\">Kingdom: <b>").append(attributes2Properties.get(AttributeData.attributeList[11])).append("</b></font><br>");
            markup.append("<font face=\"Arial\">Group: <b>").append(attributes2Properties.get(AttributeData.attributeList[10])).append("</b></font><br>");
            markup.append("<font face=\"Arial\">Genome Size (MB): <b>").append(attributes2Properties.get(AttributeData.attributeList[8])).append("</b></font><br>");
            markup.append("<font face=\"Arial\">GC Content: <b>").append(attributes2Properties.get(AttributeData.attributeList[9])).append("</b></font><p>");
            markup.append("<u><b><font face=\"Arial\">Attributes:</font></u></b><br>");
            int i = 0;
            for (String attribute : AttributeData.attributeList) {
                i++;
                if (i < 8)
                    markup.append("<u><font face=\"Arial\">").append(attribute).append("</u>" + ": <b><font face=\"Arial\">").append(attributes2Properties.get(attribute)).append("</font></b><br>");
            }
            descEditor.setText(markup.toString());
        }
        return new JScrollPane(descEditor);
    }

    /**
     * Depending of the given node label a preview of its children
     * is displayed
     *
     * @param label
     * @param node  parent node
     * @return the preview panel
     */
    private JScrollPane getPreviewPanel(String label, TreeNode node) {
        if (previewEditor == null) {
            previewEditor = new JEditorPane("text/html", "");
            previewEditor.setEditable(false);
        }
        String parentnode = node.toString();
        StringBuilder markup = new StringBuilder();
        label = removeSizeInfo(label);
        //if label is attribute
        if (attData.getAttributes2Properties().containsKey(label)) {
            markup.append("<h3><u><font face=\"Arial\">").append(label).append("</font></u></h3>");
            for (String prop : attData.getAttributes2Properties().get(label)) {
                markup.append("<font face=\"Arial\">&nbsp;").append(prop).append("</font><br>");
            }
        } else { //if label is "yes","no",... get parentnode -> attribute
            if (doSortByAlpha) {
                if (attribute2taxa2value.containsKey(parentnode + ":" + label)) {
                    String attribute_kind = parentnode + ":" + label;
                    Map<String, Number> taxnames2values = attribute2taxa2value.get(attribute_kind);
                    markup.append("<h3><u><font face=\"Arial\">").append(parentnode).append(": ").append(label).append(" [").append(taxnames2values.size()).append(" Taxa]</font></u></h3>");
                    for (String taxname : taxnames2values.keySet()) {
                        markup.append("<font face=\"Arial\"> &nbsp; ").append(taxname).append(" [").append(taxnames2values.get(taxname)).append(" Reads]" + "</font><br>");
                    }
                }
            } else {
                if (doSortByNrOfReads) { //use other sorted data set
                    String attribute_kind = parentnode + ":" + label;
                    if (attribute2SortedTaxValPair.containsKey(attribute_kind)) {
                        Pair pairTax2Val = attribute2SortedTaxValPair.get(attribute_kind);
                        String[] taxnames = (String[]) pairTax2Val.getFirst();
                        int[] noOfReads = (int[]) pairTax2Val.getSecond();
                        markup.append("<h3><u><font face=\"Arial\">").append(parentnode).append(": ").append(label).append(" [").append(taxnames.length).append(" Taxa]</font></u></h3>");
                        for (int j = taxnames.length - 1; j >= 0; j--) {
                            markup.append("<font face=\"Arial\"> &nbsp; ").append(taxnames[j]).append(" [").append(noOfReads[j]).append(" Reads]" + "</font><br>");
                        }
                    }
                }
            }
        }
        previewEditor.setText(markup.toString());
        return new JScrollPane(previewEditor);
    }


    /**
     * Removes the last part of a label,
     * e.g. E.coli [100 Reads] -> E.coli
     *
     * @param label
     * @return the label withou the read info
     */
    private String removeSizeInfo(String label) {
        int pos = label.indexOf("[");
        if (pos > 0)
            return label.substring(0, pos).trim();
        return label;
    }

    /**
     * By clicking on the top node 'Microbial Attributes" a
     * help panel is displayed.
     *
     * @return the preview panel
     */
    private JScrollPane getHelpPanel() {
        if (helpEditor == null) {
            helpEditor = new JEditorPane("text/html", "");
            helpEditor.setEditable(false);
            helpEditor.setText(helpText.toString());
        }
        return new JScrollPane(helpEditor);
    }


    /**
     * Constructs the tree.
     *
     * @return a scrollpane with the tree
     */
    public JScrollPane getTreePanel() {
        root = new DefaultMutableTreeNode("Microbial Attributes");
        tree = new JTree(this.root);
        this.createNodes(this.root);
        ToolTipManager.sharedInstance().registerComponent(tree);
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        this.tree.setCellRenderer(renderer);

        this.tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) AttributesWindow.this.tree.getLastSelectedPathComponent();
            if (node == null)
                return;
            String label = node.getUserObject().toString();
            if (node.isLeaf() && AttributesWindow.this.attData.getMicrobialTaxa().contains(removeSizeInfo(label))) {
                AttributesWindow.this.selectedTaxon = removeSizeInfo(label);
                AttributesWindow.this.splitPane.setBottomComponent(AttributesWindow.this.getDescriptionPanel());
            } else if (!node.isLeaf()) {
                if (label.equals("Microbial Attributes")) {
                    AttributesWindow.this.splitPane.setBottomComponent(AttributesWindow.this.getHelpPanel());
                } else //kind node e.g. "Negative [100 Reads, 20 Taxa]"
                    if (node.getPath().length >= 2) {
                        AttributesWindow.this.selectedTaxon = removeSizeInfo(label);
                        AttributesWindow.this.splitPane.setBottomComponent(AttributesWindow.this.getPreviewPanel(label, node.getPath()[node.getPath().length - 2]));
                    }
            }
            splitPane.setDividerLocation(dividerLocation);
        });

        this.tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    if (TaxonomyData.getName2IdMap().getNames().contains(selectedTaxon)) {
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(commandManager.getJMenuItem(ShowInNCBIWebPageCommand.ALTNAME));
                        popup.show((Component) e.getSource(), e.getX(), e.getY());
                        popup.show((Component) e.getSource(), e.getX(), e.getY());
                    }

                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    if (TaxonomyData.getName2IdMap().getNames().contains(selectedTaxon)) {
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(commandManager.getJMenuItem(ShowInNCBIWebPageCommand.ALTNAME));
                        popup.show((Component) e.getSource(), e.getX(), e.getY());
                    }
                }
            }
        });

        return new JScrollPane(tree);
    }

    /**
     * Constructs the inner nodes and leaves of the JTree
     *
     * @param top the root
     */
    private void createNodes(DefaultMutableTreeNode top) {
        DefaultMutableTreeNode attributeNode = null;
        DefaultMutableTreeNode kindNode;
        DefaultMutableTreeNode toSelect = null;

        String addedAttribute = "";
        for (String attribute_kind : attribute2taxa2value.keySet()) {
            //split the attribute from the kind, e.g. [Gram Stain:Yes]
            int pos = attribute_kind.indexOf(":");
            String attribute = attribute_kind.substring(0, pos);
            String kind = attribute_kind.substring(pos + 1);

            kindNode = new DefaultMutableTreeNode(kind);
            if (!attribute.equals(addedAttribute)) {
                attributeNode = new DefaultMutableTreeNode(attribute);
                addedAttribute = attribute;
                top.add(attributeNode);
            }
            Map<String, Number> taxa2values = attribute2taxa2value.get(attribute_kind);
            int nrOfReadsPerKind = 0;
            int nrOfReadsPerTaxon = 0;
            if (doSortByAlpha) {
                for (String taxname : taxa2values.keySet()) {
                    nrOfReadsPerTaxon = taxa2values.get(taxname).intValue();
                    if (nrOfReadsPerTaxon > 0) {
                        nrOfReadsPerKind += nrOfReadsPerTaxon;
                        DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(taxname, false);
                        leaf.setUserObject(leaf.getUserObject().toString() + " [" + nrOfReadsPerTaxon + " Reads]");
                        kindNode.add(leaf);
                        if (toSelect == null) {
                            selectedTaxon = taxname;
                            toSelect = leaf;
                        }
                    }
                }
            } else {
                if (doSortByNrOfReads) {
                    if (attribute2SortedTaxValPair.containsKey(attribute_kind)) {
                        Pair pairTax2Val = attribute2SortedTaxValPair.get(attribute_kind);
                        String[] taxnames = (String[]) pairTax2Val.getFirst();
                        int[] nrOfReads = (int[]) pairTax2Val.getSecond();
                        nrOfReadsPerKind = 0;
                        for (int i = taxnames.length - 1; i >= 0; i--) {
                            String taxname = taxnames[i];
                            nrOfReadsPerTaxon = nrOfReads[i];
                            if (nrOfReadsPerTaxon > 0) {
                                nrOfReadsPerKind += nrOfReadsPerTaxon;
                                DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(taxname, false);
                                leaf.setUserObject(leaf.getUserObject().toString() + " [" + nrOfReadsPerTaxon + " Reads]");
                                kindNode.add(leaf);
                                if (toSelect == null) {
                                    selectedTaxon = taxname;
                                    toSelect = leaf;
                                }
                            }
                        }
                    }
                }
            }
            kindNode.setUserObject(kindNode.getUserObject().toString() + " [" + taxa2values.size() + " Taxa, " + nrOfReadsPerKind + " Reads]");
            if (attributeNode != null)
                attributeNode.add(kindNode);
        }
        //select first leaf
        if (toSelect != null) {
            TreeNode[] treePath = toSelect.getPath();
            if (treePath != null) {
                tree.expandPath(new TreePath(treePath));
                tree.setSelectionPath(new TreePath(treePath));
            }
        }
    }

    /**
     * Sorts the values ascendingly. The names array is sorted accordingly.
     *
     * @param names  string array
     * @param values int array
     */
    private void doInsertionSort(String[] names, int[] values) {
        int i, j, t;
        String temp = "";
        for (i = 1; i < values.length; i++) {
            j = i;
            t = values[j];
            temp = names[j];
            while (j > 0 && values[j - 1] > t) {
                values[j] = values[j - 1];
                names[j] = names[j - 1];
                j--;
            }
            values[j] = t;
            names[j] = temp;
        }
    }


    /**
     * collapse the given node   or root
     *
     * @param v
     */
    private void collapse(DefaultMutableTreeNode v) {
        if (v == null)
            v = this.root;

        for (Enumeration descendants = v.depthFirstEnumeration(); descendants.hasMoreElements(); ) {
            v = (DefaultMutableTreeNode) descendants.nextElement();
            this.tree.collapsePath(new TreePath(v.getPath()));
        }
    }

    /**
     * collapse an array of paths
     *
     * @param paths
     */
    public void collapse(TreePath[] paths) {
        for (TreePath path : paths) {
            this.collapse((DefaultMutableTreeNode) path.getLastPathComponent());
        }
    }

    /**
     * expand the given node
     *
     * @param v
     */
    private void expand(DefaultMutableTreeNode v) {
        if (v == null)
            v = this.root;

        for (Enumeration descendants = v.breadthFirstEnumeration(); descendants.hasMoreElements(); ) {
            v = (DefaultMutableTreeNode) descendants.nextElement();
            this.tree.expandPath(new TreePath(v.getPath()));
        }
    }

    /**
     * expand an array of paths
     *
     * @param paths
     */
    public void expand(TreePath[] paths) {
        for (TreePath path : paths) {
            this.expand((DefaultMutableTreeNode) path.getLastPathComponent());
        }
    }

    public JFrame getFrame() {
        return this.frame;
    }

    /**
     * gets the title
     *
     * @return title
     */
    public String getTitle() {
        return this.frame.getTitle();
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return this.uptodate;
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        dir.removeViewer(this);
        frame.dispose();
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        locked = true;
        commandManager.setEnableCritical(false);
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        this.uptodate = flag;
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        commandManager.setEnableCritical(true);
        locked = false;
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * ask view to rescan itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what what should be updated? Possible values: Director.ALL or Director.TITLE
     */
    public void updateView(String what) {
        this.uptodate = false;
        if (this.doClear) {
            this.doClear = false;
        }
        commandManager.updateEnableState();
        this.setTitle();
        this.uptodate = true;
    }


    /**
     * set the title of the window
     */
    private void setTitle() {
        String newTitle = "Microbial Attributes - " + this.dir.getDocument().getTitle();

        /*
        if (dir.getDocument().isDirty())
            newTitle += "*";
           */

        if (this.dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + this.dir.getID() + "] - " + ProgramProperties.getProgramVersion();

        if (!this.frame.getTitle().equals(newTitle)) {
            this.frame.setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    /**
     * gets the associated command manager
     *
     * @return command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    public JTree getTree() {
        return tree;
    }


    public JSplitPane getSplitPane() {
        return splitPane;
    }


    /**
     * Print the graph associated with this viewer.
     *
     * @param gc0        the graphics context.
     * @param format     page format
     * @param pagenumber page index
     */
    public int print(Graphics gc0, PageFormat format, int pagenumber) throws PrinterException {
        if (pagenumber == 0) {
            Graphics2D gc = ((Graphics2D) gc0);

            Dimension dim = frame.getContentPane().getSize();

            int image_w = dim.width;
            int image_h = dim.height;

            double paper_x = format.getImageableX() + 1;
            double paper_y = format.getImageableY() + 1;
            double paper_w = format.getImageableWidth() - 2;
            double paper_h = format.getImageableHeight() - 2;

            double scale_x = paper_w / image_w;
            double scale_y = paper_h / image_h;
            double scale = Math.min(scale_x, scale_y);

            double shift_x = paper_x + (paper_w - scale * image_w) / 2.0;
            double shift_y = paper_y + (paper_h - scale * image_h) / 2.0;

            gc.translate(shift_x, shift_y);
            gc.scale(scale, scale);

            gc.setStroke(new BasicStroke(1.0f));
            gc.setColor(Color.BLACK);

            frame.getContentPane().paint(gc);

            return Printable.PAGE_EXISTS;
        } else
            return Printable.NO_SUCH_PAGE;
    }


    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "AttributesWindow";
    }

}
