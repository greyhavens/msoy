//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains a summary of the party information, to be published in room objects
 * and ultimately exposed to games.
 */
public class PartySummary extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The party id. */
    public int id;

    /** The current name of the party. */
    public String name;

    /** The name of the group (and id). */
    public GroupName group;

    /** The party's icon (the group icon). */
    public MediaDesc icon;

    /** Suitable for unserialization. */
    public PartySummary ()
    {
    }

    /** Create a PartySummary. */
    public PartySummary (int id, String name, GroupName group, MediaDesc icon)
    {
        this.id = id;
        this.name = name;
        this.group = group;
        this.icon = icon;
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return id;
    }
}
