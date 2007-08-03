//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.data.Table;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.parlor.server.TableManager;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyTable;
import com.threerings.msoy.game.data.GameSummary;

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
//         _summary = null; // clear old ref, so we will lazy-create new ones
    }

    @Override 
    protected GameConfig createConfig (Table table) 
    {
        MsoyGameConfig config = (MsoyGameConfig)super.createConfig(table);
        config.init(_lobj.game, _lobj.gameDef);
        return config;
    }

// TODO: if we end up keeping this, forward these on to our world server

//     @Override 
//     protected void notePlayerAdded (Table table, BodyObject body) 
//     {
//         super.notePlayerAdded(table, body);

//         if (_summary == null) {
//             _summary = new GameSummary(_lobj.game);
//         }

//         // attach the GameSummary to our MemberObject, and update our occupant info 
//         MemberObject member = (MemberObject) body;
//         member.setPendingGame(_summary);
//         MsoyServer.memberMan.updateOccupantInfo(member);
//     }

//     @Override 
//     protected Table notePlayerRemoved (int playerOid, BodyObject body)
//     {
//         // remove the GameSummary from our MemberObject, and update our occupant info
//         if (body != null) {
//             MemberObject member = (MemberObject) body;
//             member.setPendingGame(null);
//             MsoyServer.memberMan.updateOccupantInfo(member);
//         }

//         return super.notePlayerRemoved(playerOid, body);
//     }

    protected LobbyObject _lobj;

//     /** The single GameSummary used for all members at pending tables. Lazy-created. */
//     protected GameSummary _summary = null;
}
