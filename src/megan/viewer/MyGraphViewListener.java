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

import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.*;
import jloda.swing.util.Cursors;
import jloda.util.Basic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Listener for all GraphView events
 */
public class MyGraphViewListener implements IGraphViewListener {
    private final static ExecutorService service = Executors.newFixedThreadPool(1);

    private boolean inWait = false;

    final static boolean hasLabelXORBug = true;

    private final ViewerBase viewer;
    private final PhyloTree tree;
    private final NodeArray<Rectangle> node2bbox;

    private final int inClick = 1;
    protected final int inMove = 2;
    private final int inRubberband = 3;
    private final int inNewEdge = 4;
    private final int inMoveNodeLabel = 5;
    private final int inMoveEdgeLabel = 6;
    private final int inMoveInternalEdgePoint = 7;
    private final int inScrollByMouse = 8;
    private final int inMoveMagnifier = 9;
    private final int inResizeMagnifier = 10;

    private int current;
    private int downX;
    private int downY;
    private Rectangle selRect;
    private Point prevPt;
    private Point offset; // used by move node label

    private final NodeSet hitNodes;
    private final NodeSet hitNodeLabels;
    private final EdgeSet hitEdges;
    private final EdgeSet hitEdgeLabels;

    private final NodeSet nodesWithMovedLabels;

    private boolean inPopup = false;

    // is mouse still pressed?
    private boolean stillDownWithoutMoving = false;

    /**
     * Constructor
     *
     * @param gv        PhyloTreeView
     * @param node2bbox mapping of nodes to bounding boxes
     */
    public MyGraphViewListener(ViewerBase gv, NodeArray<Rectangle> node2bbox, NodeSet nodesWithMovedLabels) {
        viewer = gv;
        tree = (PhyloTree) gv.getGraph();
        this.node2bbox = node2bbox;
        this.nodesWithMovedLabels = nodesWithMovedLabels;

        hitNodes = new NodeSet(viewer.getGraph());
        hitNodeLabels = new NodeSet(viewer.getGraph());
        hitEdges = new EdgeSet(viewer.getGraph());
        hitEdgeLabels = new EdgeSet(viewer.getGraph());
    }

    /**
     * Mouse pressed.
     *
     * @param me MouseEvent
     */
    public void mousePressed(MouseEvent me) {
        downX = me.getX();
        downY = me.getY();
        selRect = null;
        prevPt = null;
        offset = new Point();
        current = 0;
        stillDownWithoutMoving = true;

        viewer.requestFocusInWindow();

        int magnifierHit = viewer.trans.getMagnifier().hit(downX, downY);

        if (magnifierHit != Magnifier.HIT_NOTHING) {
            switch (magnifierHit) {
                case Magnifier.HIT_MOVE:
                    current = inMoveMagnifier;
                    viewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    break;
                case Magnifier.HIT_RESIZE:
                    current = inResizeMagnifier;
                    viewer.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    break;
                case Magnifier.HIT_INCREASE_MAGNIFICATION:
                    if (viewer.trans.getMagnifier().increaseDisplacement())
                        viewer.repaint();
                    break;
                case Magnifier.HIT_DECREASE_MAGNIFICATION:
                    if (viewer.trans.getMagnifier().decreaseDisplacement())
                        viewer.repaint();
                    break;
                default:
                    break;
            }
            return;
        }


        getHits(downX, downY, false);
        int numHitNodes = hitNodes.size();
        viewer.fireDoPress(hitNodes);
        int numHitNodeLabels = hitNodeLabels.size();
        int numHitEdges = hitEdges.size();
        int numHitEdgeLabels = hitEdgeLabels.size();
        viewer.fireDoPress(hitEdges);

        if (me.isPopupTrigger()) {
            inPopup = true;
            viewer.setCursor(Cursor.getDefaultCursor());
            if (numHitNodes != 0) {
                viewer.fireNodePopup(me, hitNodes);
                viewer.resetCursor();
                return;
            } else if (numHitNodeLabels != 0) {
                viewer.fireNodeLabelPopup(me, hitNodeLabels);
                viewer.resetCursor();
                return;
            } else if (numHitEdges != 0) {
                viewer.fireEdgePopup(me, hitEdges);
                viewer.resetCursor();
                return;
            } else if (numHitEdgeLabels != 0) {
                viewer.fireEdgeLabelPopup(me, hitEdgeLabels);
                viewer.resetCursor();
                return;
            } else if (me.isPopupTrigger()) {
                viewer.firePanelPopup(me);
                viewer.resetCursor();
                return;
            }
        }

        if (numHitNodes == 0 && numHitNodeLabels == 0 && numHitEdges == 0 && numHitEdgeLabels == 0) {
            if (me.isAltDown() || me.isShiftDown()) {
                current = inRubberband;
                viewer.setCursor(Cursor.getDefaultCursor());
            } else {
                current = inScrollByMouse;
                viewer.setCursor(Cursors.getClosedHand());

                if (!inWait) {
                    service.execute(new Runnable() {
                        public void run() {
                            try {
                                inWait = true;
                                synchronized (this) {
                                    Thread.sleep(500);
                                }
                            } catch (InterruptedException ignored) {
                            }
                            if (stillDownWithoutMoving) {
                                current = inRubberband;
                                viewer.setCursor(Cursor.getDefaultCursor());
                            }
                            inWait = false;
                        }
                    });
                }
            }
        } else {
            viewer.setCursor(Cursor.getDefaultCursor());
            if (viewer.getAllowEdit() && numHitNodes == 1 && me.isControlDown()
                    && !me.isShiftDown())
                current = inNewEdge;
            else if (numHitNodes == 0 && numHitEdges == 0 && numHitNodeLabels > 0) {
                Node v = hitNodeLabels.getFirstElement();

                try {
                    if (!viewer.getSelected(v) || viewer.getLabel(v) == null)
                        return; // move labels only of selected node
                } catch (NotOwnerException ignored) {
                }
                current = inMoveNodeLabel;
                viewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

            } else if (numHitNodes == 0 && numHitEdges == 0 && numHitNodeLabels == 0) {
                Edge e = hitEdgeLabels.getFirstElement();
                try {
                    if (!viewer.getSelected(e) || viewer.getLabel(e) == null)
                        return; // move labels only of selected edges
                } catch (NotOwnerException ignored) {
                }

                current = inMoveEdgeLabel;
                viewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

            } else if (viewer.isAllowMoveInternalEdgePoints() && numHitEdges == 1)
                current = inMoveInternalEdgePoint;
        }
    }

    /**
     * find out what we have hit
     *
     * @param x
     * @param y
     * @param onlyOne
     */
    private void getHits(int x, int y, boolean onlyOne) {
        hitNodes.clear();
        hitNodeLabels.clear();
        hitEdges.clear();
        hitEdgeLabels.clear();
        Node root = tree.getRoot();
        if (root != null)
            getHitsRec(x, y, root, null, onlyOne);

        // need to look at all the node labels the user has dragged around
        for (Node v = nodesWithMovedLabels.getFirstElement(); v != null; v = nodesWithMovedLabels.getNextElement(v)) {
            if (viewer.getLabel(v) != null && viewer.getLabel(v).length() > 0 && viewer.getLocation(v) != null && viewer.getLabelVisible(v)
                    && viewer.getLabelRect(v) != null && viewer.getLabelRect(v).contains(x, y))
                hitNodeLabels.add(v);
            if (onlyOne)
                return;
        }
    }

    /**
     * recursively go down tree to find hits
     *
     * @param x
     * @param y
     * @param v
     * @param e0
     * @param onlyOne
     * @return has hit something
     */
    private boolean getHitsRec(int x, int y, Node v, Edge e0, boolean onlyOne) {
        Rectangle bbox0 = node2bbox.get(v);
        if (bbox0 == null)
            return false;
        Rectangle bbox = new Rectangle(bbox0.x - 100, bbox0.y - 40, bbox0.width + 1000, bbox0.height + 80);
        Rectangle bboxDC = new Rectangle();
        viewer.trans.w2d(bbox, bboxDC);
        boolean hit = false;

        Rectangle labelBox = null;
        final NodeView nv = viewer.getNV(v);
        if (nv != null && nv.getLabel() != null && nv.getLabel().length() > 0 && nv.getLabelVisible())
            labelBox = nv.getLabelRect(viewer.trans);

        if (bboxDC.contains(x, y) || v.getInDegree() == 0 || v.getOutDegree() == 0
                || (labelBox != null && labelBox.contains(x, y))) // always check root and leaves
        {
            if (viewer.getLocation(v) != null && viewer.getBox(v).contains(x, y)) {
                hitNodes.add(v);
                hit = true;
                if (onlyOne)
                    return true;
            }
            if (viewer.getLabel(v) != null && viewer.getLocation(v) != null && viewer.getLabelVisible(v) && viewer.getLabelRect(v) != null && viewer.getLabelRect(v).contains(x, y)) {
                hitNodeLabels.add(v);
                hit = true;
                if (onlyOne)
                    return true;
            }
            for (Edge f = v.getFirstAdjacentEdge(); !hit && f != null; f = v.getNextAdjacentEdge(f)) {
                if (f != e0) {
                    Node w = v.getOpposite(f);
                    NodeView vv = viewer.getNV(v);
                    NodeView wv = viewer.getNV(w);
                    if (vv.getLocation() == null || wv.getLocation() == null)
                        continue;
                    Point vp = vv.computeConnectPoint(wv.getLocation(), viewer.trans);
                    Point wp = wv.computeConnectPoint(vv.getLocation(), viewer.trans);

                    if (tree.findDirectedEdge(v, w) != null)
                        viewer.adjustBiEdge(Objects.requireNonNull(vp), Objects.requireNonNull(wp)); // adjust for parallel edge

                    if (viewer.getEV(f).hitEdge(vp, wp, viewer.trans, x, y, 3)) {
                        hitEdges.add(f);
                        hit = true;
                        if (onlyOne)
                            return true;
                    }
                    viewer.getEV(f).setLabelReferenceLocation(vp, wp, viewer.trans);
                    if (viewer.getLabel(f) != null && viewer.getLabel(f).length() > 0 && viewer.getLabelVisible(f) && viewer.getLabelRect(f) != null &&
                            viewer.getLabelRect(f).contains(x, y)) {
                        hitEdgeLabels.add(f);
                        hit = true;
                        if (onlyOne)
                            return true;
                    }
                    if (!hit)
                        hit = getHitsRec(x, y, w, f, onlyOne);
                    if (hit && onlyOne)
                        return true;
                }
            }
        }
        return hit;
    }

    /**
     * find out what we have hit
     *
     * @param rect
     */
    private void getHits(Rectangle rect) {
        hitNodes.clear();
        hitNodeLabels.clear();
        hitEdges.clear();
        hitEdgeLabels.clear();
        Node root = tree.getRoot();
        if (root != null)
            getHitsRec(rect, root, null);

        // need to look at all the node labels the user has dragged around
        for (Node v = nodesWithMovedLabels.getFirstElement(); v != null; v = nodesWithMovedLabels.getNextElement(v)) {
            if (viewer.getLabel(v) != null && viewer.getLocation(v) != null && viewer.getLabelVisible(v) && viewer.getLabelRect(v) != null
                    && rect.contains(viewer.getLabelRect(v)))
                hitNodeLabels.add(v);
        }
    }

    /**
     * recursively go down tree to find hits
     *
     * @param rect
     * @param v
     * @param e0
     * @return has hit something
     */
    private boolean getHitsRec(Rectangle rect, Node v, Edge e0) {
        Rectangle bbox = node2bbox.get(v);
        Rectangle bboxDC = new Rectangle();
        viewer.trans.w2d(bbox, bboxDC);
        int height = bboxDC.height;
        boolean hit = false;

        // grow box slightly to capture labels
        bboxDC.add(bboxDC.getMinX() - 20, bboxDC.getMinY() - 20);
        bboxDC.add(bboxDC.getMaxX() + 20, bboxDC.getMaxY() + 20);

        if (bboxDC.intersects(rect) || v.getInDegree() == 0 || v.getOutDegree() == 0) // always check root and leaves
        {
            if (viewer.getLocation(v) != null && rect.contains(viewer.getBox(v))) {
                hitNodes.add(v);
                hit = true;
            }
            if (viewer.getLabel(v) != null && viewer.getLocation(v) != null && viewer.getLabelVisible(v) && viewer.getLabelRect(v) != null
                    && rect.contains(viewer.getLabelRect(v))) {
                hitNodeLabels.add(v);
                hit = true;
            }
            if (viewer.getGraph().getDegree(v) > 1 && height / (viewer.getGraph().getDegree(v) - 1) < 2)
                return hit; // box is too small to see

            for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
                if (f != e0) {
                    Node w = f.getOpposite(v);
                    if (viewer.getLocation(v) != null && viewer.getLocation(w) != null &&
                            rect.contains(viewer.trans.w2d(viewer.getLocation(v)))
                            && rect.contains(viewer.trans.w2d(viewer.getLocation(w))))
                        hitEdges.add(f);

                    if (viewer.getLabel(f) != null && viewer.getLabelVisible(f) && viewer.getLabelRect(f) != null &&
                            rect.contains(viewer.getLabelRect(f))) {
                        hitEdgeLabels.add(f);
                        hit = true;
                    }
                    if (getHitsRec(rect, w, f))
                        hit = true;
                }
            }
        }
        return hit;
    }

    /**
     * Mouse released.
     *
     * @param me MouseEvent
     */
    public void mouseReleased(MouseEvent me) {
        viewer.resetCursor();
        stillDownWithoutMoving = false;

        if (current == inScrollByMouse) {
            return;
        }

        viewer.fireDoRelease(hitNodes);
        viewer.fireDoRelease(hitEdges);

        if (me.isPopupTrigger()) {
            inPopup = true;
            viewer.setCursor(Cursor.getDefaultCursor());

            if (hitNodes.size() != 0)
                viewer.fireNodePopup(me, hitNodes);
            else if (hitNodeLabels.size() != 0)
                viewer.fireNodeLabelPopup(me, hitNodeLabels);
            else if (hitEdges.size() != 0)
                viewer.fireEdgePopup(me, hitEdges);
            else if (hitEdgeLabels.size() != 0)
                viewer.fireEdgeLabelPopup(me, hitEdgeLabels);
            else
                viewer.firePanelPopup(me);
            viewer.resetCursor();

            return;
        }

        if (current == inRubberband) {
            Rectangle rect = new Rectangle(downX, downY, 0, 0);
            rect.add(me.getX(), me.getY());
            getHits(rect);
            selectNodesEdges(hitNodes, hitEdges, me.isShiftDown(), me.getClickCount());
            viewer.repaint();
        } else if (current == inNewEdge) {
            NodeSet firstHit = getHitNodes(downX, downY);

            if (firstHit.size() == 1) {
                Node v = firstHit.getFirstElement();
                NodeSet secondHit = getHitNodes(me.getX(), me.getY());

                Node w = null;
                if (secondHit.size() == 0) {
                    try {
                        Point2D location = viewer.trans.d2w(me.getPoint());
                        viewer.setDefaultNodeLocation(location);
                        Edge e = viewer.newEdge(v, null);
                        if (e != null) {
                            w = viewer.getGraph().getTarget(e);
                            viewer.setLocation(w, location);
                        }
                    } catch (NotOwnerException ignored) {
                    }
                } else if (secondHit.size() == 1) {
                    w = secondHit.getFirstElement();

                    if (w != null) {
                        if (v != w) {
                            viewer.newEdge(v, w);
                        }
                    }
                }
                viewer.repaint();
            }
        } else if (current == inMoveNodeLabel) {
            viewer.repaint();
        } else if (current == inMoveEdgeLabel) {
            viewer.repaint();
        }
    }


    /**
     * Mouse entered.
     *
     * @param me MouseEvent
     */
    public void mouseEntered(MouseEvent me) {
    }

    /**
     * Mouse exited.
     *
     * @param me MouseEvent
     */
    public void mouseExited(MouseEvent me) {
        stillDownWithoutMoving = false;
    }

    /**
     * Mouse clicked.
     *
     * @param me MouseEvent
     */
    public void mouseClicked(MouseEvent me) {
        int meX = me.getX();
        int meY = me.getY();

        if (inPopup) {
            inPopup = false;
            return;
        }

        getHits(meX, meY, false);

        if (current == inScrollByMouse) // in navigation mode, double-click to lose selection
        {
            if (hitNodes.size() == 0 && hitEdges.size() == 0 && hitNodeLabels.size() == 0 && hitEdgeLabels.size() == 0) {
                viewer.selectAllNodes(false);
                viewer.selectAllEdges(false);
                viewer.repaint();
                return;
            }
        }
        current = inClick;

        if (hitNodes.size() != 0)
            viewer.fireDoClick(hitNodes, me.getClickCount());
        if (hitEdges.size() != 0)
            viewer.fireDoClick(hitEdges, me.getClickCount());
        if (hitNodeLabels.size() != 0)
            viewer.fireDoClickLabel(hitNodeLabels, me.getClickCount());
        if (hitEdgeLabels.size() != 0)
            viewer.fireDoClickLabel(hitEdgeLabels, me.getClickCount());

        if (me.getClickCount() == 1) {
            if (!hitNodes.isEmpty() || !hitEdges.isEmpty())
                selectNodesEdges(hitNodes, hitEdges, me.isShiftDown(), me.getClickCount());
            else if (!hitNodeLabels.isEmpty() || !hitEdgeLabels.isEmpty())
                selectNodesEdges(hitNodeLabels, hitEdgeLabels, me.isShiftDown(), me.getClickCount());
        }
        if (me.getClickCount() == 2 && viewer.getAllowEdit() && hitNodes.size() == 0 && hitEdges.size() == 0) {
            // New node:
            if (viewer.getAllowNewNodeDoubleClick()) {
                viewer.setDefaultNodeLocation(viewer.trans.d2w(me.getPoint()));
                Node v = viewer.newNode();
                if (v != null) {
                    try {
                        viewer.setLocation(v, viewer.trans.d2w(me.getPoint()));
                        viewer.setDefaultNodeLocation(viewer.trans.d2w(new Point(meX + 10, meY + 10)));
                        viewer.repaint();
                    } catch (NotOwnerException ignored) {
                    }
                }
            }
        } else if (me.getClickCount() == 3 && viewer.isAllowInternalEdgePoints() && hitNodes.size() == 0 && hitEdges.size() == 1) {
            Edge e = hitEdges.getFirstElement();
            try {
                EdgeView ev = viewer.getEV(e);
                Point vp = viewer.trans.w2d(viewer.getLocation(viewer.getGraph().getSource(e)));
                Point wp = viewer.trans.w2d(viewer.getLocation(viewer.getGraph().getTarget(e)));
                int index = ev.hitEdgeRank(vp, wp, viewer.trans, me.getX(), meY, 3);
                java.util.List<Point2D> list = viewer.getInternalPoints(e);
                Point2D aptWorld = viewer.trans.d2w(me.getPoint());
                if (list == null) {
                    list = new LinkedList<>();
                    list.add(aptWorld);
                    viewer.setInternalPoints(e, list);
                } else
                    list.add(index, aptWorld);
            } catch (NotOwnerException ex) {
                Basic.caught(ex);
            }
        } else if (me.getClickCount() == 2
                && ((viewer.isAllowEditNodeLabelsOnDoubleClick() && hitNodeLabels.size() > 0)
                || (viewer.isAllowEditNodeLabelsOnDoubleClick() && hitNodes.size() > 0))) {
            try {
// undo node label
                Node v;
                if (hitNodeLabels.size() > 0)
                    v = hitNodeLabels.getLastElement();
                else
                    v = hitNodes.getLastElement();
                String label = viewer.getLabel(v);
                label = JOptionPane.showInputDialog(viewer.getFrame(), "Edit Node Label:", label);
                if (label != null && !label.equals(viewer.getLabel(v))) {
                    viewer.setLabel(v, label);
                    viewer.setLabelVisible(v, label.length() > 0);
                    viewer.repaint();
                }
            } catch (NotOwnerException ex) {
                Basic.caught(ex);
            }

        } else if (me.getClickCount() == 2 &&
                ((viewer.isAllowEditEdgeLabelsOnDoubleClick() && hitEdgeLabels.size() > 0)
                        || (viewer.isAllowEditEdgeLabelsOnDoubleClick() && hitEdges.size() > 0))) {
            try {
                Edge e;
                if (hitEdgeLabels.size() > 0)
                    e = hitEdgeLabels.getLastElement();
                else
                    e = hitEdges.getLastElement();
                String label = viewer.getLabel(e);
                label = JOptionPane.showInputDialog(viewer.getFrame(), "Edit Edge Label:", label);
                if (label != null && !label.equals(viewer.getLabel(e))) {
                    viewer.setLabel(e, label);
                    viewer.setLabelVisible(e, label.length() > 0);
                    viewer.repaint();
                }
            } catch (NotOwnerException ex) {
                Basic.caught(ex);
            }
        }
        current = 0;
    }


    /**
     * Mouse dragged.
     *
     * @param me MouseEvent
     */
    public void mouseDragged(MouseEvent me) {
        stillDownWithoutMoving = false;
        if (current == inScrollByMouse) {
            viewer.setCursor(Cursors.getClosedHand());
            JScrollPane scrollPane = viewer.getScrollPane();
            int dX = me.getX() - downX;
            int dY = me.getY() - downY;

            if (dY != 0) {
                JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
                int amount = Math.round(dY * (scrollBar.getMaximum() - scrollBar.getMinimum()) / viewer.getHeight());
                if (amount != 0) {
                    scrollBar.setValue(scrollBar.getValue() - amount);
                }
            }
            if (dX != 0) {
                JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
                int amount = Math.round(dX * (scrollBar.getMaximum() - scrollBar.getMinimum()) / viewer.getWidth());
                if (amount != 0) {
                    scrollBar.setValue(scrollBar.getValue() - amount);
                }
            }
        } else if (current == inRubberband) {
            Graphics2D gc = (Graphics2D) viewer.getGraphics();

            if (gc != null) {
                gc.setXORMode(viewer.getCanvasColor());
                if (selRect != null)
                    gc.drawRect(selRect.x, selRect.y, selRect.width, selRect.height);
                selRect = new Rectangle(downX, downY, 0, 0);
                selRect.add(me.getX(), me.getY());
                gc.drawRect(selRect.x, selRect.y, selRect.width, selRect.height);
            }
        } else if (viewer.isAllowInternalEdgePoints() &&
                current == inMoveInternalEdgePoint) {
            Point p1 = new Point(downX, downY); // old [pos
            Edge e = getHitEdges(downX, downY).getFirstElement();

            downX = me.getX();
            downY = me.getY();
            Point p2 = new Point(downX, downY);     // new pos

            try {
                if (e != null) {
                    viewer.getEV(e).moveInternalPoint(viewer.trans, p1, p2);
                    viewer.repaint();
                }
            } catch (NotOwnerException ignored) {
            }

        } else if (current == inMoveNodeLabel) {
            if (hitNodeLabels.size() > 0) {
                Node v = hitNodeLabels.getFirstElement();

                try {
                    if (!viewer.getSelected(v) || viewer.getNV(v).getLabel() == null)
                        return; // move labels only of selected node
                } catch (NotOwnerException e) {
                    return; // move labels only of selected node
                }

                NodeView nv = viewer.getNV(v);

                Graphics2D gc = (Graphics2D) viewer.getGraphics();

                if (gc != null) {
                    Point apt = viewer.trans.w2d(nv.getLocation());
                    int meX = me.getX();
                    int meY = me.getY();
                    gc.setXORMode(viewer.getCanvasColor());
                    if (prevPt != null) {
                        gc.drawLine(apt.x, apt.y, prevPt.x, prevPt.y);
                    } else {
                        prevPt = new Point(downX, downY);
                        Point labPt = nv.getLabelPosition(viewer.trans);
                        if (labPt != null) {
                            offset.x = labPt.x - downX;
                            offset.y = labPt.y - downY;
                        }
                    }
                    gc.drawLine(apt.x, apt.y, meX, meY);
                    nv.hiliteLabel(gc, viewer.trans, viewer.getFont());


                    int labX = meX + offset.x;
                    int labY = meY + offset.y;

                    nv.setLabelPositionRelative(labX - apt.x, labY - apt.y);
                    nv.hiliteLabel(gc, viewer.trans, viewer.getFont());

                    prevPt.x = meX;
                    prevPt.y = meY;

                    nodesWithMovedLabels.add(v);
                }
            }
        } else if (current == inMoveEdgeLabel) {
            if (hitEdgeLabels.size() > 0) {
                try {
                    Edge e = hitEdgeLabels.getFirstElement();
                    try {
                        if (!viewer.getSelected(e) || viewer.getEV(e).getLabel() == null)
                            return; // move labels only of selected edges
                    } catch (NotOwnerException ex) {
                        return; // move labels only of selected edges
                    }
                    EdgeView ev = viewer.getEV(e);

                    Point2D nextToV;
                    Point2D nextToW;

                    Graph G = viewer.getGraph();
                    NodeView vv = viewer.getNV(G.getSource(e));
                    NodeView wv = viewer.getNV(G.getTarget(e));

                    nextToV = wv.getLocation();
                    nextToW = vv.getLocation();
                    if (viewer.getInternalPoints(e) != null) {
                        if (viewer.getInternalPoints(e).size() != 0) {
                            nextToV = viewer.getInternalPoints(e).get(0);
                            nextToW = viewer.getInternalPoints(e).get(viewer.getInternalPoints(e).size() - 1);
                        }
                    }
                    Point pv = vv.computeConnectPoint(nextToV, viewer.trans);
                    Point pw = wv.computeConnectPoint(nextToW, viewer.trans);

                    if (G.findDirectedEdge(G.getTarget(e), G.getSource(e)) != null)
                        viewer.adjustBiEdge(Objects.requireNonNull(pv), Objects.requireNonNull(pw)); // want parallel bi-edges

                    Graphics2D gc = (Graphics2D) viewer.getGraphics();

                    if (gc != null) {
                        ev.setLabelReferenceLocation(nextToV, nextToW, viewer.trans);
                        ev.setLabelSize(gc);

                        Point apt = ev.getLabelReferencePoint();
                        int meX = me.getX();
                        int meY = me.getY();
                        gc.setXORMode(viewer.getCanvasColor());
                        if (prevPt != null)
                            gc.drawLine(apt.x, apt.y, prevPt.x, prevPt.y);
                        else {
                            prevPt = new Point(downX, downY);
                            Point labPt = ev.getLabelPosition(viewer.trans);
                            offset.x = Objects.requireNonNull(labPt).x - downX;
                            offset.y = labPt.y - downY;
                        }
                        gc.drawLine(apt.x, apt.y, meX, meY);
                        ev.hiliteLabel(gc, viewer.trans);
                        int labX = meX + offset.x;
                        int labY = meY + offset.y;

                        ev.setLabelPositionRelative(labX - apt.x, labY - apt.y);
                        ev.hiliteLabel(gc, viewer.trans);

                        prevPt.x = meX;
                        prevPt.y = meY;
                    }
                } catch (NotOwnerException ex) {
                    Basic.caught(ex);
                }
            }
        } else if (current == inNewEdge) {
            Graphics gc = viewer.getGraphics();

            if (gc != null) {
                gc.setXORMode(viewer.getCanvasColor());
                if (selRect != null) // we misuse the selRect here...
                    gc.drawLine(downX, downY, selRect.x, selRect.y);
                selRect = new Rectangle(me.getX(), me.getY(), 0, 0);
                gc.drawLine(downX, downY, me.getX(), me.getY());
            }
        } else if (current == inMoveMagnifier) {
            int meX = me.getX();
            int meY = me.getY();
            if (meX != downX || meY != downY) {
                viewer.trans.getMagnifier().move(downX, downY, meX, meY);
                downX = meX;
                downY = meY;
                viewer.repaint();
            }
        } else if (current == inResizeMagnifier) {
            int meY = me.getY();
            if (meY != downY) {
                viewer.trans.getMagnifier().resize(downY, meY);
                downX = me.getX();
                downY = meY;
                viewer.repaint();
            }
        }
    }

    /**
     * Mouse moved.
     *
     * @param me MouseEvent
     */
    public void mouseMoved(MouseEvent me) {
        stillDownWithoutMoving = false;
        if (!viewer.isLocked()) {
            Node v = getAHitNodeOrNodeLabel(me.getX(), me.getY());
            if (v != null)
                viewer.setToolTipText(v);
            else
                viewer.setToolTipText((String) null);
        } else
            viewer.setToolTipText((String) null);
    }

    /**
     * Updates the selection of nodes and edges.
     *
     * @param hitNodes NodeSet
     * @param hitEdges EdgeSet
     * @param shift    boolean
     * @param clicks   int
     */
    private void selectNodesEdges(NodeSet hitNodes, EdgeSet hitEdges, boolean shift, int clicks) {
        if (hitNodes.size() == 1) // in this case, only do node selection
            hitEdges.clear();

        Graph G = viewer.getGraph();

        boolean changed = false;

        // no shift, deselect everything:
        if (!shift && (viewer.getNumberSelectedNodes() > 0 || viewer.getNumberSelectedEdges() > 0)) {
            viewer.selectAllNodes(false);
            viewer.selectAllEdges(false);
            changed = true;
        }

        try {
            if ((clicks > 0 || viewer.isAllowRubberbandNodes()) && hitNodes.size() > 0) {
                for (Node v = G.getFirstNode(); v != null; v = G.getNextNode(v)) {
                    if (hitNodes.contains(v)) {
                        if (!shift) {
                            viewer.setSelected(v, true);
                            changed = true;
                            if (clicks > 1)
                                break;
                        } else // shift==true
                        {
                            if (!viewer.getSelected(v))
                                viewer.setSelected(v, true);
                            else //
                                viewer.setSelected(v, false);
                            changed = true;
                        }
                    }
                }
            }

            if ((clicks > 0 || viewer.isAllowRubberbandEdges()) && hitEdges.size() > 0) {
                for (Edge e = G.getFirstEdge(); e != null; e = G.getNextEdge(e)) {
                    if (hitEdges.contains(e)) {
                        if (!shift) {
                            if (clicks == 0 || viewer.getNumberSelectedNodes() == 0) {
                                viewer.setSelected(e, true);
                                // selectedNodes.insert(tree.source(e));
                                // selectedNodes.insert(tree.target(e));
                                changed = true;
                            }
                            if (clicks > 1)
                                break;
                        } else // shift==true
                        {
                            if (!viewer.getSelected(e)) {
                                viewer.setSelected(e, true);
                                // selectedNodes.insert(tree.source(e));
                                // selectedNodes.insert(tree.target(e));
                            } else // selectedEdges.member(e)
                                viewer.setSelected(e, false);
                            changed = true;
                        }
                    }
                }
            }
        } finally {
            if (changed)
                viewer.repaint();
        }
    }

    /**
     * Get the hit nodes.
     *
     * @param rect Rectangle
     * @return hit_nodes NodeSet
     */
    NodeSet getHitNodes(Rectangle rect) {
        getHits(rect);
        return hitNodes;
    }

    /**
     * Get the hit nodes.
     *
     * @param x int
     * @param y int
     * @return hit_nodes NodeSet
     */
    private NodeSet getHitNodes(int x, int y) {
        getHits(x, y, false);
        return hitNodes;
    }

    /**
     * Get the hit nodes.
     *
     * @param x int
     * @param y int
     * @return hit_nodes NodeSet
     */
    private Node getAHitNodeOrNodeLabel(int x, int y) {
        getHits(x, y, true);
        if (hitNodes.size() > 0)
            return hitNodes.getFirstElement();
        if (hitNodeLabels.size() > 0)
            return hitNodeLabels.getFirstElement();
        return null;
    }

    /**
     * Get the hit node labels.
     *
     * @param x int
     * @param y int
     * @return nodes whose labels have been hit
     */
    NodeSet getHitNodeLabels(int x, int y) {
        getHits(x, y, false);
        return hitNodeLabels;
    }

    /**
     * Get the hit edge labels.
     *
     * @param x int
     * @param y int
     * @return edges whose labels have been hit
     */
    EdgeSet getHitEdgeLabels(int x, int y) {
        getHits(x, y, false);
        return hitEdgeLabels;
    }

    /**
     * Get the hit edges.
     *
     * @param rect Rectangle
     * @return hit_edges EdgeSet
     */
    EdgeSet getHitEdges(Rectangle rect) {
        getHits(rect);
        return hitEdges;
    }

    /**
     * Get the hit edges.
     *
     * @param x int
     * @param y int
     * @return hit_edges EdgeSet
     */
    private EdgeSet getHitEdges(int x, int y) {
        getHits(x, y, false);
        return hitEdges;
    }

    // KeyListener methods:

    /**
     * Key typed
     *
     * @param ke Keyevent
     */
    public void keyTyped(KeyEvent ke) {
    }

    /**
     * Key pressed
     *
     * @param ke KeyEvent
     */
    public void keyPressed(KeyEvent ke) {
        JScrollPane scrollPane = viewer.getScrollPane();

        if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
            JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
            if (!ke.isShiftDown() && scrollBar.getVisibleAmount() < scrollBar.getMaximum()) {
                scrollBar.setValue(scrollBar.getValue() + scrollBar.getBlockIncrement(1));
            } else {
                double scale = viewer.trans.getScaleX() / 1.1;
                if (scale >= MainViewer.XMIN_SCALE)
                    viewer.trans.composeScale(1 / 1.1, 1.0);
            }
        } else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
            JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
            if (!ke.isShiftDown() && scrollBar.getVisibleAmount() < scrollBar.getMaximum()) {
                scrollBar.setValue(scrollBar.getValue() - scrollBar.getBlockIncrement(1));
            } else { //centerAndScale
                double scale = 1.1 * viewer.trans.getScaleX();
                if (scale <= MainViewer.XMAX_SCALE) {
                    viewer.trans.composeScale(1.1, 1.0);
                }
            }
        } else if (ke.getKeyCode() == KeyEvent.VK_UP) {
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            if (!ke.isShiftDown() && scrollBar.getVisibleAmount() < scrollBar.getMaximum()) {
                scrollBar.setValue(scrollBar.getValue() - scrollBar.getBlockIncrement(1));
            } else { //centerAndScale
                double scale = 1.1 * viewer.trans.getScaleY();
                if (scale <= MainViewer.YMAX_SCALE) {
                    viewer.trans.composeScale(1, 1.1);
                }
            }
        } else if (ke.getKeyCode() == KeyEvent.VK_PAGE_UP) { //centerAndScale
            double scale = 1.1 * viewer.trans.getScaleY();
            if (scale <= MainViewer.YMAX_SCALE) {
                viewer.trans.composeScale(1, 1.1);
            }
        } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            if (!ke.isShiftDown() && scrollBar.getVisibleAmount() < scrollBar.getMaximum()) {
                scrollBar.setValue(scrollBar.getValue() + scrollBar.getBlockIncrement(1));
            } else { //centerAndScale
                double scale = viewer.trans.getScaleY() / 1.1;
                if (scale >= MainViewer.YMIN_SCALE) {
                    viewer.trans.composeScale(1, 1 / 1.1);
                }
            }
        } else if (ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN) { //centerAndScale
            double scale = viewer.trans.getScaleY() / 1.1;
            if (scale >= MainViewer.YMIN_SCALE) {
                viewer.trans.composeScale(1, 1 / 1.1);
            }
        } else if (ke.getKeyCode() == KeyEvent.VK_DELETE && viewer.getAllowEdit()) {
            viewer.delSelectedEdges();
            viewer.delSelectedNodes();
        } else if (ke.getKeyCode() == KeyEvent.VK_SPACE) {
        } else if (ke.getKeyChar() == 'c') {
            viewer.centerGraph();
        } else if ((ke.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
            viewer.setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Key released
     *
     * @param ke KeyEvent
     */
    public void keyReleased(KeyEvent ke) {
        if ((ke.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
            viewer.resetCursor();
        }
    }

    // ComponentListener methods:

    /**
     * component hidded
     *
     * @param ev ComponentEvent
     */
    public void componentHidden(ComponentEvent ev) {
    }

    /**
     * component moved
     *
     * @param ev ComponentEvent
     */
    public void componentMoved(ComponentEvent ev) {
    }

    /**
     * component resized
     *
     * @param ev ComponentEvent
     */
    public void componentResized(ComponentEvent ev) {
        viewer.setSize(viewer.getSize());
    }

    /**
     * component shown
     *
     * @param ev ComponentEvent
     */
    public void componentShown(ComponentEvent ev) {
    }

    /**
     * react to a mouse wheel event
     *
     * @param e
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            boolean doScaleVertical = !e.isMetaDown() && !e.isAltDown() && !e.isShiftDown();
            boolean doScaleHorizontal = !e.isMetaDown() && !e.isControlDown() && !e.isAltDown() && e.isShiftDown();
            boolean doScrollVertical = !e.isMetaDown() && e.isAltDown() && !e.isShiftDown();
            boolean doScrollHorizontal = !e.isMetaDown() && e.isAltDown() && e.isShiftDown();

            boolean useMag = viewer.trans.getMagnifier().isActive();
            viewer.trans.getMagnifier().setActive(false);

            if (doScrollVertical) { //scroll
                viewer.getScrollPane().getVerticalScrollBar().setValue(viewer.getScrollPane().getVerticalScrollBar().getValue() + e.getUnitsToScroll());
            } else if (doScaleVertical) { //centerAndScale
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans, e.getPoint());

                double toScroll = 1.0 + (e.getUnitsToScroll() / 100.0);
                double s = (toScroll > 0 ? 1.0 / toScroll : toScroll);
                double scale = s * viewer.trans.getScaleY();
                if (scale >= MainViewer.YMIN_SCALE && scale <= MainViewer.YMAX_SCALE) {
                    viewer.trans.composeScale(1, s);
                    spa.adjust(false, true);
                }

            } else if (doScrollHorizontal) {
                viewer.getScrollPane().getHorizontalScrollBar().setValue(viewer.getScrollPane().getHorizontalScrollBar().getValue() + e.getUnitsToScroll());
            } else if (doScaleHorizontal) { //centerAndScale
                ScrollPaneAdjuster spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans, e.getPoint());
                double units = 1.0 + (e.getUnitsToScroll() / 100.0);
                double s = (units > 0 ? 1.0 / units : units);
                double scale = s * viewer.trans.getScaleX();
                if (scale >= MainViewer.XMIN_SCALE && scale <= MainViewer.XMAX_SCALE) {
                    viewer.trans.composeScale(s, 1);
                    spa.adjust(true, false);
                }
            }
            viewer.trans.getMagnifier().setActive(useMag);
        }
    }
}

// EOF
