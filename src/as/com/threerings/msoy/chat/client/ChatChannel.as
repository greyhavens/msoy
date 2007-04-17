//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.Equalable;
import com.threerings.util.Hashable;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

/**
 * Identifies a particular chat channel.
 */
public class ChatChannel
    implements Equalable, Hashable
{
    public static const FRIEND_CHANNEL :int = 0;
    public static const GROUP_CHANNEL :int = 1;
    public static const PRIVATE_CHANNEL :int = 2;

    public var type :int;
    public var ident :Object;

    public static function makeFriendChannel (friend :Name) :ChatChannel
    {
        return new ChatChannel(FRIEND_CHANNEL, friend);
    }

    // TODO: makeGroupChannel()
    // TODO: makePrivateChannel()

    public function getName () :String
    {
        switch (type) {
        case FRIEND_CHANNEL: return MessageBundle.tcompose("m.friend_channel", ident);
        case GROUP_CHANNEL: return "todo";
        case PRIVATE_CHANNEL: return "todo";
        }
        return MessageBundle.taint("");
    }

    // from interface Equalable
    public function equals (other :Object) :Boolean
    {
        var ochannel :ChatChannel = (other as ChatChannel);
        if (ochannel.type != type) {
            return false;
        }
        switch (type) {
        case FRIEND_CHANNEL:
            return (ident as Name).equals(ochannel.ident as Name);
        case GROUP_CHANNEL:
            return false; // TODO
        case PRIVATE_CHANNEL:
            return false; // TODO
        }
        return false;
    }

    // from interface Hashable
    public function hashCode () :int
    {
        var code :int = type;
        switch (type) {
        case FRIEND_CHANNEL:
            code ^= (ident as Name).hashCode();
            break;
        case GROUP_CHANNEL:
            // TODO
            break;
        case PRIVATE_CHANNEL:
            // TODO
            break;
        }
        return code;
    }

    /** Don't call this use the factory methods. We'd make it protected but we can't. */
    public function ChatChannel (type :int, ident :Object) :void
    {
        this.type = type;
        this.ident = ident;
    }
}
}
