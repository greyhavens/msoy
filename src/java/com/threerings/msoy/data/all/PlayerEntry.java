//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

public class PlayerEntry
    implements IsSerializable, DSet.Entry
{
    /** The display name of the friend. */
    public VizMemberName name;

    /** This player's current status. */
    public String status;

    /** Suitable for deserialization. */
    public PlayerEntry ()
    {
    }

    public PlayerEntry (VizMemberName name, String status)
    {
        this.name = name;
        this.status = status;
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return this.name.getKey();
    }

    @Override // from Object
    public int hashCode ()
    {
        return this.name.hashCode();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof PlayerEntry) &&
            (this.name.getMemberId() == ((PlayerEntry)other).name.getMemberId());
    }

    @Override
    public String toString ()
    {
        return "PlayerEntry[" + name + "]";
    }
}
