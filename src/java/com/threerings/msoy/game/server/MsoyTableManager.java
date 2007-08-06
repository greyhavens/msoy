//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.data.Table;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.parlor.server.TableManager;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyTable;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.server.MsoyServer;

public class MsoyTableManager extends TableManager
{
    public MsoyTableManager (LobbyObject lobj)
    {
        super(lobj);

        _lobj = lobj;
        _tableClass = MsoyTable.class;
    }

    /**
     * Should be called whenever the game is updated, so that we create a new GameSummary.
     */
    public void gameUpdated ()
    {
    }

    @Override 
    protected GameConfig createConfig (Table table) 
    {
        MsoyGameConfig config = (MsoyGameConfig)super.createConfig(table);
        config.init(_lobj.game, _lobj.gameDef);
        return config;
    }

    @Override 
    protected void notePlayerAdded (Table table, BodyObject body) 
    {
        super.notePlayerAdded(table, body);

        // mark this player as "in" this game
        PlayerObject plobj = (PlayerObject) body;
        MsoyGameServer.worldClient.updatePlayer(plobj.getMemberId(), _lobj.game);
    }

    @Override 
    protected Table notePlayerRemoved (int playerOid, BodyObject body)
    {
        // mark this player as no longer "in" this game (TODO: if they are playing the game, keep
        // them in it?)
        if (body != null) {
            PlayerObject plobj = (PlayerObject) body;
            MsoyGameServer.worldClient.updatePlayer(plobj.getMemberId(), null);
        }

        return super.notePlayerRemoved(playerOid, body);
    }

    protected LobbyObject _lobj;
}
