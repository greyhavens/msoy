package com.threerings.msoy.swiftly.client;

public class ProjectDirectory extends FileElement
{
    public ProjectDirectory (String name, FileElement parent)
    {
        super(name, parent);
        _type = FileElement.DIRECTORY;
    }
}
