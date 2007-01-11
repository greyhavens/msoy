package com.threerings.msoy.swiftly.client;

public class FileElement
{
    public static final int PROJECT = 0;
    public static final int DOCUMENT = 1;
    public static final int DIRECTORY = 2;

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

    public int getType ()
    {
        return _type;
    }

    public String toString ()
    {
        return getName();
    }

    protected String _name;
    protected FileElement _parent;
    protected int _type;
}
