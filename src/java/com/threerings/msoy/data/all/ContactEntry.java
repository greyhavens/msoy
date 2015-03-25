//
// $Id$

package com.threerings.msoy.data.all;

import com.google.common.collect.ComparisonChain;
import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

/**
 * Stores contact information for a player's IM buddies.
 */
public class ContactEntry
    implements Comparable<ContactEntry>, DSet.Entry, IsSerializable
{
    /** The jabber name of the contact. */
    public JabberName name;

    /** Are they online? */
    public boolean online;

    /** For serialization. */
    public ContactEntry ()
    {
    }

    public ContactEntry (JabberName name, boolean online)
    {
        this.name = name;
        this.online = online;
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return name;
    }

    // from interface Comparable
    public int compareTo (ContactEntry that)
    {
        // online folks show up above offline folks, then sort by name
        return ComparisonChain.start().compareFalseFirst(online, that.online)
            .compare(name, that.name).result();
    }

    @Override // from Object
    public int hashCode ()
    {
        return name.hashCode();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof ContactEntry) && name.equals(((ContactEntry)other).name);
    }

    @Override
    public String toString ()
    {
        return "ContactEntry[" + name + "]";
    }
}
