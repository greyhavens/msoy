package com.threerings.msoy.room.data;

import com.threerings.presents.dobj.DSet;

/**
 * Binds together an owner id, typically the id of an avr game and the id of the properties object
 * associated with the owner's agent and clients.
 */
public class RoomPropertiesEntry
    implements DSet.Entry
{
    /**
     * Identifier for the unique owber of the properties. This is currently expected to just be 
     * the avrg game id, but may be extended to include other scopes, hence the 64 bits.
     */
    public int ownerId;

    /**
     * Object id of the {@link RoomPropertiesObject} if a client is interested in subscribing to 
     * the properties.
     */
    public int propsOid;
    
    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return ownerId;
    }
}
