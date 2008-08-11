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
     * Identifier for the unique owber of the properties. This is currently expected to just be 
     * the avrg game id, but may be extended to include other scopes, hence the 64 bits.
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
