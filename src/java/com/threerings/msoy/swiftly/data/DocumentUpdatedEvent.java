//
// $Id$

package com.threerings.msoy.swiftly.data;

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;

import static com.threerings.msoy.Log.log;

/**
 * An event dispatched when the user updates a document.
 */
public class DocumentUpdatedEvent extends DEvent
{
    public DocumentUpdatedEvent (int targetOid, int editorOid, int elementId, String text)
    {
        super(targetOid);
        _editorOid = editorOid;
        _elementId = elementId;
        _text = text;
    }

    /**
     * Returns the oid of the client that initiated this edit.
     */
    public int getEditorOid ()
    {
        return _editorOid;
    }

    // from DEvent
    public boolean applyToObject (DObject target)
        throws ObjectAccessException
    {
        // we only operate on a known type of object
        ProjectRoomObject roomObj = (ProjectRoomObject)target;
        SwiftlyDocument document = roomObj.documents.get(_elementId);
        if (document == null) {
            log.warning("Requested to update unknown element! [elementId=" + _elementId + "].");
        } else {
            // TODO: diffs!
            document.setText(_text);
        }
        return true;
    }

    // documentation inherited
    protected void notifyListener (Object listener)
    {
        if (listener instanceof DocumentUpdateListener) {
            ((DocumentUpdateListener)listener).documentUpdated(this);
        }
    }

    protected int _elementId;
    protected int _editorOid;
    protected String _text;
}
