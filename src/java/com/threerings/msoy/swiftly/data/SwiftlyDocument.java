//
// $Id$

package com.threerings.msoy.swiftly.data;

import com.threerings.presents.dobj.DSet;

/**
 * Represents a source file in a project and contains the text of the file.
 */
public class SwiftlyDocument
    implements DSet.Entry
{
    
    /** Uniquely identifies this document element in the distributed state. */
    public int elementId;

    public SwiftlyDocument ()
    {
    }

    public String getText ()
    {
        return _text;
    }

    public void setText (String text)
    {
        _text = text;
    }

    public Comparable getKey ()
    {
        return elementId;
    }
    
    protected String _text;
}
