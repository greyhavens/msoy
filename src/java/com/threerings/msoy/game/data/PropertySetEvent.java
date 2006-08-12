//
// $Id$

package com.threerings.msoy.game.data;

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
    public PropertySetEvent (int targetOid, String propertyName, byte[] data)
    {
        super(targetOid, propertyName);

        _data = data;
    }

    // from abstract DEvent
    public boolean applyToObject (DObject target)
    {
        ((FlashGameObject) target).applyPropertySet(_name, _data);
        return true;
    }

    /** The flash-side data that is assigned to this property. */
    protected byte[] _data;
}
