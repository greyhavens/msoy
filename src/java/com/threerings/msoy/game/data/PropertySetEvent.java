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

    // from abstract DEvent
    public boolean applyToObject (DObject target)
    {
        ((FlashGameObject) target).applyPropertySet(_name, _data, _index);
        return true;
    }

    /**
     * Implements customized streaming.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        //Streamer streamer = Streamer.getStreamer(BYTE_ARRAY_CLASS);

        if (_index >= 0) {
            out.writeByte(SET_ELEMENT);
            out.writeInt(_index);
            out.writeBareObject(_data);
            //streamer.writeObject(_data, out, false);

        } else if (_data instanceof byte[][]) {
            out.writeByte(SET_ARRAY);
            byte[][] data = (byte[][]) _data;
            out.writeInt(data.length);
            for (int ii = 0; ii < data.length; ii++) {
                out.writeBareObject(data[ii]);
                //streamer.writeObject(data[ii], out, false);
            }

        } else {
            out.writeByte(SET_NORMAL);
            out.writeBareObject(_data);
            //streamer.writeObject(_data, out, false);
        }
    }

    /**
     * Implements customized streaming.
     */
    public void readObject (ObjectInputStream ins)
        throws IOException, ClassNotFoundException
    {
        ins.defaultReadObject();

        byte type = ins.readByte();
        _index = (type == SET_ELEMENT) ? ins.readInt() : -1;

        Streamer streamer = Streamer.getStreamer(BYTE_ARRAY_CLASS);

        if (type == SET_ARRAY) {
            int length = ins.readInt();
            byte[][] data = new byte[length][];
            for (int ii=0; ii < length; ii++) {
                data[ii] = (byte[]) streamer.createObject(ins);
                streamer.readObject(data[ii], ins, false);
            }
            _data = data;

        } else {
            byte[] data = (byte[]) streamer.createObject(ins);
            streamer.readObject(data, ins, false);
            _data = data;
        }
    }

    protected static final byte SET_NORMAL = 0;
    protected static final byte SET_ARRAY = 1;
    protected static final byte SET_ELEMENT = 2;

    protected static final Class BYTE_ARRAY_CLASS = (new byte[0]).getClass();

    /** The flash-side data that is assigned to this property. */
    protected transient Object _data;

    /** The index. */
    protected transient int _index;
}
