//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.ArrayIntSet;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.dobj.ObjectAddedEvent;
import com.threerings.presents.dobj.ObjectRemovedEvent;
import com.threerings.presents.dobj.OidListListener;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.data.GameObject;
import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.server.TableManager;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.data.all.MemberName;

/**
 * Customizes the basic table manager with MSOY specific bits.
 */
@EventThread
public class MsoyTableManager extends TableManager
{
    public MsoyTableManager (RootDObjectManager omgr, InvocationManager invmgr, PlaceRegistry plreg,
                             PlayerNodeActions playerActions, LobbyManager lmgr)
    {
        super(omgr, invmgr, plreg, lmgr.getLobbyObject());

        _playerActions = playerActions;
        _lmgr = lmgr;
        _lobj = lmgr.getLobbyObject();

        _allowBooting = true;
    }

    @Override
    protected GameConfig createConfig (Table table)
    {
        MsoyGameConfig config = (MsoyGameConfig)super.createConfig(table);
        _lmgr.initConfig(config);
        return config;
    }

    @Override
    protected void notePlayerAdded (Table table, BodyObject body)
    {
        super.notePlayerAdded(table, body);

        // mark this player as "in" this game
        PlayerObject plobj = (PlayerObject) body;
        _playerActions.updatePlayer(plobj.getMemberId(), new GameSummary(_lobj.game));
    }

    @Override
    protected Table notePlayerRemoved (int playerOid, BodyObject body)
    {
        // mark this player as no longer "in" this game, unless this method is being called because
        // the game itself started.
        PlayerObject plobj = (PlayerObject) body;
        if (plobj != null && !_membersPlaying.contains(plobj.getMemberId())) {
            _playerActions.updatePlayer(plobj.getMemberId(), null);
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
        if (table.players != null) {
            for (int ii = 0, nn = table.players.length; ii < nn; ii++) {
                MemberName member = (MemberName) table.players[ii];
                if (member != null) {
                    _membersPlaying.add(member.getMemberId());
                }
            }
        }

        super.gameCreated(table, gameobj, gmgr);
        gameobj.addListener(_playerUpdater);
    }

    @Override
    protected void purgeTable (Table table)
    {
        // check for players in local map - this is the last time we'll hear about this game, so
        // make sure the players really are cleared out.
        for (int ii = 0; table.players != null && ii < table.players.length; ii++) {
            if (table.players[ii] == null) {
                continue;
            }

            MemberName member = (MemberName) table.players[ii];
            if (_membersPlaying.remove(member.getMemberId())) {
                _playerActions.updatePlayer(member.getMemberId(), null);
            }
        }

        super.purgeTable(table);
    }

    protected OidListListener _playerUpdater = new OidListListener() {
        public void objectAdded (ObjectAddedEvent event) {
            PlayerObject plobj = (PlayerObject) _omgr.getObject(event.getOid());
            int memberId = plobj.getMemberId();
            if (!_membersPlaying.contains(memberId)) {
                _playerActions.updatePlayer(memberId, new GameSummary(_lobj.game));
            }
            _membersPlaying.add(memberId);
        }

        public void objectRemoved (ObjectRemovedEvent event) {
            PlayerObject plobj = (PlayerObject) _omgr.getObject(event.getOid());
            int memberId = plobj.getMemberId();
            _playerActions.updatePlayer(memberId, null);
            _membersPlaying.remove(memberId);
        }
    };

    protected LobbyManager _lmgr;
    protected LobbyObject _lobj;
    protected ArrayIntSet _membersPlaying = new ArrayIntSet();
    protected PlayerNodeActions _playerActions;
}
