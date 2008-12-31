package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DSet_Entry;

/**
 * Binds together an owner id, typically the id of an avr game and the id of the properties object
 * associated with the owner's agent and clients.
 */
public class RoomPropertiesEntry
    implements DSet_Entry
{
    /**
     * Unique identifier for the owner of the properties. This is currently expected to just be
     * the avrg game id.
     */
    public var ownerId :int;

    /**
     * Object id of the <code>RoomPropertiesObject</code> if a client is interested in subscribing to
     * the properties.
     * @see RoomPropertiesObject
     */
    public var propsOid :int;

    /** @inheritDoc */
    // from DSet_Entry
    public function getKey () :Object
    {
        return ownerId;
    }

    public function readObject (ins :ObjectInputStream) :void
    {
        ownerId = ins.readInt();
        propsOid = ins.readInt();
    }

    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(ownerId);
        out.writeInt(propsOid);
    }
}
}
