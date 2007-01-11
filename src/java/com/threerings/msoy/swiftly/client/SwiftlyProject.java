package com.threerings.msoy.swiftly.client;

import java.util.ArrayList;

public class SwiftlyProject extends FileElement
{
    public SwiftlyProject (String name, ArrayList<SwiftlyDocument> files, FileElement parent)
    {
        super(name, parent);
        _files = files;
    }

    public ArrayList<SwiftlyDocument> getFiles ()
    {
        return _files;

    }

    public void setFiles (ArrayList<SwiftlyDocument> files)
    {
        _files = files;
    }

    protected ArrayList<SwiftlyDocument> _files;
}
