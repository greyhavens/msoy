//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.peer.data.MsoyNodeObject;

/**
 * An action to be invoked on every server on which a player is logged in. This is different from
 * MemberNodeAction which locates the world session for a member. This locates the member's game
 * session and operates on their {@link PlayerObject}.
 *
 * You must read {@link PeerManager.NodeAction} for caveats before using this class.
 */
public abstract class PlayerNodeAction extends PeerManager.NodeAction
{
    public PlayerNodeAction (int playerId)
    {
        _playerId = playerId;
    }

    public PlayerNodeAction ()
    {
    }

    @Override // from PeerManager.NodeAction
    public boolean isApplicable (NodeObject nodeobj)
    {
        // TODO: fixy fixy
        // return ((MsoyNodeObject)nodeobj).clients.containsKey(MemberName.makeKey(_playerId));
        return false;
    }

    @Override // from PeerManager.NodeAction
    protected void execute ()
    {
        PlayerObject plobj = _locator.lookupPlayer(_playerId);
        if (plobj != null) {
            execute(plobj);
        } // if not, oh well, they went away
    }

    protected abstract void execute (PlayerObject plobj);

    protected int _playerId;

    /** Used to look up member objects. */
    @Inject protected transient PlayerLocator _locator;
}
