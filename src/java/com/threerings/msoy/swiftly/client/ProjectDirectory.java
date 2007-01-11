package com.threerings.msoy.swiftly.client;

public class ProjectDirectory extends FileElement
{
    public ProjectDirectory (String name, FileElement parent)
    {
        super(parent);
        setName(name);
    }

    public String getName ()
    {
        return _name;
    }

    public void setName (String name)
    {
        _name = name;
    }

    public String toString ()
    {
        return _name;
    }

    protected String _name;
}
