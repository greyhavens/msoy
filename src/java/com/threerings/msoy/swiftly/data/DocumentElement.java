//
// $Id$

package com.threerings.msoy.swiftly.data;

/**
 * Represents a source file in a project and contains the text of the document.
 */
public class DocumentElement extends PathElement
{
    public DocumentElement ()
    {
    }

    public DocumentElement (String name, PathElement parent, String text)
    {
        super(Type.FILE, name, parent);
        _text = text;
    }

    public String getText ()
    {
        return _text;
    }

    public void setText (String text)
    {
        _text = text;
    }

    protected String _text;
}
