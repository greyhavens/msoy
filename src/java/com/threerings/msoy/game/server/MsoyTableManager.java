//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.ArrayIntSet;

import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.data.GameObject;
import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.server.TableManager;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyTable;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.data.all.MemberName;

import static com.threerings.msoy.Log.log;

public class MsoyTableManager extends TableManager
{
    public MsoyTableManager (LobbyManager lmgr)
    {
        super(lmgr.getLobbyObject());

        _lmgr = lmgr;
        _lobj = lmgr.getLobbyObject();
        _tableClass = MsoyTable.class;
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
        // mark this player as no longer "in" this game, unless this method is being called because
        // the game itself started.
        PlayerObject plobj = (PlayerObject) body;
        if (plobj != null && !_membersPlaying.contains(plobj.getMemberId())) {
            MsoyGameServer.worldClient.updatePlayer(plobj.getMemberId(), null);
        }

        return super.notePlayerRemoved(playerOid, body);
    }

    @Override
    protected GameManager createGameManager (GameConfig config)
        throws InstantiationException, InvocationException
    {
        return _lmgr.createGameManager(config);
    }

    @Override
    protected void gameCreated (Table table, GameObject gameobj, GameManager gmgr)
    {
        for (int ii = 0; table.occupants != null && ii < table.occupants.length; ii++) {
            if (table.occupants[ii] == null) {
                continue;
            }
            MemberName member = (MemberName) table.occupants[ii];
            _membersPlaying.add(member.getMemberId());
        }

        super.gameCreated(table, gameobj, gmgr);
        gameobj.addListener(_playerUpdater);
    }

    @Override
    protected void purgeTable (Table table)
    {
        // check for occupants in local map - this is the last time we'll hear about this game, so
        // make sure the players really are cleared out.
        for (int ii = 0; table.occupants != null && ii < table.occupants.length; ii++) {
            if (table.occupants[ii] == null) {
                continue;
            }

            MemberName member = (MemberName) table.occupants[ii];
            if (_membersPlaying.remove(member.getMemberId())) {
                MsoyGameServer.worldClient.updatePlayer(member.getMemberId(), null);
            }
        }

        super.purgeTable(table);
    }

    protected OidListListener _playerUpdater = new OidListListener() {
        public void objectAdded (ObjectAddedEvent event) {
            PlayerObject plobj = (PlayerObject) MsoyGameServer.omgr.getObject(event.getOid());
            int memberId = plobj.getMemberId();
            if (!_membersPlaying.contains(memberId)) {
                MsoyGameServer.worldClient.updatePlayer(memberId, _lobj.game);
            }
            _membersPlaying.add(memberId);
        }

        public void objectRemoved (ObjectRemovedEvent event) {
            PlayerObject plobj = (PlayerObject) MsoyGameServer.omgr.getObject(event.getOid());
            int memberId = plobj.getMemberId();
            MsoyGameServer.worldClient.updatePlayer(memberId, null);
            _membersPlaying.remove(memberId);
        }
    };

    protected LobbyManager _lmgr;
    protected LobbyObject _lobj;
    protected ArrayIntSet _membersPlaying = new ArrayIntSet();
}
