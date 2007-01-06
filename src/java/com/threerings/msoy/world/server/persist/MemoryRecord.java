//
// $Id$

package com.threerings.msoy.world.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;

import com.threerings.msoy.world.data.EntityIdent;

/**
 * Maintains memory information for "smart" digital items (furniture, pets, etc).
 */
@Entity
public class MemoryRecord
{
    /** Used when using {@link #type} in a query. */
    public static final String TYPE = "type";

    /** Used when using {@link #entityId} in a query. */
    public static final String ENTITY_ID = "entityId";

    /** The type of entity for which we're storing memory (see {@link EntityIdent}). */
    @Id public byte type;

    /** The entity-specific unique identifier for our entity. */
    @Id public int entityId;

    /** The key that identifies this memory datum. */
    @Id public String key;

    /** A serialized representation of this datum's value. */
    public byte[] value;

    /**
     * Creates a key for the supplied entity identifier.
     */
    public static Key makeKey (EntityIdent ident)
    {
        return new Key(TYPE, ident.type, ENTITY_ID, ident.entityId);
    }

    /** Used when loading instances from the repository. */
    public MemoryRecord ()
    {
    }

    /**
     * Creates a memory record from the supplied memory information.
     */
    public MemoryRecord (EntityIdent ident, String key, byte[] value)
    {
        this.type = ident.type;
        this.entityId = ident.entityId;
        this.key = key;
        this.value = value;
    }
}
