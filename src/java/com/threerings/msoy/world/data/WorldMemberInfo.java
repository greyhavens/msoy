//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Extends MemberInfo with world-specific information.
 */
public class WorldMemberInfo extends MemberInfo
{
//    /** The style of chat bubble to use. */
//    public short chatStyle;
//
//    /** The style with which the chat bubble pops up. */
//    public short chatPopStyle;

    /** Suitable for unserialization. */
    public WorldMemberInfo ()
    {
    }

    /**
     * Configures an occcupant info for the supplied member.
     */
    public WorldMemberInfo (MemberObject user)
    {
        super(user);

//        chatStyle = user.chatStyle;
//        chatPopStyle = user.chatPopStyle;

        if (user.avatar != null) {
            _media = user.avatar.avatarMedia;
        } else if (user.isGuest()) {
            _media = Avatar.getDefaultGuestAvatarMedia();
        } else {
            _media = Avatar.getDefaultMemberAvatarMedia();
        }
    }

    // from interface WorldOccupantInfo
    public MediaDesc getMedia ()
    {
        return _media;
    }

    protected MediaDesc _media;
}
