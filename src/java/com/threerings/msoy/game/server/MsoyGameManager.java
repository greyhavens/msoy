//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;

import com.samskivert.util.StringUtil;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.server.EZGameManager;
import com.threerings.ezgame.server.GameCookieManager;

import com.whirled.data.ItemInfo;
import com.whirled.data.LevelInfo;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;

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

    /**
     * Called by the lobby manager once we're started to inform us of our level and item packs.
     */
    public void setGameData (Game game, ArrayList<LevelPack> lpacks, ArrayList<ItemPack> ipacks)
        // TODO: have lobby manager pass in info on per-player packs or have us do it?
    {
        MsoyGameObject gobj = (MsoyGameObject)_plobj;

        LevelInfo[] linfo = new LevelInfo[lpacks.size()];
        for (int ii = 0; ii < lpacks.size(); ii++) {
            linfo[ii] = toLevelInfo(lpacks.get(ii));
        }
        gobj.setLevelPacks(linfo);

        ItemInfo[] iinfo = new ItemInfo[ipacks.size()];
        for (int ii = 0; ii < ipacks.size(); ii++) {
            iinfo[ii] = toItemInfo(ipacks.get(ii));
        }
        gobj.setItemPacks(iinfo);
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

    protected LevelInfo toLevelInfo (LevelPack pack)
    {
        LevelInfo info = new LevelInfo();
        info.ident = pack.ident;
        info.name = pack.name;
        info.mediaURL = pack.getFurniMedia().getMediaPath();
        info.premium = pack.premium;
        return info;
    }

    protected ItemInfo toItemInfo (ItemPack pack)
    {
        ItemInfo info = new ItemInfo();
        info.ident = pack.ident;
        info.name = pack.name;
        info.mediaURL = pack.getFurniMedia().getMediaPath();
        return info;
    }

    protected WhirledGameDelegate _whirledDelegate;
}
