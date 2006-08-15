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
    public function PropertySetEvent (
        targetOid :int = 0, propName :String = null, value :Object = null,
        index :int = -1)
    {
        super(targetOid, propName);

        // to help prevent the kids from doing bad things, we serialize
        // the value immediately.
        if (propName != null) {
            _index = index;

            if (index < 0 && (value is Array)) {
                var array :Array = (value as Array);
                var encoded :Array = new Array();
                for (var ii :int = 0; ii < array.length; ii++) {
                    encoded[ii] = FlashObjectMarshaller.encode(array[ii]);
                }
                _data = encoded;

            } else {
                _data = FlashObjectMarshaller.encode(value);
            }
        }
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

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        if (_index >= 0) {
            out.writeByte(SET_ELEMENT);
            out.writeInt(_index);
            out.writeField(_data);

        } else if (_data is Array) {
            out.writeByte(SET_ARRAY);
            var arr :Array = (_data as Array);
            out.writeInt(arr.length);
            for (var ii :int = 0; ii < arr.length; ii++) {
                out.writeField(arr[ii]);
            }

        } else {
            out.writeByte(SET_NORMAL);
            out.writeField(_data);
        }
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        var type :int = ins.readByte();
        _index = (type == SET_ELEMENT) ? ins.readInt() : -1;

        if (type == SET_ARRAY) {
            var arr :Array = new Array();
            arr.length = ins.readInt();
            for (var ii :int = 0; ii < arr.length; ii++) {
                arr[ii] = FlashObjectMarshaller.decode(
                    ins.readField(ByteArray) as ByteArray);
            }
            _data = arr;

        } else {
            _data = FlashObjectMarshaller.decode(
                ins.readField(ByteArray) as ByteArray);
        }
    }

    protected static const SET_NORMAL :int = 0;
    protected static const SET_ARRAY :int = 1;
    protected static const SET_ELEMENT :int = 2;

    /** The flash-side data that is assigned to this property. */
    protected var _data :Object;

    /** The index of the property, if applicable. */
    protected var _index :int;

    protected var _oldValue :Object;
}
}
