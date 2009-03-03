//
// $Id$

package com.threerings.msoy.web.server;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.NodeObject;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Provides various services to servlets.
 */
@Singleton
public class ServletLogic
{
    /**
     * Invokes the supplied operation on all peer nodes (on the distributed object manager thread)
     * and blocks the current thread until the execution has completed.
     */
    public void invokePeerOperation (String name, final Function<NodeObject,Void> op)
        throws ServiceException
    {
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(name);
        _omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    _peerMan.applyToNodes(op);
                    waiter.requestCompleted(null);
                } catch (Exception e) {
                    waiter.requestFailed(e);
                }
            }
        });
        waiter.waitForResult();
    }

    // our dependencies
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MsoyPeerManager _peerMan;
}
