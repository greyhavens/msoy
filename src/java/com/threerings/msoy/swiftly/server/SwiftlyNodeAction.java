//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.peer.data.MsoyNodeObject;

/**
 * A node action that locates and acts on the server hosting a particular Swiftly project.
 */
public abstract class SwiftlyNodeAction extends PeerManager.NodeAction
{
    public SwiftlyNodeAction (int projectId)
    {
        _projectId = projectId;
    }

    public SwiftlyNodeAction ()
    {
    }

    @Override // from PeerManager.NodeAction
    public boolean isApplicable (NodeObject nodeobj)
    {
        return ((MsoyNodeObject)nodeobj).hostedProjects.containsKey(_projectId);
    }

    @Override // from PeerManager.NodeAction
    protected void execute ()
    {
        execute(_swiftlyMan);
    }

    protected abstract void execute (SwiftlyManager swiftlyMan);

    protected int _projectId;

    /** Used once we arrive on our target server. */
    @Inject protected transient SwiftlyManager _swiftlyMan;
}
