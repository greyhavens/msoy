package com.threerings.msoy.swiftly.client;

import java.io.Reader;
import java.io.StringReader;

public class SwiftlyDocument
{
    public SwiftlyDocument (String filename, String text)
    {
        _filename = filename;
        _text = text;
    }

    public Reader getReader ()
    {
        return (Reader)new StringReader(getText());

    }

    public String getText ()
    {
        return _text;
    }

    public String getFilename ()
    {
        return _filename;
    }

    public String toString ()
    {
        return _filename;
    }

    protected String _text = new String();
    protected String _filename = new String();

    // TODO probably need a HashCode here?
}
