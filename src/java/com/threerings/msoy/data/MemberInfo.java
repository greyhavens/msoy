//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.RandomUtil;

import com.threerings.crowd.data.OccupantInfo;

/**
 * Extends basic OccupantInfo with member-specific information.
 */
public class MemberInfo extends OccupantInfo
{
    /** The media that represents our avatar. */
    public MediaData media;

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

        media = user.avatar;
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
