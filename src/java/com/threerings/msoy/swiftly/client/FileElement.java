package com.threerings.msoy.swiftly.client;

public class FileElement
{
    public FileElement (String name, FileElement parent)
    {
        setName(name);
        setParent(parent);
    }

    public String getName ()
    {
        return _name;

    }

    public void setName (String name)
    {
        _name = name;
    }

    public FileElement getParent ()
    {
        return _parent;
    }

    public void setParent (FileElement parent)
    {
        _parent = parent;
    }

    public String toString ()
    {
        return getName();
    }

    protected String _name;
    protected FileElement _parent;
}
