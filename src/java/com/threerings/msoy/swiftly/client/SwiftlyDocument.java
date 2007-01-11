package com.threerings.msoy.swiftly.client;

import java.io.Reader;
import java.io.StringReader;

public class SwiftlyDocument extends FileElement
{
    public SwiftlyDocument (String filename, String text, FileElement parent)
    {
        super(filename, parent);
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

    protected String _text;

    // TODO probably need a HashCode here?
}
