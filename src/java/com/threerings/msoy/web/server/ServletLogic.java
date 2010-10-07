//
// $Id$

package com.threerings.msoy.web.server;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.web.gwt.ServiceException;
import com.threerings.web.server.ServletWaiter;

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
    public void invokePeerOperation (String name, final Function<MsoyNodeObject,Void> op)
        throws ServiceException
    {
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(name);
        _omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    for (MsoyNodeObject mnobj : _peerMan.getMsoyNodeObjects()) {
                        op.apply(mnobj);
                    }
                    waiter.requestCompleted(null);
                } catch (Exception e) {
                    waiter.requestFailed(e);
                }
            }
        });
        waiter.waitForResult();
    }

    // our dependencies
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected RootDObjectManager _omgr;
}
