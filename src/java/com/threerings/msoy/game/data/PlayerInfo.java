//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.util.ActionScript;

import com.whirled.data.WhirledOccupantInfo;

import com.threerings.msoy.data.VizMemberName;

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
    }

    /**
     * Use this method in preference to getHeadshotURL, as MediaDescs should
     * be passed to endpoints, not urls.
     */
    public MediaDesc getHeadshot ()
    {
        return ((VizMemberName)username).getPhoto();
    }

    // from interface WhirledOccupantInfo
    public String getHeadshotURL ()
    {
        return getHeadshot().getMediaPath();
    }
}
