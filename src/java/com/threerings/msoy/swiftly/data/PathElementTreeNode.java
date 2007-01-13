//
// $Id$

package com.threerings.msoy.swiftly.data;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Represents a single path element in the tree model version of our project elements.
 */
public class PathElementTreeNode extends DefaultMutableTreeNode
{
    public PathElementTreeNode (PathElement element)
    {
        super(element);
    }

    public PathElement getElement ()
    {
        return (PathElement)getUserObject();
    }

    @Override // from DefaultMutableTreeNode
    public boolean isLeaf ()
    {
        return getElement().getType() == PathElement.Type.FILE;
    }

    @Override // from DefaultMutableTreeNode
    public boolean getAllowsChildren ()
    {
        return !isLeaf();
    } 
}
