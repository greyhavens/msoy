//
// $Id$

package com.threerings.msoy.chat.data;

import com.google.common.collect.Comparators;

import com.threerings.util.Name;

import com.threerings.crowd.chat.data.ChatChannel;

import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.JabberName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RoomName;

import static com.threerings.msoy.Log.log;

/**
 * Defines Whirled's chat channel types.
 */
public class MsoyChatChannel extends ChatChannel
{
    /** A chat channel between two players. Implemented using tells. */
    public static final int MEMBER_CHANNEL = 1;

    /** A chat channel open to all members of a group. */
    public static final int GROUP_CHANNEL = 2;

    /** A chat channel created by a player into whom they invite other players. */
    public static final int PRIVATE_CHANNEL = 3;

    /** A chat channel for room chat. */
    public static final int ROOM_CHANNEL = 4;

    /** A chat channel between a member and the jabber gateway. */
    public static final int JABBER_CHANNEL = 5;

    /** String translations for the various chat types.  Used for chat logging. */
    public static final String[] XLATE_TYPE = {
        "none", "member", "group", "private", "room", "jabber"
    };

    /** The type of this chat channel. */
    public int type;

    /** The name that identifies this channel (either a {@link MemberName}, {@link GroupName} or
     * {@link ChannelName}. */
    public Name ident;

    /**
     * Creates a channel identifier for a channel communicating with the specified player.
     */
    public static ChatChannel makeMemberChannel (MemberName member)
    {
        return new MsoyChatChannel(MEMBER_CHANNEL, member);
    }

    /**
     * Creates a channel identifier for the specified group's channel.
     */
    public static ChatChannel makeGroupChannel (GroupName group)
    {
        return new MsoyChatChannel(GROUP_CHANNEL, group);
    }

    /**
     * Creates a channel identifier for the specified named private channel.
     */
    public static ChatChannel makePrivateChannel (ChannelName channel)
    {
        return new MsoyChatChannel(PRIVATE_CHANNEL, channel);
    }

    /**
     * Creates a channel identifier for the specified named room channel.
     */
    public static ChatChannel makeRoomChannel (RoomName room)
    {
        return new MsoyChatChannel(ROOM_CHANNEL, room);
    }

    /**
     * Creates a channel identifier for the specified named jabber channel.
     */
    public static ChatChannel makeJabberChannel (JabberName channel)
    {
        return new MsoyChatChannel(JABBER_CHANNEL, channel);
    }

    /** Used for unserialization. */
    public MsoyChatChannel ()
    {
    }

    /**
     * Returns a string we can use to register this channel with the ChatDirector.
     */
    public String toLocalType ()
    {
        return type + ":" + getId(ident);
    }

    @Override // from ChatChannel
    public String getLockName ()
    {
        return toLocalType();
    }

    @Override // from ChatChannel
    public int compareTo (ChatChannel other)
    {
        return toString().compareTo(other.toString());
    }

    @Override // from Object
    public int hashCode ()
    {
        return type ^ ident.hashCode();
    }

    @Override // from Object
    public String toString ()
    {
        return toLocalType();
    }

    protected MsoyChatChannel (int type, Name ident)
    {
        this.type = type;
        this.ident = ident;
    }

    protected String getId (Name name)
    {
        if (name instanceof MemberName) {
            return "" + ((MemberName) name).getMemberId();

        } else if (name instanceof GroupName) {
            return "" + ((GroupName) name).getGroupId();

        } else if (name instanceof RoomName) {
            return "" + ((RoomName) name).getSceneId();

        } else if (name instanceof ChannelName) {
            ChannelName channelName = (ChannelName) name;
            return channelName.getCreatorId() + ":" + channelName;

        } else if (name instanceof JabberName) {
            return ((JabberName) name).toJID();

        } else {
            log.warning("ChatChannel unable to determine id! [" + name + "]");
            return null;
        }
    }
}
