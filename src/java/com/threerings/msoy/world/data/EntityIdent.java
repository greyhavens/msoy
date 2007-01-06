//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.Comparators;

import com.threerings.io.SimpleStreamableObject;

/**
 * Identifies a particular entity in a scene. This includes avatars, furniture, pets and anything
 * else we might dream up in the future that wishes to generate trigger events or maintain a
 * persistent memory.
 */
public class EntityIdent extends SimpleStreamableObject
    implements Comparable<EntityIdent>
{
    /** Identifies an avatar entity. */
    public static final byte AVATAR = 0;

    /** Identifies a furniture entity. */
    public static final byte FURNITURE = 1;

    /** Identifies a pet entity. */
    public static final byte PET = 2;

    /** The type of this entity: {@link #AVATAR}, {@link #FURNITURE}, etc. */
    public byte type;

    /** A type-specific identifier. */
    public int entityId;

    // from interface Comparable<EntityIdent>
    public int compareTo (EntityIdent other)
    {
        return (type == other.type) ?
            Comparators.compare(entityId, other.entityId) : (type - other.type);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof EntityIdent) {
            EntityIdent oid = (EntityIdent)other;
            return type == oid.type && entityId == oid.entityId;
        } else {
            return false;
        }
    }

    @Override // from Object
    public int hashCode ()
    {
        return (type << 16) ^ entityId;
    }
}
