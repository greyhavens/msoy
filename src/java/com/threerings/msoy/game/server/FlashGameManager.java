//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.util.Name;

import com.threerings.presents.dobj.AccessController;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.parlor.turn.server.TurnGameManager;
import com.threerings.parlor.turn.server.TurnGameManagerDelegate;

import com.threerings.msoy.server.MsoyObjectAccess;

import com.threerings.msoy.game.data.FlashGameObject;

/**
 * A manager for "flash" games in msoy.
 */
public class FlashGameManager extends GameManager
    implements TurnGameManager
{
    public FlashGameManager ()
    {
        addDelegate(_turnDelegate = new TurnGameManagerDelegate(this));
    }

    // from TurnGameManager
    public void turnWillStart ()
    {
    }

    // from TurnGameManager
    public void turnDidStart ()
    {
    }

    // from TurnGameManager
    public void turnDidEnd ()
    {
    }

    @Override
    protected Class<? extends PlaceObject> getPlaceObjectClass ()
    {
        return FlashGameObject.class;
    }

    @Override
    protected AccessController getAccessController ()
    {
        return MsoyObjectAccess.GAME;
    }

    /** Our turn delegate. */
    protected TurnGameManagerDelegate _turnDelegate;
}
