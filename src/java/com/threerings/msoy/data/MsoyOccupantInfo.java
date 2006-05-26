//
// $Id$

package com.threerings.msoy.data;

import com.threerings.util.RandomUtil;

import com.threerings.crowd.data.OccupantInfo;

/**
 * Extends basic OccupantInfo with information specific to msoy.
 */
public class MsoyOccupantInfo extends OccupantInfo
{
    /** The media that represents our avatar. */
    public MediaData avatar;

    /** Suitable for unserialization. */
    public MsoyOccupantInfo ()
    {
    }

    public MsoyOccupantInfo (MsoyUserObject user)
    {
        super(user);

        // TODO
        avatar = new MediaData(0); //RandomUtil.getInt(2));
    }
}
