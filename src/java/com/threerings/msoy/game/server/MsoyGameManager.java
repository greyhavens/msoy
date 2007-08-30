//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.StringUtil;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.ezgame.server.EZGameManager;
import com.threerings.ezgame.server.GameCookieManager;

import com.threerings.msoy.game.data.MsoyGameConfig;
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

    @Override // from PlaceManager
    public String where ()
    {
        if (_config == null || _plobj == null) {
            return super.where();
        }
        MsoyGameConfig cfg = (MsoyGameConfig)_config;
        return "[" + cfg.name + ":" + cfg.getGameId() + ":" + _gameobj.getOid() +
            "(" + StringUtil.toString(_gameobj.players) + ")";
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
