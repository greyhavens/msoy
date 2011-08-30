//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.util.Comparable;
import com.threerings.util.Hashable;
import com.threerings.util.StringUtil;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Information pertaining to a player's IM connections.
 */
public class GatewayEntry
    implements Comparable, DSet_Entry, Hashable
{
    /** The gateway name. */
    public var gateway :String;

    /** Is the user logged into the gateway? */
    public var online :Boolean;

    /** The username being used to conenct to the gateway. */
    public var username :String;

    public function GatewayEntry (
        gateway :String = null, online :Boolean = false, username :String = null)
    {
        this.gateway = gateway;
        this.online = online;
        this.username = username;
    }

    // from Hashable
    public function hashCode () :int
    {
        return StringUtil.hashCode(gateway);
    }

    // from Comparable
    public function compareTo (other :Object) :int
    {
        var that :GatewayEntry = (other as GatewayEntry);
        // online folks show up above offline folks
        if (this.online != that.online) {
            return this.online ? -1 : 1;
        }
        // then, sort by name
        return this.gateway.localeCompare(that.gateway);
    }

    // from Hashable
    public function equals (other :Object) :Boolean
    {
        return (other is GatewayEntry) && gateway == (other as GatewayEntry).gateway;
    }

    //
    public function toString () :String
    {
        return "GatewayEntry[" + gateway + "]";
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return gateway;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        gateway = (ins.readField(String) as String);
        online = ins.readBoolean();
        username = (ins.readField(String) as String);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(gateway);
        out.writeBoolean(online);
        out.writeObject(username);
    }
}
}
