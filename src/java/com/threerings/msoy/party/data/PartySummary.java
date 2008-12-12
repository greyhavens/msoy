//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;

public class PartySummary extends SimpleStreamableObject
{
    /** The party id. */
    public int id;

    /** The current name of the party. */
    public String name;

    /** The name of the group (and id). */
    public GroupName group;

    /** The icon for this party, planned to be the group's icon. */
    public MediaDesc icon;
}
