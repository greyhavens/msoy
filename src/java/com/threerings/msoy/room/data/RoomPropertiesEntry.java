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
     * Unique identifier for the owner of the properties. This is currently expected to just be
     * the avrg game id.
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
