//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.util.ActionScript;

import com.whirled.data.WhirledOccupantInfo;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains information on occupants in a game.
 */
public class PlayerInfo extends OccupantInfo
    implements WhirledOccupantInfo
{
    /** Used when unserializing. */
    public PlayerInfo ()
    {
    }

    /**
     * Creates an info record for the specified player.
     */
    @ActionScript(omit=true)
    public PlayerInfo (PlayerObject body)
    {
        super(body);
        _headShot = body.getHeadShotMedia();
    }

    // from interface WhirledOccupantInfo
    public String getHeadshotURL ()
    {
        return _headShot.getMediaPath();
    }

    /** The media of the user's headshot (part of their avatar). */
    protected MediaDesc _headShot;
}
