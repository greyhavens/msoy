//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Represents data for a {@link Group} in a neighborhood query result.
 */
public class NeighborGroup 
    implements IsSerializable, Streamable, Cloneable
{
    /** The ID of the group. */
    public int groupId;

    /** The name of the group. */
    public String groupName;

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
