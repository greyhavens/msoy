//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Provides access to information shared by {@link WorldActorInfo} and {@link WorldMemberInfo}.
 */
public interface WorldOccupantInfo
{
    /**
     * Returns the item that was used to create this occupant.
     */
    public ItemIdent getItemIdent ();

    /**
     * Returns the media that is used to display this occupant.
     */
    public MediaDesc getMedia ();

    /**
     * Return the current state of the occupant, which may be null.
     */
    public String getState ();
}
