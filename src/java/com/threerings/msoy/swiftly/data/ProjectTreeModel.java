//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.util.Enumeration;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.threerings.msoy.swiftly.client.controller.PathElementEditor;
import com.threerings.msoy.swiftly.client.event.PathElementListener;

/**
 * Present the contents of a Swiftly project as a tree model.
 */
public class ProjectTreeModel extends DefaultTreeModel
    implements PathElementListener
{
    public ProjectTreeModel (PathElementEditor editor, PathElement root,
                             Set<PathElement> pathElements)
    {
        super(null);
        _editor = editor;

        // construct our tree model based on the room object's contents
        PathElementTreeNode node = new PathElementTreeNode(root);
        setRoot(node);
        fillTree(node, root, pathElements);
    }

    // from interface PathElementListener
    public void elementAdded (final PathElement element)
    {
        updateNodes(new NodeOp() {
            public boolean isMatch (PathElementTreeNode node) {
                return node.getElement().elementId == element.getParent().elementId;
            }
            public void update (PathElementTreeNode node) {
                insertNodeInto(new PathElementTreeNode(element), node, node.getChildCount());
            }
        });
    }

    // from interface PathElementListener
    public void elementUpdated (final PathElement element)
    {
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

    // from interface PathElementListener
    public void elementRemoved (final PathElement element)
    {
        updateNodes(new NodeOp() {
            public boolean isMatch (PathElementTreeNode node) {
                return node.getElement().elementId == element.elementId;
            }
            public void update (PathElementTreeNode node) {
                removeNodeFromParent(node);
            }
        });
    }

    @Override // from DefaultTreeModel
    public void valueForPathChanged (TreePath path, Object newValue)
    {
        PathElementTreeNode node = (PathElementTreeNode)path.getLastPathComponent();
        PathElement element = (PathElement)node.getUserObject();
        String newName = (String)newValue;
        _editor.renamePathElement(element, newName);
    }

    @Deprecated // TODO: this might not need to stick around
    public void updateNodeName (PathElement element, TreePath path)
    {
        super.valueForPathChanged(path, element);
    }

    /**
     * Recursively fill the tree with a set of PathElements.
     */
    private void fillTree (PathElementTreeNode node, PathElement parent,
                           Set<PathElement> pathElements)
    {
        for (PathElement element : pathElements) {
            if (element.getParent() != null && element.getParent() == parent) {
                PathElementTreeNode child = new PathElementTreeNode(element);
                node.add(child);
                if (element.getType() == PathElement.Type.DIRECTORY) {
                    fillTree(child, element, pathElements);
                }
            }
        }
    }

    private void updateNodes (NodeOp op)
    {
        Enumeration iter = ((DefaultMutableTreeNode)root).breadthFirstEnumeration();
        while (iter.hasMoreElements()) {
            PathElementTreeNode node = (PathElementTreeNode)iter.nextElement();
            if (op.isMatch(node)) {
                op.update(node);
            }
        }
    }

    private interface NodeOp
    {
        public boolean isMatch (PathElementTreeNode node);
        public void update (PathElementTreeNode node);
    }

    private final PathElementEditor _editor;
}
