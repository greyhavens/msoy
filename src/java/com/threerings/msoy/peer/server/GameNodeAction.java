//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.peer.data.MsoyNodeObject;

/**
 * Used to dispatch node actions on servers that are hosting a particular game.
 */
public abstract class GameNodeAction extends PeerManager.NodeAction
{
    public GameNodeAction (int gameId)
    {
        _gameId = gameId;
    }

    public GameNodeAction ()
    {
    }

    @Override // from PeerManager.NodeAction
    public boolean isApplicable (NodeObject nodeobj)
    {
        return ((MsoyNodeObject)nodeobj).hostedGames.containsKey(_gameId);
    }

    protected int _gameId;
}
