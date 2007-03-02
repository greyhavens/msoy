//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.IOException;

import com.threerings.io.ObjectOutputStream;
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

    /**
     * After serialization, call lazarus with the DSet of associated pathElements
     * to correctly re-bind the any transient instance variables. This
     * relies on PathElement nodes being added to the DSet prior to their associated
     * SwiftlyDocuments, and an assert() below makes sure of that.
     */
    public void lazarus (DSet<PathElement> pathElements) {
        if (_pathKey != null) {
            _path = pathElements.get(_pathKey);
            assert(_path != null);
        }
    }

    /**
     * Store the PathElement's key prior to serialization, such that we can use it to re-bind
     * the transient PathElement instance variable when lazarus() is called on the other side
     * of the wire, post-serialization.
     */
    public void writeObject(ObjectOutputStream out)
        throws IOException
    {
        if (_path != null) {
            _pathKey = _path.getKey();            
        }
        out.defaultWriteObject();
    }

    /** Document contents, ineffeciently stored entirely in memory. */
    protected String _text;

    /** Reference to our associated path element. */
    protected transient PathElement _path = null;

    /** Key for the associated PathElement, used to re-bind the transient _path instance variable
     * post-serialization. */
    private Comparable _pathKey = null;
}
