//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.ezgame.server.EZGameManager;
import com.threerings.ezgame.server.GameCookieManager;

import com.threerings.msoy.game.data.MsoyGameObject;

/**
 * Manages a MetaSOY game.
 */
public class MsoyGameManager extends EZGameManager
{
    public MsoyGameManager ()
    {
        super();
        addDelegate(_whirledDelegate = new WhirledGameDelegate(this));
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new MsoyGameObject();
    }

    @Override
    protected GameCookieManager createCookieManager ()
    {
        return new GameCookieManager(MsoyGameServer.gameCookieRepo);
    }

    protected WhirledGameDelegate _whirledDelegate;
}
