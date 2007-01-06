//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.util.Comparable;
import com.threerings.util.Equalable;
import com.threerings.util.Hashable;
import com.threerings.util.Integer;

/**
 * Identifies a particular entity in a scene. This includes avatars, furniture, pets and anything
 * else we might dream up in the future that wishes to generate trigger events or maintain a
 * persistent memory.
 */
public class EntityIdent extends SimpleStreamableObject
    implements Comparable, Hashable
{
    /** Identifies an avatar entity. */
    public static const AVATAR :int = 0;

    /** Identifies a furniture entity. */
    public static const FURNITURE :int = 1;

    /** Identifies a pet entity. */
    public static const PET :int = 2;

    /** The type of this entity: {@link #AVATAR}, {@link #FURNITURE}, etc. */
    public var type :int;

    /** A type-specific identifier. */
    public var entityId :int;

    public function EntityIdent ()
    {
    }

    // from interface Hashable
    public function hashCode () :int
    {
        return (type << 16) ^ entityId;
    }

    // from interface Comparable<EntityIdent>
    public function compareTo (other :Object) :int
    {
        var oident :EntityIdent = (other as EntityIdent);
        var rv :int = (type - oident.type);
        return (rv != 0) ? rv : Integer.compare(entityId, oident.entityId);
    }

    // from interface Equalable
    public function equals (other :Object) :Boolean
    {
        if (other is EntityIdent) {
            var oid :EntityIdent = (other as EntityIdent);
            return type == oid.type && entityId == oid.entityId;
        } else {
            return false;
        }
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        type = ins.readByte();
        entityId = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeByte(type);
        out.writeInt(entityId);
    }
}
}
