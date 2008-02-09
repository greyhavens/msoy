//
// $Id$

package com.threerings.msoy.chat.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.util.Hashable;
import com.threerings.util.Name;
import com.threerings.util.StringUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.RoomName;

/**
 * Defines a particular chat channel.
 */
public class ChatChannel extends SimpleStreamableObject
    implements Hashable
{
    /** A chat channel between two players. Implemented using tells. */
    public static const MEMBER_CHANNEL :int = 1;

    /** A chat channel open to all members of a group. */
    public static const GROUP_CHANNEL :int = 2;

    /** A chat channel created by a player into whom they invite other players. */
    public static const PRIVATE_CHANNEL :int = 3;

    /** A chat channel carrying room chat.  GROUP_CHANNEL is a more specific subtype of 
     * ROOM_CHANNEL */
    public static const ROOM_CHANNEL :int = 4;

    /** The type of this chat channel. */
    public var type :int;

    /** The name that identifies this channel (either a {@link MemberName}, {@link GroupName} or
     * {@link ChannelName}. */
    public var ident :Name;

    /**
     * Creates a channel identifier for a channel communicating with the specified member.
     */
    public static function makeMemberChannel (member :MemberName) :ChatChannel
    {
        return new ChatChannel(MEMBER_CHANNEL, member);
    }

    /**
     * Creates a channel identifier for the specified group's channel.
     */
    public static function makeGroupChannel (group :GroupName) :ChatChannel
    {
        return new ChatChannel(GROUP_CHANNEL, group);
    }

    /**
     * Creates a channel identifier for the specified named private channel.
     */
    public static function makePrivateChannel (channel :ChannelName) :ChatChannel
    {
        return new ChatChannel(PRIVATE_CHANNEL, channel);
    }

    /** 
     * Creates a channel identifier for the specified room.
     */
    public static function makeRoomChannel (room :RoomName) :ChatChannel
    {
        return new ChatChannel(ROOM_CHANNEL, room);
    }

    /** 
     * Returns the static type of the given localType.
     */
    public static function typeOf (localType :String) :int
    {
        var type :int = -1;
        try {
            type = StringUtil.parseInteger(localType.charAt(0));
        } catch (err :ArgumentError) {
            // NOOP, return -1
        }

        return type;
    }

    public function ChatChannel (type :int = 0, ident :Name = null)
    {
        this.type = type;
        this.ident = ident;
    }

    /**
     * Returns a string we can use to register this channel with the ChatDirector.
     */
    public function toLocalType () :String
    {
        return type + ":" + ident;
    }

    // from Object
    public function hashCode () :int
    {
        return type ^ ident.hashCode();
    }

    // from Object
    public function equals (other :Object) :Boolean
    {
        var oc :ChatChannel = (other as ChatChannel);
        return (oc != null) && type == oc.type && ident.equals(oc.ident);
    }

    // from Object
    override public function toString () :String
    {
        return toLocalType();
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        type = ins.readInt();
        ident = (ins.readObject() as Name);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(type);
        out.writeObject(ident);
    }
}
}
