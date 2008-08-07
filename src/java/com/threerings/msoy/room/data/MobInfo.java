//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.crowd.data.OccupantInfo;

/**
 * Represents an AVRG MOB, a room occupant that is not backed up by an entity but rather acquires
 * its visualization from an AVRG in the client.
 */
public class MobInfo extends OccupantInfo
{
    /**
     * Creates an info record for the supplied MOB.
     */
    public MobInfo (MobObject mobj, int gameId, String ident)
    {
        super(mobj);
        _gameId = gameId;
        _ident = ident;
    }

    /** Used for unserialization. */
    public MobInfo ()
    {
    }

    /**
     * Returns the gameId of the AVRG that spawned this MOB and is responsible for it on the
     * client side.
     */
    public int getGameId ()
    {
        return _gameId;
    }

    /**
     * Returns the string identifier for this MOB. This is provided by the AVRG and interpreted by
     * the AVRG, we don't parse it.
     */
    public String getIdent ()
    {
        return _ident;
    }

    protected int _gameId;

    protected String _ident;
}
