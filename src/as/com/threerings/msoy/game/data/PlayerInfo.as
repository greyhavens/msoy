/
// $Id$

package com.threerings.msoy.game.data {

import com.whirled.data.WhirledOccupantInfo;
import com.whirled.game.data.WhirledGameOccupantInfo;

import com.threerings.msoy.data.VizMemberName;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains information on occupants in a game.
 */
public class PlayerInfo extends WhirledGameOccupantInfo
    implements WhirledOccupantInfo
{
    public function PlayerInfo (who :PlayerObject = null)
    {
        // only used for unserialization
    }

    /**
     * Use this method in preference to getHeadshotURL, as MediaDescs should
     * be passed to endpoints, not urls.
     */
    public function getHeadshot () :MediaDesc
    {
        return (username as VizMemberName).getPhoto();
    }

    // from WhirledOccupantInfo
    public function getHeadshotURL () :String
    {
        return getHeadshot().getMediaPath();
    }
}
}
