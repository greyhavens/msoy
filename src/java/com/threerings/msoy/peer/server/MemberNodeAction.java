//
// $Id$

package com.threerings.msoy.peer.server;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthName;
import com.threerings.msoy.server.MemberLocator;

import static com.threerings.msoy.Log.log;

/**
 * An action to be invoked on every server on which a member is logged in. You must read {@link
 * PeerManager.NodeAction} for caveats before using this class.
 */
public abstract class MemberNodeAction extends PeerManager.NodeAction
{
    public MemberNodeAction (int memberId)
    {
        _memberId = memberId;
        _memberKey = MsoyAuthName.makeKey(memberId);
    }

    public MemberNodeAction ()
    {
    }

    @Override // from PeerManager.NodeAction
    public boolean isApplicable (NodeObject nodeobj)
    {
        return nodeobj.clients.containsKey(_memberKey);
    }

    @Override // from PeerManager.NodeAction
    protected void execute ()
    {
        MemberObject memobj = _locator.lookupMember(_memberId);
        if (memobj != null) {
            if (!memobj.isActive()) {
                log.warning("WTF? Got an inactive member from the locator?", "who", memobj.who());
            } else {
                execute(memobj);
            }
        } // if not, oh well, they went away
    }

    protected abstract void execute (MemberObject memobj);

    protected int _memberId;

    protected transient MsoyAuthName _memberKey;

    /** Used to look up member objects. */
    @Inject protected transient MemberLocator _locator;
}
