//
// $Id$

package com.threerings.msoy.world.data;

import com.google.common.collect.Comparators;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;

/**
 * A reference to an AVRG for control purposes.
 */
public class ControllableAVRGame extends Controllable
{
    public ControllableAVRGame (int gameId)
    {
        _gameId = gameId;
    }
    
    public int getGameId ()
    {
        return _gameId;
    }
    
    public int compareTo (Controllable other)
    {
        if (other instanceof ControllableEntity) {
            return -1;
        }
        if (other instanceof ControllableAVRGame) {
            return Comparators.compare(_gameId, ((ControllableAVRGame) other).getGameId());
        }
        throw new IllegalArgumentException("Unknown Controllable subclass: " + other.getClass());
    }

    @Override
    public boolean isControllableBy (int bodyOid)
    {
        MemberObject member = (MemberObject) MsoyServer.omgr.getObject(bodyOid);
        return member != null && member.game != null && member.game.gameId == _gameId;
    }

    protected int _gameId;

}