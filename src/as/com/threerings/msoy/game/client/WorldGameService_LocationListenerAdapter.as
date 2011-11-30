//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.langBoolean;

import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * A functional adapter for the WorldGameService_LocationListener interface.
 */
public class WorldGameService_LocationListenerAdapter
    implements WorldGameService_LocationListener
{
    /**
     * Creates a new WorldGame service Location listener that will delegate to the
     * given function(s). Any Function that is null will simply not be called.
     */
    public function WorldGameService_LocationListenerAdapter (
        gameLocated :Function, failed :Function)
    {
        _gameLocated = gameLocated;
        _failed = failed;
    }

    // from Java WorldGameService_LocationListener
    public function gameLocated (arg1 :String, arg2 :int, arg3 :Boolean) :void
    {
        if (_gameLocated != null) {
            _gameLocated(arg1, arg2, arg3);
        }
    }

    // from InvocationService_InvocationListener
    public function requestFailed (cause :String) :void
    {
        if (_failed != null) {
            _failed(cause);
        }
    }

    protected var _gameLocated :Function;
    protected var _failed :Function;
}
}
