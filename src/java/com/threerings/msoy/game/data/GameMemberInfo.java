//
// $Id$

package com.threerings.msoy.game.data;

import com.whirled.data.WhirledOccupantInfo;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Extends MemberInfo with game-specific information.
 */
public class GameMemberInfo extends MemberInfo
    implements WhirledOccupantInfo
{
    /** Suitable for unserialization. */
    public GameMemberInfo ()
    {
    }

    /** Creates an info record for the supplied user. */
    public GameMemberInfo (MemberObject user)
    {
        super(user);

        _headShot = user.getHeadShotMedia();
        _humanity = user.getHumanity();
    }

    // from interface WhirledOccupantInfo
    public String getHeadshotURL ()
    {
        return _headShot.getMediaPath();
    }

    // from interface WhirledOccupantInfo
    public int getAdjustedFlowPerMinute (int flowPerMinute)
    {
        // scale available flow linearly with humanity
        return Math.round(flowPerMinute * _humanity);
    }

    /** The media of the user's headshot (part of their avatar). */
    protected MediaDesc _headShot;

    /** This member's humanity rating from 0 to 1. */
    protected float _humanity;
}
