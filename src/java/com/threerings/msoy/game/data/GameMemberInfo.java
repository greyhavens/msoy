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
    }

    // from interface WhirledOccupantInfo
    public String getHeadshotURL ()
    {
        return _headShot.getMediaPath();
    }

    /** The media of the user's headshot (part of their avatar). */
    protected MediaDesc _headShot;
}
