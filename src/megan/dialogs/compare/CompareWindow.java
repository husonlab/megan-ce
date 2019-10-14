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
package megan.dialogs.compare;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ActionJList;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.compare.commands.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * dialog to set up comparison of data sets
 * Daniel Huson, 3.2007
 */
public class CompareWindow extends JDialog {
    private final ActionJList<MyListItem> jList;
    private final DefaultListModel<MyListItem> listModel;
    private Comparer.COMPARISON_MODE mode = Comparer.COMPARISON_MODE.RELATIVE;
    private boolean ignoreNoHits;
    private boolean keep1;


    private boolean canceled = false;

    private final CommandManager commandManager;

    /**
     * setup and display the compare dialog
     */
    public CompareWindow(JFrame parent, final Director dir, Collection<String> files) throws CanceledException {
        super(parent);

        mode = Comparer.COMPARISON_MODE.valueOfIgnoreCase(ProgramProperties.get("CompareWindowMode", Comparer.COMPARISON_MODE.RELATIVE.toString()));
        if (mode == null)
            mode = Comparer.COMPARISON_MODE.RELATIVE;
        ignoreNoHits = ProgramProperties.get("CompareWindowIgnoreHoHits", true);
        keep1 = ProgramProperties.get("CompareWindowKeep1", false);

        setIconImages(ProgramProperties.getProgramIconImages());

        commandManager = new CommandManager(dir, this, new String[]{"megan.dialogs.compare.commands"}, !ProgramProperties.isUseGUI());

        setTitle("Compare - " + ProgramProperties.getProgramVersion());

        if (parent != null)
            setLocationRelativeTo(parent);
        else
            setLocation(300, 300);
        setSize(500, 400);

        setModal(true);

        listModel = new DefaultListModel<>();
        jList = new ActionJList<>(listModel);
        jList.setTransferHandler(new ListItemTransferHandler());
        jList.setDropMode(DropMode.INSERT);
        jList.setDragEnabled(true);

        loadListFromOpenDocuments();
        if (files != null)
            loadList(files, false);

        jList.addListSelectionListener(listSelectionEvent -> commandManager.updateEnableState());

        getContentPane().setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        JPanel headerPanel = new JPanel();
        headerPanel.setBorder(new EmptyBorder(2, 5, 2, 5));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.LINE_AXIS));
        headerPanel.add(new JLabel("Select samples:"));
        headerPanel.add(Box.createHorizontalGlue());
        headerPanel.add(commandManager.getButton(AddFilesCommand.NAME));
        headerPanel.add(new JLabel(" " + AddFilesCommand.NAME));

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(new JScrollPane(jList), BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.CENTER);

        JPanel middle = new JPanel();
        middle.setLayout(new BoxLayout(middle, BoxLayout.LINE_AXIS));
        mainPanel.add(middle, BorderLayout.SOUTH);
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        JPanel lowerPanel = new JPanel();
        lowerPanel.setLayout(new BorderLayout());

        JPanel modePanel = new JPanel();
        modePanel.setLayout(new BorderLayout());
        modePanel.setBorder(new EmptyBorder(2, 5, 2, 5));

        JPanel modePanelLeft = new JPanel();
        modePanelLeft.setLayout(new BoxLayout(modePanelLeft, BoxLayout.Y_AXIS));

        final ButtonGroup group = new ButtonGroup();
        AbstractButton but = commandManager.getRadioButton(commandManager.getCommand(SetAbsoluteModeCommand.NAME));
        modePanelLeft.add(but);
        group.add(but);
        if (getMode().equals(Comparer.COMPARISON_MODE.ABSOLUTE))
            but.setSelected(true);

        but = commandManager.getRadioButton(commandManager.getCommand(SetNormalizedModeCommand.NAME));
        modePanelLeft.add(but);
        group.add(but);
        if (getMode().equals(Comparer.COMPARISON_MODE.RELATIVE))
            but.setSelected(true);

        final JPanel modePanelRight = new JPanel();
        modePanelRight.setLayout(new BoxLayout(modePanelRight, BoxLayout.Y_AXIS));
        modePanelRight.add(commandManager.getButton(SetIgnoreNoHitsCommand.NAME));
        modePanelRight.add(commandManager.getButton(SetKeep1Command.NAME));

        modePanel.add(modePanelLeft, BorderLayout.WEST);
        modePanel.add(modePanelRight, BorderLayout.EAST);

        lowerPanel.add(modePanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());

        JPanel bottomLeft = new JPanel();
        bottomLeft.setLayout(new BoxLayout(bottomLeft, BoxLayout.LINE_AXIS));
        bottomLeft.add(commandManager.getButton(SelectNoneCommand.NAME));
        bottomLeft.add(commandManager.getButton(SelectAllCommand.NAME));
        bottomPanel.add(bottomLeft, BorderLayout.WEST);

        JPanel bottomRight = new JPanel();
        bottomRight.setLayout(new BoxLayout(bottomRight, BoxLayout.LINE_AXIS));
        bottomRight.add(Box.createHorizontalGlue());
        bottomRight.add(Box.createHorizontalStrut(40));
        bottomRight.add(commandManager.getButton(CancelCommand.NAME));
        JButton applyButton = (JButton) commandManager.getButton(ApplyCommand.NAME);
        bottomRight.add(applyButton);
        getRootPane().setDefaultButton(applyButton);

        bottomPanel.add(bottomRight, BorderLayout.EAST);
        lowerPanel.add(bottomPanel, BorderLayout.SOUTH);

        getContentPane().add(lowerPanel, BorderLayout.SOUTH);
        commandManager.updateEnableState();
        setVisible(true);
    }

    /**
     * setup the list
     */
    private void loadListFromOpenDocuments() {
        java.util.List<IDirector> projects = ProjectManager.getProjects();

        for (IDirector project : projects) {
            if (project instanceof Director) {
                Director dir = (Director) project;

                if (!dir.getMainViewer().isLocked() && dir.getDocument().getNumberOfReads() > 0
                    /* && dir.getDocument().getNumberOfDatasets() == 1*/) {
                    listModel.addElement(new MyListItem(dir));
                }
            }
        }
    }

    /**
     * setup the list
     */
    private void loadList(Collection<String> files, boolean loadReadAssignmentMode) {

        int[] selectionIds = new int[files.size()];
        int f = 0;
        for (String file : files) {
            try {
                final MyListItem myListItem = new MyListItem(file, loadReadAssignmentMode);
                selectionIds[f++] = listModel.size();
                listModel.addElement(myListItem);
            } catch (Exception ex) {
                Basic.caught(ex);
            }
        }
        if (f > 0)
            jList.setSelectedIndices(selectionIds);
    }

    /**
     * get the comparison mode
     *
     * @return mode
     */
    public Comparer.COMPARISON_MODE getMode() {
        return mode;
    }

    /**
     * set the comparison mode
     *
     * @param mode
     */
    public void setMode(Comparer.COMPARISON_MODE mode) {
        this.mode = mode;
    }

    public boolean isIgnoreNoHits() {
        return ignoreNoHits;
    }

    public void setIgnoreNoHits(boolean ignoreNoHits) {
        this.ignoreNoHits = ignoreNoHits;
    }

    public boolean isKeep1() {
        return keep1;
    }

    public void setKeep1(boolean keep1) {
        this.keep1 = keep1;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * get the command string
     */
    public String getCommand() {
        ProgramProperties.put("CompareWindowMode", getMode().toString());
        ProgramProperties.put("CompareWindowIgnoreHoHits", isIgnoreNoHits());
        ProgramProperties.put("CompareWindowKeep1", isKeep1());

        java.util.List<MyListItem> selected = jList.getSelectedValuesList();

        System.err.println("Selected: " + selected.size());


        if (selected.size() >= 1) {

            final Document.ReadAssignmentMode readAssignmentMode = computeMajorityReadAssignmentMode(selected);

            StringBuilder buf = new StringBuilder();
            buf.append("compare");
            buf.append(" mode=").append(mode);
            buf.append(" readAssignmentMode=").append(readAssignmentMode.toString());
            buf.append(" keep1=").append(isKeep1());
            buf.append(" ignoreUnassigned=").append(isIgnoreNoHits());

            boolean first = true;
            for (Object aSelected : selected) {
                MyListItem item = (MyListItem) aSelected;
                if (item.getPID() >= 0) {
                    if (first) {
                        buf.append(" pid=");
                        first = false;
                    } else
                        buf.append(", ");
                    buf.append(item.getPID());
                }
            }
            first = true;
            for (MyListItem item : selected) {
                if (item.getPID() == -1) {
                    if (first) {
                        buf.append(" meganFile=");
                        first = false;
                    } else
                        buf.append(", ");
                    buf.append("'").append(item.getName()).append("'");
                }
            }
            buf.append(";");
            return buf.toString();
        } else
            return null;
    }

    /**
     * returns the most often mentioned read assignment mode
     *
     * @param list
     * @return most mentioned mode
     */
    private static Document.ReadAssignmentMode computeMajorityReadAssignmentMode(List<MyListItem> list) {
        Map<Document.ReadAssignmentMode, Integer> mode2count = new HashMap<>();
        for (MyListItem item : list) {
            mode2count.merge(item.getReadAssignmentMode(), 1, Integer::sum);
        }
        Document.ReadAssignmentMode readAssignmentMode = null;
        for (Document.ReadAssignmentMode mode : mode2count.keySet()) {
            if (readAssignmentMode == null || mode2count.get(mode) > mode2count.get(readAssignmentMode)
                    || (mode2count.get(mode).equals(mode2count.get(readAssignmentMode)) && readAssignmentMode == Document.ReadAssignmentMode.readCount))
                readAssignmentMode = mode;
        }
        return readAssignmentMode != null ? readAssignmentMode : Document.ReadAssignmentMode.readCount;
    }

    /**
     * add a file to the viewer
     *
     * @param fileName
     */
    public void addFile(final String fileName) {
        boolean ok = true;
        for (int i = 0; ok && i < listModel.getSize(); i++) {
            MyListItem item = listModel.getElementAt(i);
            if (item.getName() != null && fileName.equals(item.getName()))
                ok = false;
        }
        if (ok) {
            SwingUtilities.invokeLater(() -> {
                if (!isCanceled()) {
                    try {
                        MyListItem item = new MyListItem(fileName, true);
                        int index = listModel.size();
                        listModel.add(index, item);
                        int[] selection = jList.getSelectedIndices();
                        int[] newSelection = new int[selection.length + 1];
                        System.arraycopy(selection, 0, newSelection, 0, selection.length);
                        newSelection[selection.length] = index;
                        jList.setSelectedIndices(newSelection);
                        jList.repaint();
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                }
            });
        }
    }

    public JList getJList() {
        return jList;
    }

    public ListModel getListModel() {
        return listModel;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

}

//http://docs.oracle.com/javase/tutorial/uiswing/dnd/dropmodedemo.html
class ListItemTransferHandler extends TransferHandler {
    private final DataFlavor localObjectFlavor;
    private Object[] transferedObjects = null;

    public ListItemTransferHandler() {
        localObjectFlavor = new DataFlavor(MyListItem[].class, "Array of items");
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JList list = (JList) c;
        indices = list.getSelectedIndices();
        transferedObjects = list.getSelectedValuesList().toArray();
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{localObjectFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return Objects.equals(localObjectFlavor, flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (isDataFlavorSupported(flavor)) {
                    return transferedObjects;
                } else {
                    throw new UnsupportedFlavorException(flavor);
                }
            }
        };
    }

    @Override
    public boolean canImport(TransferSupport info) {
        return info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE; //TransferHandler.COPY_OR_MOVE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferSupport info) {
        if (!canImport(info)) {
            return false;
        }
        JList target = (JList) info.getComponent();
        JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
        DefaultListModel listModel = (DefaultListModel) target.getModel();
        int index = dl.getIndex();
        int max = listModel.getSize();
        if (index < 0 || index > max) {
            index = max;
        }
        addIndex = index;
        try {
            Object[] values = (Object[]) info.getTransferable().getTransferData(localObjectFlavor);
            addCount = values.length;
            for (Object value : values) {
                int idx = index++;
                listModel.add(idx, value);
                target.addSelectionInterval(idx, idx);
            }
            return true;
        } catch (UnsupportedFlavorException | IOException ex) {
            Basic.caught(ex);
        }
        return false;
    }

    @Override
    protected void exportDone(
            JComponent c, Transferable data, int action) {
        cleanup(c, action == MOVE);
    }

    private void cleanup(JComponent c, boolean remove) {
        if (remove && indices != null) {
            JList source = (JList) c;
            DefaultListModel model = (DefaultListModel) source.getModel();
            if (addCount > 0) {
                //http://java-swing-tips.googlecode.com/svn/trunk/DnDReorderList/src/java/example/MainPanel.java
                for (int i = 0; i < indices.length; i++) {
                    if (indices[i] >= addIndex) {
                        indices[i] += addCount;
                    }
                }
            }
            for (int i = indices.length - 1; i >= 0; i--) {
                model.remove(indices[i]);
            }
        }
        indices = null;
        addCount = 0;
        addIndex = -1;
    }

    private int[] indices = null;
    private int addIndex = -1; //Location where items were added
    private int addCount = 0;  //Number of items added.
}
