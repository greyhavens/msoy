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
    /** The memberId of this occupant, or 0 if they're not a member. */
    public int memberId;

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

        memberId = user.memberId;
        media = user.avatar;
        chatStyle = user.chatStyle;
        chatPopStyle = user.chatPopStyle;
    }
}
