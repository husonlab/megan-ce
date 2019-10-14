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
package megan.groups;


import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.util.ActionJList;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import megan.core.Document;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * List of groups
 * Created by huson on 8/7/14.
 */
public class GroupsPanel extends JPanel {
    private final Document document;
    private final IDirectableViewer viewer;
    private final ActionJList<Group> jList;
    private final DefaultListModel<Group> listModel;

    private final static String NOT_GROUPED = "Ungrouped";

    private IGroupsChangedListener groupsChangedListener;

    private final static Color panelBackgroundColor = UIManager.getColor("Panel.background");
    private final static Color backgroundColor = new Color(230, 230, 230);

    private boolean inSelectSamples = false; // prevents bouncing while selecting

    private int numberOfGroups = 0;

    /**
     * constructor
     *
     * @param document
     */
    public GroupsPanel(Document document, IDirectableViewer viewer) {
        super(new BorderLayout());
        super.setBorder(BorderFactory.createTitledBorder("Groups"));
        this.document = document;
        this.viewer = viewer;
        listModel = new DefaultListModel<>();
        jList = new ActionJList<>(listModel);
        add(new JScrollPane(jList), BorderLayout.CENTER);
        jList.setCellRenderer(new MyCellRenderer());

        jList.setTransferHandler(new GroupTransferHandler(jList));
        jList.setDropMode(DropMode.INSERT);
        jList.setDragEnabled(true);
        MySelectionListener mySelectionListener = new MySelectionListener();
        jList.getSelectionModel().addListSelectionListener(mySelectionListener);

        jList.addMouseListener(new MyMouseListener());
        syncDocumentToPanel();
    }

    /**
     * sync document to list
     */
    public void syncDocumentToPanel() {
        ArrayList<Group> list = new ArrayList(document.getNumberOfSamples());
        String[] names = document.getSampleNamesAsArray();
        Set<String> groups = new HashSet<>();
        for (String name : names) {
            String groupId = document.getSampleAttributeTable().getGroupId(name);
            if (groupId == null)
                groupId = NOT_GROUPED;
            else
                groups.add(groupId);
            list.add(new Group(groupId, name));
        }
        numberOfGroups = groups.size();
        java.util.Collections.sort(list);

        listModel.clear();
        String previousId = "";
        for (Group group : list) {
            if (!previousId.equals(group.getGroupId()))
                listModel.addElement(new Group(group.getGroupId())); // add separator
            listModel.addElement(group);
            previousId = group.getGroupId();
        }
        if (!Objects.equals(previousId, NOT_GROUPED))
            listModel.addElement(new Group(NOT_GROUPED)); // add separator for ungrouped
        updateGroupSizes();
    }

    /**
     * sync current state of list to document
     */
    private void syncListToDocument() {
        updateGroupSizes();

        String groupId = null;
        for (int i = 0; i < listModel.getSize(); i++) {
            Group group = listModel.elementAt(i);
            if (group.isGroupHeader()) {
                groupId = group.getGroupId();
                if (Objects.equals(groupId, NOT_GROUPED))
                    groupId = null;
            } else {
                document.getSampleAttributeTable().putGroupId(group.getSampleName(), groupId);
            }
        }
        if (groupsChangedListener != null)
            groupsChangedListener.changed();
    }

    /**
     * updates the group size
     */
    private void updateGroupSizes() {
        int ungroupedCount = 0;

        Group currentGroup = null;
        int count = 0;
        for (int i = 0; i < listModel.getSize(); i++) {
            Group group = listModel.getElementAt(i);
            if (group.isGroupHeader()) {
                if (currentGroup == null)
                    ungroupedCount = count;
                else {
                    if (Objects.equals(currentGroup.getGroupId(), NOT_GROUPED))
                        currentGroup.setSize(count + ungroupedCount);
                    else
                        currentGroup.setSize(count);
                }
                count = 0;
                currentGroup = group;
            } else
                count++;
        }
        if (count > 0 && currentGroup != null) {
            if (Objects.equals(currentGroup.getGroupId(), NOT_GROUPED))
                currentGroup.setSize(count + ungroupedCount);
            else
                currentGroup.setSize(count);
        }
    }

    public void selectAll() {
        if (!inSelectSamples) {
            inSelectSamples = true;
            try {
                for (int i = 0; i < listModel.getSize(); i++) {
                    jList.setSelectionInterval(i, i);
                }
            } finally {
                inSelectSamples = false;
            }
        }
    }

    public void selectNone() {
        if (!inSelectSamples) {
            inSelectSamples = true;
            jList.clearSelection();
            inSelectSamples = false;
        }
    }

    /**
     * returns two selected group ids, if exactly two are selected, else null
     *
     * @return two selected groups or null
     */
    public Pair<String, String> getTwoSelectedGroups() {
        String firstId = null;
        int firstCount = 0;
        boolean groupHeader1 = false;
        String secondId = null;
        int secondCount = 0;
        boolean groupHeader2 = false;

        for (Group group : jList.getSelectedValuesList()) {
            if (Objects.equals(group.getGroupId(), NOT_GROUPED))
                return null;
            if (firstId == null) {
                firstId = group.getGroupId();
                firstCount++;
                if (group.isGroupHeader())
                    groupHeader1 = true;
            } else if (group.getGroupId().equals(firstId)) {
                firstCount++;
                if (group.isGroupHeader())
                    groupHeader1 = true;
            } else if (secondId == null) {
                secondId = group.getGroupId();
                secondCount++;
                if (group.isGroupHeader())
                    groupHeader2 = true;
            } else if (group.getGroupId().equals(secondId)) {
                secondCount++;
                if (group.isGroupHeader())
                    groupHeader2 = true;
            } else // a third group is selected
                return null;
        }
        // if group header not selected then all nodes in group must be selected:
        if (!groupHeader1 || !groupHeader2) {
            for (int i = 0; i < listModel.getSize(); i++) {
                Group group = listModel.get(i);
                if (!groupHeader1 && !group.isGroupHeader() && group.getGroupId().equals(firstId) && !jList.isSelectedIndex(i))
                    return null;
                if (!groupHeader2 && !group.isGroupHeader() && group.getGroupId().equals(secondId) && !jList.isSelectedIndex(i))
                    return null;
            }
        }
        if (firstId != null && secondId != null)
            return new Pair<>(firstId, secondId);
        else
            return null;
    }

    class MySelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                // The mouse button has not yet been released
            } else {
                if (!inSelectSamples) {
                    inSelectSamples = true;
                    try {
                        document.getSampleSelection().clear();
                        document.getSampleSelection().setSelected(getSelectedSamples(), true);
                        if (viewer != null)
                            viewer.updateView(IDirector.ENABLE_STATE);
                    } finally {
                        inSelectSamples = false;
                    }
                }
            }
        }
    }

    /**
     * get all currently selected samples
     *
     * @return selected
     */
    public Set<String> getSelectedSamples() {
        Set<String> selected = new HashSet<>();
        for (Group group : jList.getSelectedValuesList())
            if (!group.isGroupHeader())
                selected.add(group.getSampleName());
        return selected;
    }


    /**
     * replace the selection state of the named samples
     *
     * @param samples
     * @param select
     */
    public void selectSamples(Collection<String> samples, boolean select) {
        if (!inSelectSamples) {
            inSelectSamples = true;
            try {
                BitSet toSelect = new BitSet();

                for (int i = 0; i < listModel.getSize(); i++) {
                    Group group = listModel.get(i);
                    if (!group.isGroupHeader()) {
                        boolean contained = samples.contains(group.getSampleName());
                        if (contained && select || !contained && jList.isSelectedIndex(i))
                            toSelect.set(i);
                    }
                }
                jList.clearSelection();
                for (int i = toSelect.nextSetBit(0); i != -1; i = toSelect.nextSetBit(i + 1)) {
                    jList.addSelectionInterval(i, i);
                }
            } finally {
                inSelectSamples = false;
            }
        }
    }

    public int getNumberOfSamples() {
        return document.getNumberOfSamples();
    }

    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    class MyMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) { // double click on a group header selects that group
                int index = jList.locationToIndex(e.getPoint());
                if (index != -1) {
                    final Group groupHeader = listModel.get(index);
                    if (groupHeader.isGroupHeader()) {
                        for (int i = 0; i < listModel.getSize(); i++) {
                            if (i != index) {
                                final Group group = listModel.get(i);
                                if (!group.isGroupHeader() && group.getGroupId().equals(groupHeader.getGroupId())) {
                                    jList.addSelectionInterval(i, i);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void showPopupMenu(final MouseEvent e) {
        final int index = jList.locationToIndex(e.getPoint()); //select the item
        final Group group = listModel.getElementAt(index);
        if (group.isGroupHeader()) {
            JPopupMenu menu = new JPopupMenu();
            AbstractAction selectGroupAction = new AbstractAction("Select Group") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < listModel.getSize(); i++) {
                        Group aGroup = listModel.elementAt(i);
                        if (!aGroup.isGroupHeader() && aGroup.getGroupId().equals(group.getGroupId()))
                            jList.addSelectionInterval(i, i);
                    }
                    jList.removeSelectionInterval(index, index);
                }
            };

            selectGroupAction.setEnabled(group.isGroupHeader());
            menu.add(selectGroupAction);

            menu.addSeparator();

            {
                AbstractAction deleteAction = new AbstractAction("Delete Group" + (!Objects.equals(group.getGroupId(), NOT_GROUPED) ? " " + group.getGroupId() : "")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        listModel.remove(index);
                        syncListToDocument();
                    }
                };
                deleteAction.setEnabled(!Objects.equals(group.getGroupId(), NOT_GROUPED));
                menu.add(deleteAction);
            }
            AbstractAction newAction = new AbstractAction("Add New Group") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addNewGroup(index);
                }
            };
            menu.add(newAction);

            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * add a new group
     *
     * @param index
     */
    public void addNewGroup(int index) {
        if (index == -1)
            index = listModel.getSize();

        BitSet used = new BitSet();
        int insertHere = 0;
        for (int i = 0; i < listModel.getSize(); i++) {
            Group group = listModel.elementAt(i);
            if (group.isGroupHeader() && !Objects.equals(group.getGroupId(), NOT_GROUPED)) {
                if (Basic.isInteger(group.getGroupId()))
                    used.set((Basic.parseInt(group.getGroupId())));
            }
            if (Objects.equals(group.getGroupId(), NOT_GROUPED))
                insertHere = i;
        }
        if (index + 1 < insertHere)
            insertHere = index + 1;
        listModel.insertElementAt(new Group("" + used.nextClearBit(1)), insertHere);
        jList.removeSelectionInterval(index, index);
        syncListToDocument();
    }

    /**
     * cell renderer
     */
    static class MyCellRenderer implements ListCellRenderer<Group> {
        private final JPanel p = new JPanel(new BorderLayout());
        private final JLabel label = new JLabel("", JLabel.CENTER);
        private final LineBorder selectedBorder = (LineBorder) BorderFactory.createLineBorder(ProgramProperties.SELECTION_COLOR_DARKER);
        private final Border raisedBorder = BorderFactory.createRaisedBevelBorder();

        MyCellRenderer() {
            label.setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Group> list, Group value, int index, boolean isSelected, boolean cellHasFocus) {
            // icon.setIcon(value.icon);
            if (value.isGroupHeader()) {
                String groupId = value.getGroupId();
                if (!Objects.equals(groupId, NOT_GROUPED)) {
                    label.setText("Group " + value.getGroupId() + ":");
                    p.setToolTipText("Group " + value.getGroupId() + ", size: " + value.getSize());
                } else {
                    label.setText("Not grouped:");
                    p.setToolTipText("Not grouped: " + value.getSize());
                }
                p.setBorder(raisedBorder);
                p.setBackground(isSelected ? ProgramProperties.SELECTION_COLOR : panelBackgroundColor);
                label.setBackground(isSelected ? ProgramProperties.SELECTION_COLOR : panelBackgroundColor);
                label.setBorder(isSelected ? selectedBorder : null);
            } else {
                p.setToolTipText("Sample: " + value.getSampleName());
                label.setText(value.getSampleName());
                p.setBorder(isSelected ? selectedBorder : null);
                p.setBackground(isSelected ? ProgramProperties.SELECTION_COLOR : backgroundColor);
                label.setBackground(isSelected ? ProgramProperties.SELECTION_COLOR : backgroundColor);
                label.setBorder(null);
            }
            p.add(label, BorderLayout.SOUTH);
            return p;
        }
    }


    /**
     * get listener for groups changed events
     *
     * @return
     */
    public IGroupsChangedListener getGroupsChangedListener() {
        return groupsChangedListener;
    }

    /**
     * set listener for groups changed events
     *
     * @param groupsChangedListener
     */
    public void setGroupsChangedListener(IGroupsChangedListener groupsChangedListener) {
        this.groupsChangedListener = groupsChangedListener;
    }

    /**
     * groups changed listener interface
     */
    interface IGroupsChangedListener {
        void changed();
    }

    public class GroupTransferHandler extends TransferHandler {

        private final JList list;

        private final Set<Group> selected = new HashSet<>();

        public GroupTransferHandler(JList list) {
            this.list = list;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            boolean canImport = false;
            if (support.isDataFlavorSupported(GroupTransferable.GROUP_DATA_FLAVOR)) {
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                if (dl.getIndex() != -1) {
                    canImport = true;
                }
            }
            return canImport;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            try {
                List<Group> dropped = (List) data.getTransferData(GroupTransferable.GROUP_DATA_FLAVOR);
                for (Group group : dropped) {
                    ((DefaultListModel<Group>) list.getModel()).removeElement(group);
                }
                for (Group group : selected) {
                    int index = listModel.indexOf(group);
                    if (index != -1)
                        jList.addSelectionInterval(index, index);
                }
                selected.clear();
                syncListToDocument();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public boolean importData(TransferSupport support) {
            boolean accepted = false;
            if (support.isDrop()) {
                if (support.isDataFlavorSupported(GroupTransferable.GROUP_DATA_FLAVOR)) {

                    JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                    DefaultListModel<Group> model = (DefaultListModel<Group>) list.getModel();
                    int index = dl.getIndex();
                    boolean insert = dl.isInsert();

                    Transferable t = support.getTransferable();
                    try {
                        List<Group> dropped = (List) t.getTransferData(GroupTransferable.GROUP_DATA_FLAVOR);
                        selected.addAll(dropped);
                        for (Group group : dropped) {
                            if (insert) {
                                if (index >= model.getSize()) {
                                    model.addElement(group);
                                } else {
                                    if (model.removeElement(group))
                                        model.add(index, group); // this is wierd
                                    model.add(index, group);
                                }
                            } else {
                                model.addElement(group);
                            }
                        }
                        accepted = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return accepted;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new GroupTransferable(list.getSelectedValuesList());
        }

    }

    public static class GroupTransferable implements Transferable {

        static final DataFlavor GROUP_DATA_FLAVOR = new DataFlavor(Group.class, "GroupList");

        private final List<Group> groups;

        public GroupTransferable(List<Group> groups) {
            this.groups = new ArrayList<>(groups.size());
            Stack<Group> stack = new Stack<>();
            stack.addAll(groups);
            while (stack.size() > 0)
                this.groups.add(stack.pop()); // in reverse order
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{GROUP_DATA_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return GROUP_DATA_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            Object value;
            if (GROUP_DATA_FLAVOR.equals(flavor)) {
                value = groups;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
            return value;
        }

    }
}

class Group implements Comparable<Group> {
    private final String groupId;
    private int size;
    private final String name;

    public Group(String groupId) {
        this.groupId = groupId;
        this.name = null;
    }

    public Group(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getSampleName() {
        return name;
    }

    public boolean isGroupHeader() {
        return name == null;
    }

    public int compareTo(Group b) {
        int v = this.getGroupId().compareTo(b.getGroupId());
        if (v == 0) {
            v = this.getSampleName().compareTo(b.getSampleName());
        }
        return v;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
