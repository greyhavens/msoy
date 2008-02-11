//
// $Id$

package com.threerings.msoy.world.data;

/**
 * A reference to an AVRG for control purposes.
 */
public class ControllableAVRGame extends Controllable
{
    public ControllableAVRGame (int gameId)
    {
        _gameId = gameId;
    }
    
    public Comparable getKey ()
    {
        return _gameId;
    }
    
    protected int _gameId;
}