//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;
import flash.utils.IExternalizable;

import com.threerings.util.FlashObjectMarshaller;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamer;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.NamedEvent;

/**
 * Represents a property change on the actionscript object we
 * use in FlashGameObject.
 */
public class PropertySetEvent extends NamedEvent
{
    /**
     * Create a PropertySetEvent.
     */
    public function PropertySetEvent () // unserialize-only
    {
        super(0, null);
    }

    /**
     * Get the value that was set for the property.
     */
    public function getValue () :Object
    {
        return _data;
    }

    /**
     * Get the old value.
     */
    public function getOldValue () :Object
    {
        return _oldValue;
    }

    /**
     * Get the index, or -1 if not applicable.
     */
    public function getIndex () :int
    {
        return _index;
    }

    override public function applyToObject (target :DObject) :Boolean
    {
        _oldValue =
            FlashGameObject(target).applyPropertySet(_name, _data, _index);
        return true;
    }

    override protected function notifyListener (listener :Object) :void
    {
        if (listener is PropertySetListener) {
            (listener as PropertySetListener).propertyWasSet(this);
        }
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        _index = ins.readInt();
        _data = FlashObjectMarshaller.decode(ins.readObject());
    }

    /** The index of the property, if applicable. */
    protected var _index :int;

    /** The flash-side data that is assigned to this property. */
    protected var _data :Object;

    protected var _oldValue :Object;
}
}
