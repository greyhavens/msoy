//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.msoy.data.all.PeerEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.VizMemberName;

import com.threerings.msoy.item.data.all.MediaDesc;

public class PartymateEntry
    implements PeerEntry
{
    public VizMemberName name;

    public MemberName getName ()
    {
        return name;
    }

    public int getMemberId ()
    {
        return name.getMemberId();
    }

    public MediaDesc getPhoto ()
    {
        return name.getPhoto();
    }

    public Comparable getKey ()
    {
        return getMemberId();
    }

    public int hashCode ()
    {
        return getMemberId();
    }

    public boolean equals (Object other)
    {
        return (other instanceof PartymateEntry) &&
            (getMemberId() == ((PartymateEntry)other).getMemberId());
    }

    public int compareTo (Object other)
    {
        PartymateEntry that = (PartymateEntry)other;
        return MemberName.BY_DISPLAY_NAME.compare(this.name, that.name);
    }

}
