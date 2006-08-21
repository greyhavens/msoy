//
// $Id$

package com.threerings.msoy.game.data;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;
import com.threerings.io.Streamer;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.NamedEvent;

/**
 * Represents a property change on the actionscript object we
 * use in FlashGameObject.
 */
public class PropertySetEvent extends NamedEvent
{
    /** Suitable for unserialization. */
    public PropertySetEvent ()
    {
    }

    /**
     * Create a PropertySetEvent.
     */
    public PropertySetEvent (
        int targetOid, String propName, Object value, int index)
    {
        super(targetOid, propName);
        _data = value;
        _index = index;
    }

    // from abstract DEvent
    public boolean applyToObject (DObject target)
    {
        ((FlashGameObject) target).applyPropertySet(_name, _data, _index);
        return true;
    }

    /** The index. */
    protected int _index;

    /** The flash-side data that is assigned to this property. */
    protected Object _data;
}
