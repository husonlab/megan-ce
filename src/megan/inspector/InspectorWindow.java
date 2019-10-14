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
package megan.inspector;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IViewerWithFindToolBar;
import jloda.swing.director.ProjectManager;
import jloda.swing.find.FindToolBar;
import jloda.swing.find.JTreeSearcher;
import jloda.swing.find.SearchManager;
import jloda.swing.util.MultiLineCellRenderer;
import jloda.swing.util.PopupMenu;
import jloda.swing.util.StatusBar;
import jloda.swing.util.ToolBar;
import jloda.swing.window.MenuBar;
import jloda.util.*;
import megan.algorithms.ActiveMatches;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.data.*;
import megan.dialogs.input.InputDialog;
import megan.main.MeganProperties;
import megan.util.IReadsProvider;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * the  inspector window
 * Daniel Huson , 3.2006 ,  rewrote 10.2009
 */
public class InspectorWindow implements IDirectableViewer, IViewerWithFindToolBar, Printable, IReadsProvider {
    private boolean uptodate = true;
    private final JFrame frame;
    private final JPanel mainPanel;

    private final JScrollPane scrollPane;
    private final StatusBar statusBar;
    final Director dir;
    final JTree dataTree;
    private boolean doClear = false; // if this is set, window will be cleared on next rescan

    private boolean isDirty = false;

    // by default, the different types of nodes sorted alphabetically, alternative: by rank
    private boolean sortReadsAlphabetically = true;

    private final CommandManager commandManager;
    private final MenuBar menuBar;
    private final JPopupMenu popupMenu;

    private boolean showFindToolBar = false;
    private final SearchManager searchManager;

    private final Loader loader;

    private boolean isLocked = false;

    private final Map<String, NodeBase> classification2RootNode = new HashMap<>();
    private final NodeBase rootNode;

    /**
     * constructor
     *
     * @param dir
     */
    public InspectorWindow(final Director dir) {
        this.dir = dir;

        this.commandManager = new CommandManager(dir, this, new String[]{"megan.inspector.commands", "megan.commands"}, !ProgramProperties.isUseGUI());

        frame = new JFrame();
        setTitle();
        frame.setIconImages(ProgramProperties.getProgramIconImages());

        frame.getContentPane().setLayout(new BorderLayout());

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), getCommandManager());
        frame.setJMenuBar(menuBar);
        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        popupMenu = new PopupMenu(this, GUIConfiguration.getPopupMenuConfiguration(), getCommandManager());

        frame.add(new ToolBar(this, GUIConfiguration.getToolBarConfiguration(), commandManager), BorderLayout.NORTH);
        statusBar = new StatusBar();
        frame.getContentPane().add(statusBar, BorderLayout.SOUTH);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);

        loader = new Loader(this);

        rootNode = new NodeBase(dir.getDocument().getMeganFile().getName());

        dataTree = new JTree(rootNode);
        //dataTree.setRootVisible(false);

        final String[] cNames = dir.getDocument().getActiveViewers().toArray(new String[0]);
        NodeBase[] rootNodes = new NodeBase[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            String cName = cNames[i];
            rootNodes[i] = new NodeBase(cName);
            classification2RootNode.put(cName, rootNodes[i]);
        }

        dataTree.setRowHeight(0); //When row height is 0, each node determines its height from its contents.
        dataTree.setShowsRootHandles(true);
        {
            MyTreeListener treeListener = new MyTreeListener(dir, this);
            dataTree.addTreeWillExpandListener(treeListener);
            dataTree.addTreeExpansionListener(treeListener);
        }

        dataTree.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent me) {
                if (me.isPopupTrigger()) {
                    popupMenu.show(dataTree, me.getX(), me.getY());
                }
            }

            public void mousePressed(MouseEvent me) {
                if (me.isPopupTrigger()) {
                    popupMenu.show(dataTree, me.getX(), me.getY());
                }
            }
        });

        dataTree.addTreeSelectionListener(treeSelectionEvent -> getCommandManager().updateEnableState());

        dataTree.setCellRenderer(new MultiLineCellRenderer());
        scrollPane = new JScrollPane(dataTree);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        searchManager = new SearchManager(dir, this, new JTreeSearcher(dataTree), false, true);

        final int[] geometry = ProgramProperties.get(MeganProperties.INSPECTOR_WINDOW_GEOMETRY, new int[]{100, 100, 300, 600});
        frame.setLocationRelativeTo(MainViewer.getLastActiveFrame());
        frame.setSize(geometry[2], geometry[3]);

        frame.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                componentResized(e);
            }

            public void componentResized(ComponentEvent event) {
                if ((event.getID() == ComponentEvent.COMPONENT_RESIZED || event.getID() == ComponentEvent.COMPONENT_MOVED) &&
                        (frame.getExtendedState() & JFrame.MAXIMIZED_HORIZ) == 0
                        && (frame.getExtendedState() & JFrame.MAXIMIZED_VERT) == 0) {
                    ProgramProperties.put(MeganProperties.INSPECTOR_WINDOW_GEOMETRY, new int[]
                            {frame.getLocation().x, frame.getLocation().y, frame.getSize().width,
                                    frame.getSize().height});
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                final InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, InspectorWindow.this);
            }

            public void windowClosing(WindowEvent event) {
                if (dir.getDocument().getProgressListener() != null)
                    dir.getDocument().getProgressListener().setUserCancelled(true);
                clear();
            }
        });
    }

    /**
     * erase the tree
     */
    public void clear() {
        final DefaultTreeModel model = (DefaultTreeModel) dataTree.getModel();
        for (NodeBase root : classification2RootNode.values()) {
            root.removeAllChildren();
            model.nodeStructureChanged(root);
        }
    }

    /**
     * add a top-level node.
     *
     * @param name2Count2Ids
     * @param classificationName
     */
    public void addTopLevelNode(final LinkedList<Triplet<String, Float, Collection<Integer>>> name2Count2Ids, final String classificationName) {
        final NodeBase classificationRoot = classification2RootNode.get(classificationName);
        if (classificationRoot == null)
            return;
        if (!rootNode.isNodeChild(classificationRoot)) {
            try {
                rootNode.add(classificationRoot);
                ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(rootNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dataTree.expandPath(new TreePath(new Object[]{rootNode, classificationRoot}));
        statusBar.setText2("Rows: " + countVisibleNodes());

        loader.execute(progressListener -> {
            progressListener.setMaximum(name2Count2Ids.size());
            progressListener.setProgress(0);
            long startTime = System.currentTimeMillis();
            long diff = 500;
            boolean needsFinalRefresh = false;

            try {
                for (final Triplet<String, Float, Collection<Integer>> triplet : name2Count2Ids) {
                    final String name = triplet.getFirst();
                    boolean isPresent = false;
                    // first determine whether taxon already present as child of parent:
                    Enumeration level1Enumeration = classificationRoot.children();
                    while (!isPresent && level1Enumeration.hasMoreElements()) {
                        NodeBase level1Node = (NodeBase) level1Enumeration.nextElement();
                        if (level1Node instanceof TopLevelNode && level1Node.getName().equals(name)) {
                            isPresent = true;
                        }
                    }
                    if (!isPresent) {
                        final boolean doRefresh = (System.currentTimeMillis() - startTime > diff);
                        if (doRefresh) {
                            diff *= 2;
                        }

                        SwingUtilities.invokeAndWait(() -> {
                            try {
                                final NodeBase node = new TopLevelNode(name, Math.round(triplet.getSecond()), triplet.getThird(), classificationName);
                                classificationRoot.add(node);
                                if (doRefresh) {
                                    ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(classificationRoot);
                                    statusBar.setText2("Rows: " + countVisibleNodes());
                                }
                            } catch (Exception ex) {
                                // Basic.caught(ex);
                            }
                        });
                        needsFinalRefresh = !doRefresh;
                    }
                    progressListener.incrementProgress();
                }
            } finally {
                if (needsFinalRefresh) {
                    SwingUtilities.invokeLater(() -> ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(classificationRoot));
                }
            }
        });
    }

    /**
     * add a top-level node.
     *
     * @param name
     * @param key
     * @param classificationName
     */
    public void addTopLevelNode(final String name, final Object key, final String classificationName) {
        final NodeBase classificationRoot = classification2RootNode.get(classificationName);
        if (classificationRoot == null)
            return;
        if (!rootNode.isNodeChild(classificationRoot)) {
            try {
                rootNode.add(classificationRoot);
                ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(rootNode);
                statusBar.setText2("Rows: " + countVisibleNodes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dataTree.expandPath(new TreePath(classificationRoot));

        loader.execute(progressListener -> {
            progressListener.setMaximum(-1);

            boolean isPresent = false;
            // first determine whether taxon already present as child of parent:
            Enumeration level1Enumeration = classificationRoot.children();
            while (!isPresent && level1Enumeration.hasMoreElements()) {
                NodeBase level1Node = (NodeBase) level1Enumeration.nextElement();
                if (level1Node instanceof TopLevelNode && level1Node.getName().equals(name)) {
                    isPresent = true;
                }
            }
            if (!isPresent) {
                final int classId = (Integer) key;
                final int size = dir.getDocument().getConnector().getClassSize(classificationName, classId);
                SwingUtilities.invokeAndWait(() -> {
                    classificationRoot.add(new TopLevelNode(name, size, classId, classificationName));
                    ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(classificationRoot);
                });
            }
        });
    }

    /**
     * adds a list of reads below the root
     *
     * @param readBlock
     */
    public void addTopLevelReadNode(final IReadBlock readBlock, String classificationName) {
        final NodeBase classificationRoot = classification2RootNode.get(classificationName);
        if (classificationRoot == null)
            return;

        String readName = readBlock.getReadName();
        for (int i = 0; i < classificationRoot.getChildCount(); i++) {
            NodeBase child = (NodeBase) classificationRoot.getChildAt(i);
            if (child.getName().equals(readName))
                return;
        }

        final ReadHeadLineNode node = new ReadHeadLineNode(readBlock);

        try {
            SwingUtilities.invokeAndWait(() -> {
                classificationRoot.add(node);
                ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(classificationRoot);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            Basic.caught(e);
        }
    }

    /**
     * add all read level nodes below a top-level node
     *
     * @param parent
     */
    public void addChildren(final TopLevelNode parent) {
        loader.execute(progressListener -> {
            long lastRefreshTime = System.currentTimeMillis();
            long diff = 10;
            boolean needsFinalRefresh = false;

            try {
                progressListener.setMaximum((int) parent.getRank());
                progressListener.setProgress(0);

                IConnector connector = dir.getDocument().getConnector();

                try (IReadBlockIterator it = connector.getReadsIteratorForListOfClassIds(parent.getClassificationName(), parent.getClassIds(), 0, 100000, true, true)) {
                    while (it.hasNext()) {
                        IReadBlock readBlock = it.next();
                        if (readBlock != null) {
                            final ReadHeadLineNode node = new ReadHeadLineNode(readBlock);
                            final boolean doRefresh = (System.currentTimeMillis() - lastRefreshTime > diff);
                            if (doRefresh) {
                                if (diff == 10)
                                    diff = 50;
                                else
                                    diff *= 2;
                            }
                            SwingUtilities.invokeAndWait(() -> {
                                parent.add(node);
                                if (doRefresh) {
                                    if (sortReadsAlphabetically)
                                        sortChildrenAlphabetically(parent);
                                    ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(parent);
                                    statusBar.setText2("Rows: " + countVisibleNodes());
                                }
                            });
                            if (doRefresh)
                                lastRefreshTime = System.currentTimeMillis();
                            needsFinalRefresh = !doRefresh;
                        }
                        progressListener.incrementProgress();
                    }
                }
            } catch (CanceledException ex) {
                parent.setCompleted(false);
                throw ex;
            } finally {
                if (needsFinalRefresh) {
                    SwingUtilities.invokeLater(() -> {
                        if (sortReadsAlphabetically)
                            sortChildrenAlphabetically(parent);
                        ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(parent);
                        statusBar.setText2("Rows: " + countVisibleNodes());
                    });
                }
            }
        });
    }

    /**
     * add all match level nodes below a read-level node
     *
     * @param parent
     */
    public void addChildren(final ReadHeadLineNode parent, final String classificationName) {
        loader.execute(progressListener -> {
            boolean needsFinalRefresh = true; // true, because we add data node before entering loop

            try {
                progressListener.setMaximum((int) parent.getRank());
                progressListener.setProgress(0);

                IConnector connector = dir.getDocument().getConnector();

                long uId = parent.getUId();
                IReadBlock readBlock;
                try (IReadBlockGetter readBlockGetter = connector.getReadBlockGetter(0, 100000, true, true)) {
                    readBlock = readBlockGetter.getReadBlock(uId);
                }
                final String header = readBlock.getReadHeader();
                final String sequence = readBlock.getReadSequence();
                int length = readBlock.getReadLength();
                if (length == 0 && sequence != null)
                    length = Basic.getNumberOfNonSpaceCharacters(sequence);
                parent.setCompleted(true);
                final ReadDataHeadLineNode readDataHeadLineNode = new ReadDataHeadLineNode("DATA", length, readBlock.getComplexity(), readBlock.getReadWeight(), header + (sequence != null ? "\n" + sequence : ""));

                // add data node
                SwingUtilities.invokeAndWait(() -> parent.add(readDataHeadLineNode));

                long lastRefreshTime = System.currentTimeMillis();
                long diff = 10;

                final Document doc = dir.getDocument();
                final BitSet activeMatches = new BitSet();
                ActiveMatches.compute(doc.getMinScore(), doc.isLongReads() ? 100 : doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, classificationName, activeMatches);

                for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
                    IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                    final StringBuilder buf = new StringBuilder();
                    final int taxId = matchBlock.getTaxonId();

                    {
                        String taxonName = TaxonomyData.getName2IdMap().get(taxId);
                        if (taxonName == null) {
                            if (taxId > 0)
                                buf.append(String.format("%d;", taxId));
                            else
                                buf.append("?;");
                        } else
                            buf.append(taxonName).append(";");
                    }

                    for (String cName : doc.getActiveViewers()) {
                        if (!cName.equals(Classification.Taxonomy)) {
                            final int id = matchBlock.getId(cName);
                            if (id > 0) {
                                String label = ClassificationManager.get(cName, true).getName2IdMap().get(id);
                                if (label != null) {
                                    label = Basic.abbreviateDotDotDot(label, 50);
                                    buf.append(" ").append(label).append(";");
                                }
                            }
                        }
                    }

                    final MatchHeadLineNode node = new MatchHeadLineNode(buf.toString(), matchBlock.getBitScore(), matchBlock.isIgnore(), activeMatches.get(m), matchBlock.getUId(), taxId, matchBlock.getText());
                    // add match node

                    final boolean doRefresh = (System.currentTimeMillis() - lastRefreshTime > diff);
                    if (doRefresh) {
                        if (diff == 10)
                            diff = 50;
                        else
                            diff *= 2;
                    }
                    SwingUtilities.invokeAndWait(() -> {
                        parent.add(node);
                        if (doRefresh) {
                            ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(parent);
                            statusBar.setText2("Rows: " + countVisibleNodes());
                        }
                    });
                    if (doRefresh)
                        lastRefreshTime = System.currentTimeMillis();
                    needsFinalRefresh = !doRefresh;
                    progressListener.incrementProgress();
                }
            } catch (CanceledException ex) {
                parent.setCompleted(false);
                throw ex;
            } finally {
                if (needsFinalRefresh) {
                    SwingUtilities.invokeLater(() -> {
                        ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(parent);
                        statusBar.setText2("Rows: " + countVisibleNodes());
                    });
                }
            }
        });
    }

    /**
     * add children to read level data node
     *
     * @param parent
     */
    public void addChildren(final ReadDataHeadLineNode parent) {
        parent.add(new ReadDataTextNode(parent.getData()));
    }

    /**
     * add all read level nodes below a match-level node
     *
     * @param parent
     */
    public void addChildren(final MatchHeadLineNode parent) {
        loader.execute(progressListener -> {
            progressListener.setMaximum(-1);
            final MatchTextNode node = new MatchTextNode(parent.getMatchText() == null ? "Unknown" : parent.getMatchText());
            SwingUtilities.invokeAndWait(() -> {
                parent.add(node);
                ((DefaultTreeModel) dataTree.getModel()).nodeStructureChanged(parent);
            });
        });
    }

    /**
     * sort all children of a node by rank
     *
     * @param node
     */
    private void sortChildrenByRank(NodeBase node) {
        SortedSet<NodeBase> children = new TreeSet<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            NodeBase child = (NodeBase) node.getChildAt(i);
            children.add(child);
        }
        node.removeAllChildren();
        for (NodeBase a : children) {
            node.add(a);
        }
    }

    /**
     * sort all children of a node by rank
     *
     * @param node
     */
    private void sortChildrenAlphabetically(NodeBase node) {
        SortedSet<NodeBase> children = new TreeSet<>((n1, n2) -> {
            int value = n1.getName().compareTo(n2.getName());
            if (value == 0)
                return String.format("%5d", n1.getId()).compareTo(String.format("%5d", n2.getId()));
            else
                return value;
        });
        for (int i = 0; i < node.getChildCount(); i++) {
            NodeBase child = (NodeBase) node.getChildAt(i);
            children.add(child);
        }
        node.removeAllChildren();
        for (NodeBase a : children) {
            node.add(a);
        }
    }

    /**
     * return the frame associated with the viewer
     *
     * @return frame
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * gets the title
     *
     * @return title
     */
    public String getTitle() {
        return frame.getTitle();
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return uptodate;
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        frame.setVisible(false);
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        dir.removeViewer(this);
        searchManager.getFindDialogAsToolBar().close();
        // cancel anything that is running:
        if (getDir().getDocument().getProgressListener() != null)
            getDir().getDocument().getProgressListener().setUserCancelled(true);
        frame.dispose();
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        isLocked = true;
        getCommandManager().setEnableCritical(false);
        searchManager.getFindDialogAsToolBar().setEnableCritical(false);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusBar.setText2("Busy...");
        menuBar.setEnableRecentFileMenuItems(false);
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptodate = flag;
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        getCommandManager().setEnableCritical(true);
        searchManager.getFindDialogAsToolBar().setEnableCritical(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        statusBar.setText2("");
        menuBar.setEnableRecentFileMenuItems(true);
        isLocked = false;
    }

    /**
     * ask view to rescan itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what what should be updated? Possible values: Director.ALL or Director.TITLE
     */
    public void updateView(String what) {
        uptodate = false;
        if (doClear) {
            clear();
            doClear = false;
        }
        commandManager.updateEnableState();
        setTitle();
        statusBar.setText2("Rows: " + countVisibleNodes());
        uptodate = true;

        FindToolBar findToolBar = searchManager.getFindDialogAsToolBar();
        if (findToolBar.isClosing()) {
            showFindToolBar = false;
            findToolBar.setClosing(false);
        }
        if (!findToolBar.isEnabled() && showFindToolBar) {
            mainPanel.add(findToolBar, BorderLayout.NORTH);
            findToolBar.setEnabled(true);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        } else if (findToolBar.isEnabled() && !showFindToolBar) {
            mainPanel.remove(findToolBar);
            findToolBar.setEnabled(false);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        }
        if (findToolBar.isEnabled())
            findToolBar.clearMessage();
    }

    /**
     * count visible nodes
     *
     * @return number of visible nodes
     */
    private int countVisibleNodes() {
        int count = 0;
        for (NodeBase root : classification2RootNode.values()) {
            // todo: fix this
            Stack<NodeBase> stack = new Stack<>();
            if (root != null) {
                stack.push(root);
                while (stack.size() > 0) {
                    NodeBase node = stack.pop();
                    if (dataTree.isVisible(new TreePath(node.getPath())))
                        count++;
                    for (int i = 0; i < node.getChildCount(); i++) {
                        NodeBase child = (NodeBase) node.getChildAt(i);
                        stack.add(child);
                    }
                }
            }
        }
        return count;
    }

    /**
     * expand the given node
     *
     * @param v
     */
    public void expand(NodeBase v) {
        if (v != null) {
            for (Enumeration descendants = v.breadthFirstEnumeration(); descendants.hasMoreElements(); ) {
                v = (NodeBase) descendants.nextElement();
                dataTree.expandPath(new TreePath(v.getPath()));

            }
        }
    }

    /**
     * expand an array of paths
     *
     * @param paths
     */
    public void expand(TreePath[] paths) {
        boolean first = true;
        for (TreePath path : paths) {
            if (!dataTree.isExpanded(path)) {
                dataTree.expandPath(path);
                if (first) {
                    dataTree.repaint();
                    dataTree.scrollRowToVisible(dataTree.getRowForPath(path));
                    first = false;
                }
            }
        }
    }

    /**
     * collapse the given node   or root
     *
     * @param v
     */
    public void collapse(NodeBase v) {
        if (v != null) {
            for (Enumeration descendants = v.depthFirstEnumeration(); descendants.hasMoreElements(); ) {
                v = (NodeBase) descendants.nextElement();
                dataTree.collapsePath(new TreePath(v.getPath()));
            }
        }
    }

    /**
     * collapse an array of paths
     *
     * @param paths
     */
    public void collapse(TreePath[] paths) {
        for (TreePath path : paths) {
            collapse((NodeBase) path.getLastPathComponent());
        }
    }

    /**
     * set the title of the window
     */
    private void setTitle() {
        String newTitle = "Inspector - " + dir.getDocument().getTitle();

        if (isDirty())
            newTitle += "*";

        if (dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + dir.getID() + "] - " + ProgramProperties.getProgramVersion();

        if (!frame.getTitle().equals(newTitle)) {
            frame.setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    /**
     * returns the read head node in a path
     *
     * @param path
     * @return read head node or null
     */
    private ReadHeadLineNode getReadHeadLineNodeFromPath(TreePath path) {
        Object[] components = path.getPath();
        for (Object component : components) {
            if (component instanceof ReadHeadLineNode)
                return (ReadHeadLineNode) component;
        }
        return null;
    }

    /**
     * returns the match head node in a path
     *
     * @param path
     * @return read head node or null
     */
    private MatchHeadLineNode getMatchHeadLineNodeFromPath(TreePath path) {
        Object[] components = path.getPath();
        for (Object component : components) {
            if (component instanceof MatchHeadLineNode)
                return (MatchHeadLineNode) component;
        }
        return null;
    }

    /**
     * does window currently have a selected read headline node?
     *
     * @return true, if some read headline node is selected
     */
    public boolean hasSelectedReadHeadLineNodes() {
        if (dataTree != null) {
            TreePath[] paths = dataTree.getSelectionPaths();
            if (paths != null) {
                for (TreePath path : paths) {
                    if (getReadHeadLineNodeFromPath(path) != null)
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * get all selected read headline nodes
     *
     * @return all selected read headline nodes
     */
    public ArrayList<ReadHeadLineNode> getAllSelectedReadHeadLineNodes() {
        ArrayList<ReadHeadLineNode> list = new ArrayList<>();
        TreePath[] paths = dataTree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                ReadHeadLineNode node = getReadHeadLineNodeFromPath(path);
                if (node != null)
                    list.add(node);
            }
        }
        return list;
    }

    /**
     * does window currently have a selected read headline node?
     *
     * @return true, if some match headline node is selected
     */
    public boolean hasSelectedMatchHeadLineNodes() {
        if (dataTree != null) {
            TreePath[] paths = dataTree.getSelectionPaths();
            if (paths != null) {
                for (TreePath path : paths) {
                    if (getMatchHeadLineNodeFromPath(path) != null)
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * get all selected read headline nodes
     *
     * @return all selected read headline nodes
     */
    public ArrayList<MatchHeadLineNode> getAllSelectedMatchHeadLineNodes() {
        ArrayList<MatchHeadLineNode> list = new ArrayList<>();
        TreePath[] paths = dataTree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                MatchHeadLineNode node = getMatchHeadLineNodeFromPath(path);
                if (node != null)
                    list.add(node);
            }
        }
        return list;
    }

    private boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        if (dirty != isDirty) {
            isDirty = dirty;
            setTitle();
        }
    }

    public boolean isLocked() {
        return isLocked;
    }

    public boolean hasSelectedNodes() {
        return dataTree != null && dataTree.getSelectionCount() > 0;
    }

    /**
     * delete all the selected nodes
     */
    public void deleteSelectedNodes() {
        DefaultTreeModel model = (DefaultTreeModel) dataTree.getModel();
        TreePath[] selectedPaths = dataTree.getSelectionPaths();
        if (selectedPaths != null) {
            for (TreePath selectedPath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                node.removeAllChildren();
                model.nodeStructureChanged(node);
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                if (parent != null) {
                    parent.remove(node);
                    model.nodeStructureChanged(parent);
                }
            }
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

    public JTree getDataTree() {
        return dataTree;
    }

    /**
     * Print the graph associated with this viewer.
     *
     * @param gc0        the graphics context.
     * @param format     page format
     * @param pagenumber page index
     */
    public int print(Graphics gc0, PageFormat format, int pagenumber) throws PrinterException {
        Component panel = scrollPane.getViewport().getComponent(0);

        if (panel != null && pagenumber == 0) {
            Graphics2D gc = ((Graphics2D) gc0);
            Dimension dim = panel.getPreferredSize();
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

            panel.print(gc0);
            return Printable.PAGE_EXISTS;
        }
        return Printable.NO_SUCH_PAGE;
    }

    public NodeBase getRoot(String cName) {
        return classification2RootNode.get(cName);
    }

    public boolean isSortReadsAlphabetically() {
        return sortReadsAlphabetically;
    }

    public void setSortReadsAlphabetically(boolean sortReadsAlphabetically) {
        this.sortReadsAlphabetically = sortReadsAlphabetically;
    }

    public boolean isShowFindToolBar() {
        return showFindToolBar;
    }

    public void setShowFindToolBar(boolean showFindToolBar) {
        this.showFindToolBar = showFindToolBar;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }

    public String getSelection() {
        final StringBuilder buf = new StringBuilder();
        final TreePath[] selectedPaths = dataTree.getSelectionPaths();
        if (selectedPaths != null) {
            for (TreePath selectedPath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                buf.append(node.toString()).append("\n");
            }
        }
        return buf.toString();
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "InspectorWindow";
    }

    public Map<String, NodeBase> getClassification2RootNode() {
        return classification2RootNode;
    }

    @Override
    public boolean isReadsAvailable() {
        return getReads(1).size() > 0;
    }

    @Override
    public Collection<Pair<String, String>> getReads(int maxNumber) {
        ArrayList<Pair<String, String>> list = new ArrayList<>(Math.min(1000, maxNumber));
        if (dataTree != null) {
            final TreePath[] selectedPaths = dataTree.getSelectionPaths();
            if (selectedPaths != null) {
                for (TreePath selectedPath : selectedPaths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                    if (node instanceof ReadHeadLineNode) {
                        final ReadHeadLineNode readHeadLineNode = (ReadHeadLineNode) node;
                        if (readHeadLineNode.getReadHeader() != null && readHeadLineNode.getReadSequence() != null) {
                            list.add(new Pair<>(readHeadLineNode.getReadHeader(), readHeadLineNode.getReadSequence()));
                            if (list.size() >= maxNumber)
                                return list;
                        }
                    }
                }
            }
        }
        return list;
    }

    private Director getDir() {
        return dir;
    }
}

class MyTreeListener implements TreeWillExpandListener, TreeExpansionListener {
    private final Director dir;
    private final InspectorWindow inspectorWindow;

    MyTreeListener(Director dir, InspectorWindow inspectorWindow) {
        this.dir = dir;
        this.inspectorWindow = inspectorWindow;
    }

    /**
     * Invoked whenever a node in the tree is about to be collapsed.
     */
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        if (inspectorWindow.isLocked()) {
            if (dir.getDocument().getProgressListener() != null)
                dir.getDocument().getProgressListener().setUserCancelled(true);
            throw new ExpandVetoException(event);
        }
    }

    /**
     * Called whenever an item in the tree has been collapsed.
     */
    public void treeCollapsed(TreeExpansionEvent event) {
        final TreePath path = event.getPath();
        final NodeBase node = (NodeBase) path.getLastPathComponent();
        if (!inspectorWindow.getClassification2RootNode().containsValue(node)) {
            //node.removeAllChildren();
            DefaultTreeModel model = (DefaultTreeModel) inspectorWindow.dataTree.getModel();
            model.nodeStructureChanged(node);
        }
        inspectorWindow.updateView(Director.ALL);
    }

    /**
     * Called whenever an item in the tree has been expanded.
     */
    public void treeExpanded(TreeExpansionEvent event) {
        inspectorWindow.updateView(Director.ALL);
    }

    /**
     * Invoked whenever a node in the tree is about to be expanded.
     */
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        if (inspectorWindow.isLocked()) {
            if (dir.getDocument().getProgressListener() != null)
                dir.getDocument().getProgressListener().setUserCancelled(true);
            throw new ExpandVetoException(event);
        }

        final TreePath path = event.getPath();
        final NodeBase node = (NodeBase) path.getLastPathComponent();
        if (node.getChildCount() > 0) { // has already been expanded
            if (node.isCompleted())
                return;
            else {
                int result = JOptionPane.showConfirmDialog(inspectorWindow.getFrame(), "List of children incomplete, re-fetch?", "Re-fetch", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.NO_OPTION)
                    return;
                else if (result == JOptionPane.CANCEL_OPTION)
                    throw new ExpandVetoException(event);
                else // remove all children to trigger re-download
                    node.removeAllChildren();
            }
        }

        if (node instanceof TopLevelNode) {
            inspectorWindow.addChildren((TopLevelNode) node);
        } else if (node instanceof ReadHeadLineNode) {
            inspectorWindow.addChildren((ReadHeadLineNode) node, path.getPathComponent(1).toString());
        }
        if (node instanceof ReadDataHeadLineNode) {
            inspectorWindow.addChildren((ReadDataHeadLineNode) node);
        } else if (node instanceof MatchHeadLineNode) {
            inspectorWindow.addChildren((MatchHeadLineNode) node);
        }
    }
}
