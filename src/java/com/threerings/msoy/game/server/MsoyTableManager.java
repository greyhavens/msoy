//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.parlor.data.Table;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.parlor.server.TableManager;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyTable;

import com.threerings.msoy.item.web.Game;

public class MsoyTableManager extends TableManager
{
    public MsoyTableManager (LobbyObject lobj)
    {
        super(lobj);

        _lobj = lobj;
        _tableClass = MsoyTable.class;
    }

    @Override 
    protected GameConfig createConfig (Table table) 
    {
        MsoyGameConfig config = (MsoyGameConfig)super.createConfig(table);
        // fill in our game id and name
        Game game = _lobj.game;
        config.persistentGameId = game.getPrototypeId();
        config.name = game.name;
        return config;
    }

    protected LobbyObject _lobj;
}
