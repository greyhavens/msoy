//
// $Id$

package com.threerings.msoy.game.data;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.parlor.game.data.GameObject;

/**
 * Contains the data for a flash game.
 */
public class FlashGameObject extends GameObject
{
    /**
     * Called by PropertySetEvent to effect the property update.
     */
    protected void applyPropertySet (String propName, Object data, int index)
    {
        if (index != -1) {
            byte[][] arr = (byte[][]) _props.get(propName);
            // the array should never be null...
            if (arr.length <= index) {
                byte[][] newArr = new byte[index + 1][];
                System.arraycopy(arr, 0, newArr, 0, arr.length);
                _props.put(propName, newArr);
                arr = newArr;
            }
            arr[index] = (byte[]) data;

        } else {
            _props.put(propName, data);
            return;
        }
    }

    /**
     * A custom serialization method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        // write the number of properties, followed by each one
        out.writeInt(_props.size());
        for (Map.Entry<String, Object> entry : _props.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeObject(entry.getValue());
        }
    }

    /**
     * A custom serialization method.
     */
    public void readObject (ObjectInputStream ins)
        throws IOException, ClassNotFoundException
    {
        ins.defaultReadObject();

        _props.clear();
        int count = ins.readInt();
        while (count-- > 0) {
            String key = ins.readUTF();
            _props.put(key, ins.readObject());
        }
    }

    /** The current state of game data, opaque to us here on the server. */
    protected transient HashMap<String, Object> _props =
        new HashMap<String, Object>();
}
