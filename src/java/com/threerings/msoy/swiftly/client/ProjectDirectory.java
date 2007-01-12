package com.threerings.msoy.swiftly.client;

public class ProjectDirectory extends FileElement
{
    public ProjectDirectory (String name, FileElement parent)
    {
        super(name, parent);
        _type = FileElement.DIRECTORY;
    }

    @Override // from FileElement
    // XXX TEMP until I wire up a different icon for directories
    public String toString()
    {
        return "(" + getName() + ")";
    }
}
