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
    /** The media that represents our avatar. */
    public MediaData media;

    /** The style of chat bubble to use. */
    public short bubbleStyle;

    /** The style with which the chat bubble pops up. */
    public short bubblePopStyle;

    /** Suitable for unserialization. */
    public MsoyOccupantInfo ()
    {
    }

    public MsoyOccupantInfo (MsoyUserObject user)
    {
        super(user);

        // TODO
        media = new MediaData(AVATARS[user.getOid() % AVATARS.length]);
        if (media.id == 0) {
            bubbleStyle = (short) 1;
        }
        bubblePopStyle = (short) (user.getOid() % 2);
    }

    // TEMP: media ids for our standard avatars
    public static final int[] AVATARS = { 0, 1, 2, 20, 21, 22, 25 };
}
