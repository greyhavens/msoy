//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Present the contents of a Swiftly project as a tree model.
 */
public class ProjectTreeModel extends DefaultTreeModel
    implements SetListener
{
    public ProjectTreeModel (ProjectRoomObject roomObj)
    {
        super(null);

        _roomObj = roomObj;
        _roomObj.addListener(this);

        // construct our tree model based on the room object's contents
        PathElement root = roomObj.getRootElement();
        if (root == null) {
            return; // ack! log a warning
        }

        PathElementTreeNode node = new PathElementTreeNode(root);
        setRoot(node);
        addChildren(node, root);
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.ELEMENTS)) {
            final PathElement element = (PathElement)event.getEntry();
            updateNodes(new NodeOp() {
                public boolean isMatch (PathElementTreeNode node) {
                    return node.getElement().elementId == element.getParentId();
                }
                public void update (PathElementTreeNode node) {
                    insertNodeInto(new PathElementTreeNode(element), node, node.getChildCount());
                }
            });
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.ELEMENTS)) {
            final PathElement element = (PathElement)event.getEntry();
            updateNodes(new NodeOp() {
                public boolean isMatch (PathElementTreeNode node) {
                    return node.getElement().elementId == element.elementId;
                }
                public void update (PathElementTreeNode node) {
                    node.setUserObject(element);
                    nodeChanged(node);
                }
            });
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.ELEMENTS)) {
            final int elementId = (Integer)event.getKey();
            updateNodes(new NodeOp() {
                public boolean isMatch (PathElementTreeNode node) {
                    return node.getElement().elementId == elementId;
                }
                public void update (PathElementTreeNode node) {
                    node.removeFromParent();
                }
            });
        }
    }

    @Override // from DefaultTreeModel
    public void valueForPathChanged (TreePath path, Object newValue)
    {
        PathElementTreeNode node = (PathElementTreeNode)path.getLastPathComponent();
        PathElement element = (PathElement)node.getUserObject();
        element.setName((String)newValue);
        super.valueForPathChanged(path, element);
    }

    protected void addChildren (PathElementTreeNode node, PathElement parent)
    {
        for (PathElement element : _roomObj.elements) {
            if (element.getParentId() == parent.elementId) {
                PathElementTreeNode child = new PathElementTreeNode(element);
                node.add(child);
                if (element.getType() == PathElement.Type.DIRECTORY) {
                    addChildren(child, element);
                }
            }
        }
    }

    protected void updateNodes (NodeOp op)
    {
        Enumeration iter = ((DefaultMutableTreeNode)root).breadthFirstEnumeration();
        while (iter.hasMoreElements()) {
            PathElementTreeNode node = (PathElementTreeNode)iter.nextElement();
            if (op.isMatch(node)) {
                op.update(node);
            }
        }
    }

    protected interface NodeOp
    {
        public boolean isMatch (PathElementTreeNode node);
        public void update (PathElementTreeNode node);
    }

    protected ProjectRoomObject _roomObj;
}
