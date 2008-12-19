//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.crowd.data.OccupantInfo;

/**
 * An OccupantInfo for players.
 */
public class PlayerInfo extends OccupantInfo
{
    public PlayerInfo (PlayerObject player)
    {
        super(player);
        _partyId = player.partyId;
    }

    /**
     * Get this player's partyId.
     */
    public int getPartyId ()
    {
        return _partyId;
    }

    protected int _partyId;
}
