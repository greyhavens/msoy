//
// $Id$

package com.threerings.msoy.web.data;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Represents data for a {@link Group} in a neighborhood query result. Only 'group' is
 * required by the visualization engine.
 */
public class NeighborGroup extends NeighborEntity
{
    /** The group's id/name. */
    public GroupName group;

    /** The number of members in this group. */
    public int members;

    /** The media description of this group's logo. */
    public MediaDesc logo;
    
    /** Constructor for unserializing. */
    public NeighborGroup ()
    {
        super();
    }
}
