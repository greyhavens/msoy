package com.threerings.msoy.swiftly.client;

import java.util.ArrayList;

public class SwiftlyProject
{
    public SwiftlyProject (String name, ArrayList<SwiftlyDocument> files)
    {
        _name = name;
        _files = files;
    }

    public String getName ()
    {
        return _name;

    }

    public void setName (String name)
    {
        _name = name;
    }

    public ArrayList<SwiftlyDocument> getFiles ()
    {
        return _files;

    }

    public void setFiles (ArrayList<SwiftlyDocument> files)
    {
        _files = files;
    }

    public String toString ()
    {
        return _name;
    }

    protected String _name;
    protected ArrayList<SwiftlyDocument> _files;
}
