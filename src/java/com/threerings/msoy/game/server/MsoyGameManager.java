//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;

import com.samskivert.util.StringUtil;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.server.EZGameManager;
import com.threerings.ezgame.server.GameCookieManager;

import com.whirled.data.GameData;
import com.whirled.data.ItemData;
import com.whirled.data.LevelData;
import com.whirled.data.TrophyData;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.TrophySource;

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
    public void setGameData (Game game, ArrayList<LevelPack> lpacks, ArrayList<ItemPack> ipacks,
                             ArrayList<TrophySource> tsources)
    {
        MsoyGameObject gobj = (MsoyGameObject)_plobj;

        // populate our game data information
        ArrayList<GameData> gdata = new ArrayList<GameData>();
        for (LevelPack pack : lpacks) {
            LevelData data = new LevelData();
            data.ident = pack.ident;
            data.name = pack.name;
            data.mediaURL = pack.getFurniMedia().getMediaPath();
            data.premium = pack.premium;
            gdata.add(data);
        }
        for (ItemPack pack : ipacks) {
            ItemData data = new ItemData();
            data.ident = pack.ident;
            data.name = pack.name;
            data.mediaURL = pack.getFurniMedia().getMediaPath();
            gdata.add(data);
        }
        for (TrophySource source : tsources) {
            TrophyData data = new TrophyData();
            data.ident = source.ident;
            data.name = source.name;
            data.mediaURL = source.getThumbnailMedia().getMediaPath();
            gdata.add(data);
        }
        gobj.setGameData(gdata.toArray(new GameData[gdata.size()]));

        // keep the trophy source information around for later
        _tsources = tsources;
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
    protected ArrayList<TrophySource> _tsources;
}
