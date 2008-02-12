//
// $Id$

package com.threerings.msoy.world.data;

import com.google.common.collect.Comparators;

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

    protected int _gameId;
}