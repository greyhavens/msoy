//
// $Id: MsoyServer.java 19621 2010-11-23 22:21:17Z charlie $

package com.threerings.msoy.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.ObserverList;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.ReportManager;

import com.threerings.msoy.data.MemberClientObject;
import com.threerings.msoy.data.MemberObject;

import static com.threerings.msoy.Log.log;

/**
 */
@Singleton
public class MsoyClientManager extends ClientManager
{
    /**
     * Constructs a client manager that will interact with the supplied connection manager.
     */
    @Inject public MsoyClientManager (ReportManager repmgr, Lifecycle cycle)
    {
        super(repmgr, cycle);
    }

    @Override // from ClientManager
    protected void clientSessionDidStart (final PresentsSession session)
    {
        ClientObject clobj = session.getClientObject();
        if (clobj instanceof MemberClientObject) {
            if (((MemberClientObject) clobj).bodyOid == 0) {
                // listen for bodyOid to be set
                clobj.addListener(new AttributeChangeListener() {
                    public void attributeChanged (AttributeChangedEvent event) {
                        if (MemberClientObject.BODY_OID.equals(event.getName())) {
                            clientSessionReallyDidStart(session);
                        }
                    }
                });
                return;
            }
        }
        // else go right now
        clientSessionReallyDidStart(session);
    }

    protected void clientSessionReallyDidStart (final PresentsSession session)
    {
        ClientObject clobj = session.getClientObject();
        if (clobj instanceof MemberClientObject) {
            MemberClientObject mcobj = (MemberClientObject) clobj;
            if (mcobj.memobj != null) {
                mcobj.addListener(new ClientObjectDeathListener(mcobj.memobj));
            } else {
                log.warning("MemberClientObject session started without MemberObject",
                    "who", mcobj.who());
            }
        }

        // let the observers know
        _clobservers.apply(new ObserverList.ObserverOp<ClientObserver>() {
            public boolean apply (ClientObserver observer) {
                observer.clientSessionDidStart(session);
                return true;
            }
        });
    }

    protected static class ClientObjectDeathListener implements ObjectDeathListener {
        public void objectDestroyed (ObjectDestroyedEvent event) {
            // sanity checks
            if (!_memobj.isActive()) {
                log.warning("What the... almost destroyed already-destroyed MemberObject",
                    "member", _memobj.who());
                return;
            }
            if (_memobj.location != null) {
                log.warning("Erk, destroying MemberObject in a location", "member",
                    _memobj.who(), "location", _memobj.location);
                // but we'll accept it
            }
            // proceed with the destruction
            _memobj.destroy();
        }

        protected ClientObjectDeathListener (MemberObject memobj)
        {
            _memobj = memobj;
        }

        protected MemberObject _memobj;
    }
}
