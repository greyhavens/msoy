package com.threerings.msoy.swiftly.client;

import java.util.ArrayList;

public class SwiftlyProject extends FileElement
{
    public SwiftlyProject (String name, ArrayList<SwiftlyDocument> files)
    {
        // projects are roots and have no parent
        super(name, null);
        _files = files;
        _type = FileElement.PROJECT;
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
