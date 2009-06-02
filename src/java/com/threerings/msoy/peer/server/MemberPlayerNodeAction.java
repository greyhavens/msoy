//
// $Id$

package com.threerings.msoy.peer.server;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.NodeObject;

import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.server.PlayerLocator;

/**
 * Applies an action to both a player's MemberObject AND PlayerObject.
 */
public abstract class MemberPlayerNodeAction extends MemberNodeAction
{
    public MemberPlayerNodeAction (int memberId)
    {
        super(memberId);
        _playerKey = GameAuthName.makeKey(memberId);
    }

    public MemberPlayerNodeAction ()
    {
    }

    @Override
    public boolean isApplicable (NodeObject nodeobj)
    {
        return super.isApplicable(nodeobj) || nodeobj.clients.containsKey(_playerKey);
    }

    @Override
    protected void execute ()
    {
        super.execute();
        // AND...
        PlayerObject plobj = _plocator.lookupPlayer(_memberId);
        if (plobj != null) {
            execute(plobj);
        } // if not, oh well
    }

    protected abstract void execute (PlayerObject plobj);

    protected transient GameAuthName _playerKey;

    /** Used to look up player objects. */
    @Inject protected transient PlayerLocator _plocator;
}
