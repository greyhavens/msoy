//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;

import com.threerings.util.FlashObjectMarshaller;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

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
    public function PropertySetEvent (
            targetOid :int = 0, propertyName :String = null, data :Object = null)
    {
        super(targetOid, propertyName);

        // to help prevent the kids from doing bad things, we serialize
        // the property immediately.
        if (propertyName != null) {
            _data = FlashObjectMarshaller.encode(data);
        }
    }

    /**
     * Get the value that was set for the property.
     */
    public function getValue () :Object
    {
        return _data;
    }

    override public function applyToObject (target :DObject) :Boolean
    {
        FlashGameObject(target).applyPropertySet(_name, _data);
        return true;
    }

    override protected function notifyListener (listener :Object) :void
    {
        if (listener is PropertySetListener) {
            (listener as PropertySetListener).propertyWasSet(this);
        }
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        // _data is a ByteArray here, because we already serialized it
        // in the constructor
        out.writeField(_data);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        var bytes :ByteArray = (ins.readField(ByteArray) as ByteArray);
        _data = FlashObjectMarshaller.decode(bytes);
    }

    /** The flash-side data that is assigned to this property. */
    protected var _data :Object;
}
}
