//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ProxySubscriber;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.msoy.server.MsoyObjectAccess;

import com.threerings.msoy.party.data.PartierObject;
import com.threerings.msoy.party.data.PartyObject;

public class PartyAccessController implements AccessController
{
    public PartyAccessController (PartyManager mgr)
    {
        _mgr = mgr;
    }

    // from AccessController
    public boolean allowSubscribe (DObject object, Subscriber<?> sub)
    {
        // if the subscriber is a client, ensure that they are in this party
        if (sub instanceof ProxySubscriber) {
            PartierObject pobj = (PartierObject) ((ProxySubscriber) sub).getClientObject();
            PartyObject partyObj = (PartyObject)object;
            boolean canSubscribe = (pobj.partyId == partyObj.id);
            if (canSubscribe) {
                _mgr.clientSubscribed(pobj);
            }
            return canSubscribe;
        }

        // else: server
        return true;
    }

    // from AccessController
    public boolean allowDispatch (DObject object, DEvent event)
    {
        return MsoyObjectAccess.DEFAULT.allowDispatch(object, event);
    }

    protected PartyManager _mgr;
}
