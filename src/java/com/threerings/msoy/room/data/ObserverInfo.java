//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;

/**
 * An occupant info for use by "Whirledwide" guest observers.
 */
public class ObserverInfo extends OccupantInfo
{
    /** Creates an info record for the specified body. */
    public ObserverInfo (BodyObject bobj)
    {
        super(bobj);
    }

    /** Used for unserialization. */
    public ObserverInfo ()
    {
    }
}
