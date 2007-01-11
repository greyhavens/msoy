package com.threerings.msoy.swiftly.client;

import java.io.Reader;
import java.io.StringReader;

public class SwiftlyDocument extends FileElement
{
    public SwiftlyDocument (String filename, String text, FileElement parent)
    {
        super(parent);
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

    public void setFilename (String filename)
    {
        _filename = filename;
    }

    public String toString ()
    {
        return _filename;
    }

    protected String _text;
    protected String _filename;

    // TODO probably need a HashCode here?
}
