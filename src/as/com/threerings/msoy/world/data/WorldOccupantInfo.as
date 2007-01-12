//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Defines an interface shared between WorldActorInfo and WorldMemberInfo.
 */
public interface WorldOccupantInfo
{
    /**
     * Returns the item that was used to create this occupant.
     */
    function getItemIdent () :ItemIdent;

    /**
     * Returns the media that is used to display this occupant.
     */
    function getMedia () :MediaDesc;
}
}
