//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.crowd.data.OccupantInfo;

/**
 * Represents an AVRG MOB.
 */
public class MobInfo extends OccupantInfo
{
    /**
     * Creates an info record for the supplied MOB.
     */
    public MobInfo (MobObject mobj, String ident)
    {
        super(mobj);
        _ident = ident;
    }

    /** Used for unserialization. */
    public MobInfo ()
    {
    }

    /**
     * Returns the string identifier for this MOB. This is provided by the AVRG and interpreted by
     * the AVRG, we don't parse it.
     */
    public String getIdent ()
    {
        return _ident;
    }

    protected String _ident;
}
