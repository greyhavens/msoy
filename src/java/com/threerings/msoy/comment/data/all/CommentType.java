//
// $Id: $

package com.threerings.msoy.comment.data.all;

import com.samskivert.util.ByteEnumUtil;

import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.MsoyItemType;

import com.google.gwt.user.client.rpc.IsSerializable;

public class CommentType
    implements IsSerializable, Streamable
{
    public static CommentType forItemType (MsoyItemType type)
    {
        return new CommentType(type);
    }

    /** An entity type code indicating a comment on a room. */
    public static final CommentType ROOM = new CommentType(64);

    /** An entity type code indicating a comment on a member's profile. */
    public static final CommentType PROFILE_WALL = new CommentType(65);

    /** An entity type code indicating a comment on a game. */
    public static final CommentType GAME = new CommentType(66);

    public CommentType ()
    {
    }

    public boolean forRoom()
    {
        return ROOM.equals(this);
    }

    public boolean forProfileWall ()
    {
        return PROFILE_WALL.equals(this);
    }

    public boolean forGame ()
    {
        return GAME.equals(this);
    }

    public boolean isValid ()
    {
        return (_type >= 64 && _type <= 66) || isItemType();
    }

    public boolean isItemType ()
    {
        return (_type >= TYPE_ITEM_MIN && _type < TYPE_ITEM_MAX);
    }

    public byte toByte ()
    {
        return _type;
    }

    public MsoyItemType toItemType ()
    {
        return ByteEnumUtil.fromByte(MsoyItemType.class, _type);
    }

    @Override
    public boolean equals (Object o)
    {
        return (this == o) ||
            (o != null && getClass().equals(o.getClass()) && _type == ((CommentType) o)._type);
    }

    @Override
    public int hashCode ()
    {
        return (int) _type;
    }

    @Override
    public String toString ()
    {
        return "[type=" + _type + "]";
    }

    CommentType (MsoyItemType itemType)
    {
        _type = itemType.toByte();
    }

    CommentType (int type)
    {
        _type = (byte) type;
    }

    protected byte _type;

    /** The minimum entity type code reserved for items. */
    protected static final int TYPE_ITEM_MIN = 1;

    /** The maximum entity type code reserved for items. */
    protected static final int TYPE_ITEM_MAX = 63;
}
