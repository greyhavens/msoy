//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MsoyServer;

/**
 * An action to be invoked on every server on which a member is logged in. You must read {@link
 * NodeAction} for caveats before using this class.
 */
public abstract class MemberNodeAction extends PeerManager.NodeAction
{
    public MemberNodeAction (int memberId)
    {
        _memberId = memberId;
    }

    @Override // from PeerManager.NodeAction
    public boolean isApplicable (NodeObject nodeobj)
    {
        return ((MsoyNodeObject)nodeobj).clients.containsKey(new MemberName(null, _memberId));
    }

    @Override // from PeerManager.NodeAction
    protected void execute ()
    {
        MemberObject memobj = MsoyServer.lookupMember(_memberId);
        if (memobj != null) {
            execute(memobj);
        } // if not, oh well, they went away
    }

    protected abstract void execute (MemberObject memobj);

    protected int _memberId;
}
