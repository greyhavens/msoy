//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.RandomUtil;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Extends basic OccupantInfo with member-specific information.
 */
public class MemberInfo extends OccupantInfo
{
    /** The media that represents our avatar. */
    public MediaDesc media;

    /** The itemId of the item that is the avatar. */
    public int avatarId;

    /** The style of chat bubble to use. */
    public short chatStyle;

    /** The style with which the chat bubble pops up. */
    public short chatPopStyle;

    /** Suitable for unserialization. */
    public MemberInfo ()
    {
    }

    public MemberInfo (MemberObject user)
    {
        super(user);

        if (user.avatar != null) {
            media = user.avatar.avatarMedia;
            avatarId = user.avatar.itemId;

        } else {
            // TODO
            media =
                new MediaDesc("e1c4cd0111619e0d8c038b90292aa6ec0bd5d86a.swf");
            avatarId = -1;
        }

        chatStyle = user.chatStyle;
        chatPopStyle = user.chatPopStyle;
    }

    /**
     * Get the member id for this user, or -1 if they're a guest.
     */
    public int getMemberId ()
    {
        return ((MemberName) username).getMemberId();
    }
}
