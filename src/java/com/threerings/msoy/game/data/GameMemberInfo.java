//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;

/**
 * Extends MemberInfo with game-specific information.
 */
public class GameMemberInfo extends MemberInfo
{
    /** The media of the user's headshot (part of their avatar). */
    public MediaDesc headShot;

    /** Suitable for unserialization. */
    public GameMemberInfo ()
    {
    }

    public GameMemberInfo (MemberObject user)
    {
        super(user);

        if (user.avatar != null) {
            headShot = user.avatar.getHeadShotMedia();

        } else {
            headShot = DEFAULT_HEADSHOT;
        }
    }

    /** The default headshot to use for guests and unconfigured members. */
    protected static MediaDesc DEFAULT_HEADSHOT =
        new StaticMediaDesc(StaticMediaDesc.HEADSHOT, Item.AVATAR);
}
