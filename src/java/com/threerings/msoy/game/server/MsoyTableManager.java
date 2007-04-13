//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.parlor.data.Table;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.parlor.server.TableManager;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyTable;
import com.threerings.msoy.game.data.GameSummary;

import com.threerings.msoy.item.web.Game;

import com.threerings.msoy.server.MsoyServer;

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

    @Override 
    protected void notePlayerAdded (Table table, int playerOid) 
    {
        super.notePlayerAdded(table, playerOid);

        // attach a new GameSummary to our MemberObject, and update our occupant info 
        GameSummary sum = new GameSummary();
        sum.name = _lobj.game.name;
        sum.gameId = _lobj.game.itemId;
        sum.thumbMedia = _lobj.game.thumbMedia;
        MemberObject member = (MemberObject)MsoyServer.omgr.getObject(playerOid);
        member.currentGame = sum;
        MsoyServer.memberMan.updateOccupantInfo(member);
    }

    @Override 
    protected boolean notePlayerRemoved (Table table, int playerOid)
    {
        // remove the GameSummary from our MemberObject, and update our occupant info
        MemberObject member = (MemberObject)MsoyServer.omgr.getObject(playerOid);
        member.currentGame = null;
        MsoyServer.memberMan.updateOccupantInfo(member);
        
        return super.notePlayerRemoved(table, playerOid);
    }

    protected LobbyObject _lobj;
}
