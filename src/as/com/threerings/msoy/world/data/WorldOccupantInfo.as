//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

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

    /**
     * Return the scale that should be used for the media.
     */
    function getScale () :Number;

    /**
     * Return the current state of the occupant, which may be null.
     */
    function getState () :String;
}
}
