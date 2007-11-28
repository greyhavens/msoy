//
// $Id$

package com.threerings.msoy.peer.server;

import java.io.Serializable;

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
    /**
     * Provides this action with its configuration. We use this instead of the constructor because
     * you will have to statically extend this class (anonymous extension is not allowed)
     */
    public MemberNodeAction init (int memberId, Serializable ... args)
    {
        _memberId = memberId;
        init(args);
        return this;
    }

    @Override // from PeerManager.NodeAction
    public boolean isApplicable (NodeObject nodeobj)
    {
        return ((MsoyNodeObject)nodeobj).clients.containsKey(new MemberName(null, _memberId));
    }

    @Override // from PeerManager.NodeAction
    protected void execute (Object[] args)
    {
        MemberObject memobj = MsoyServer.lookupMember(_memberId);
        if (memobj != null) {
            execute(memobj, args);
        } // if not, oh well, they went away
    }

    protected abstract void execute (MemberObject memobj, Object[] args);

    protected int _memberId;
}
