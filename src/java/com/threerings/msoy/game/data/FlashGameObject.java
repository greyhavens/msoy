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
    protected void applyPropertySet (String property, byte[] data)
    {
        _gameData.put(property, data);
    }

    /**
     * A custom serialization method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        // write the number of properties, followed by each one
        out.writeInt(_gameData.size());
        for (Map.Entry<String, byte[]> entry : _gameData.entrySet()) {
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

        _gameData.clear();
        int count = ins.readInt();
        while (count-- > 0) {
            String key = ins.readUTF();
            byte[] data = (byte[]) ins.readObject();
            _gameData.put(key, data);
        }
    }

    /** The current state of game data, opaque to us here on the server. */
    protected transient HashMap<String, byte[]> _gameData =
        new HashMap<String, byte[]>();
}
