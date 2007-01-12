package com.threerings.msoy.swiftly.client;

import javax.swing.tree.DefaultMutableTreeNode;

public class FileElementTreeNode extends DefaultMutableTreeNode
{
    public FileElementTreeNode (Object userObject)
    {
        super(userObject);
    }

    // TODO override setUserObject. If it is a string coming in set that to the name instead
    // of overwriting the object. Do nothing if its not of FileElement class?

    @Override // from DefaultMutableTreeNode
    public boolean isLeaf ()
    {
        return (((FileElement)getUserObject()).getType() == FileElement.DOCUMENT);
    }

    @Override // from DefaultMutableTreeNode
    public boolean getAllowsChildren ()
    {
        return !isLeaf();
    } 
}
