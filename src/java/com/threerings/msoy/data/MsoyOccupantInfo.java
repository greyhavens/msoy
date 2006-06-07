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
    public MediaData media;

    /** Suitable for unserialization. */
    public MsoyOccupantInfo ()
    {
    }

    public MsoyOccupantInfo (MsoyUserObject user)
    {
        super(user);

        // TODO
        media = new MediaData(user.getOid() % 3); // consistent
    }
}
