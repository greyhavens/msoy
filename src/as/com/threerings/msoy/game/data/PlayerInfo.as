//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.crowd.data.OccupantInfo;

import com.whirled.data.WhirledOccupantInfo;

import com.threerings.msoy.data.VizMemberName;

/**
 * Contains information on occupants in a game.
 */
public class PlayerInfo extends OccupantInfo
    implements WhirledOccupantInfo
{
    public function PlayerInfo (who :PlayerObject = null)
    {
        // only used for unserialization
    }

    // from interface WhirledOccupantInfo
    public function getHeadshotURL () :String
    {
        return (username as VizMemberName).getPhoto().getMediaPath();
    }
}
}
