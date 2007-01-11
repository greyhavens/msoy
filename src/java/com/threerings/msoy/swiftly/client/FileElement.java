package com.threerings.msoy.swiftly.client;

public class FileElement
{
    public FileElement (FileElement parent)
    {
        setParent(parent);
    }

    public FileElement getParent ()
    {
        return _parent;
    }

    public void setParent (FileElement parent)
    {
        _parent = parent;
    }

    protected FileElement _parent;
}
