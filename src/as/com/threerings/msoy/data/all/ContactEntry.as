//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.util.Comparable;
import com.threerings.util.Hashable;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.all.JabberName;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Represents a jabber contact.
 */
public class ContactEntry
    implements Comparable, DSet_Entry, Hashable
{
    /** The jabber name of the contact. */
    public var name :JabberName;

    /** Are they online? */
    public var online :Boolean;

    public function ContactEntry (name :JabberName = null, online :Boolean = false)
    {
        this.name = name;
        this.online = online;
    }

    /**
     * Returns the gateway this contact is from.
     */
    public function getGateway () :String
    {
        var gateway :String = name.toJID();
        gateway = gateway.substring(gateway.indexOf("@") + 1);
        return gateway.substring(0, gateway.indexOf("."));
    }

    // from Hashable
    public function hashCode () :int
    {
        return name.hashCode();
    }

    // from Comparable
    public function compareTo (other :Object) :int
    {
        var that :ContactEntry = (other as ContactEntry);
        // online folks show up above offline folks
        if (this.online != that.online) {
            return this.online ? -1 : 1;
        }
        // then, sort by name
        return this.name.compareTo(that.name);
    }

    // from Hashable
    public function equals (other :Object) :Boolean
    {
        return (other is ContactEntry) && name.equals((other as ContactEntry).name);
    }

    //
    public function toString () :String
    {
        return "ContactEntry[" + name + "]";
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return name;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        name = JabberName(ins.readObject());
        online = ins.readBoolean();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(name);
        out.writeBoolean(online);
    }
}
}
