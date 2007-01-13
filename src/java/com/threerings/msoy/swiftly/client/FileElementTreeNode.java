package com.threerings.msoy.swiftly.client;

import javax.swing.tree.DefaultMutableTreeNode;

public class FileElementTreeNode extends DefaultMutableTreeNode
{
    public FileElementTreeNode (Object userObject)
    {
        super(userObject);
    }

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
