//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.RandomUtil;

import com.threerings.crowd.data.OccupantInfo;

/**
 * Extends basic OccupantInfo with information specific to msoy.
 */
public class MsoyOccupantInfo extends OccupantInfo
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
    public MsoyOccupantInfo ()
    {
    }

    public MsoyOccupantInfo (MsoyUserObject user)
    {
        super(user);

        memberId = user.memberId;
        media = user.avatar;
        chatStyle = user.chatStyle;
        chatPopStyle = user.chatPopStyle;
    }
}
