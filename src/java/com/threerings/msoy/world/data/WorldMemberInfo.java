//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Extends MemberInfo with world-specific information.
 */
public class WorldMemberInfo extends MemberInfo
{
    /** The media that represents our avatar. */
    public MediaDesc avatar;

    /** The style of chat bubble to use. */
    public short chatStyle;

    /** The style with which the chat bubble pops up. */
    public short chatPopStyle;

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

        if (user.avatar != null) {
            avatar = user.avatar.avatarMedia;
        } else if (user.isGuest()) {
            avatar = Avatar.getDefaultGuestAvatarMedia();
        } else {
            avatar = Avatar.getDefaultMemberAvatarMedia();
        }

        chatStyle = user.chatStyle;
        chatPopStyle = user.chatPopStyle;
    }
}
