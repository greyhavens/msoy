//
// $Id$

package com.threerings.msoy.server;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.CrowdObjectAccess;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.whirled.server.WhirledClient;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyTokenRing;

/**
 * Represents an attached Msoy client on the server-side.
 */
public class MsoyClient extends WhirledClient
{
    @Override // from PresentsClient
    protected BootstrapData createBootstrapData ()
    {
        return new MsoyBootstrapData();
    }

    @Override // from PresentsClient
    protected void populateBootstrapData (BootstrapData data)
    {
        super.populateBootstrapData(data);

        //((MsoyBootstrapData) data).chatOid = MsoyServer.chatOid;
    }

    @Override // from PresentsClient
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        _memobj = (MemberObject) _clobj;

        MsoyAuthenticator.Account acct = (MsoyAuthenticator.Account) _authdata;
        if (acct != null) {
            _memobj.setTokens(acct.tokens);
        } else {
            _memobj.setTokens(new MsoyTokenRing());
        }

        MsoyServer.registerMember(_memobj);

        _memobj.setAccessController(CrowdObjectAccess.USER);
    }

    @Override // from PresentsClient
    protected void sessionConnectionClosed ()
    {
        super.sessionConnectionClosed();

        // if we're a guest, end our session now, there's no way to reconnect
        if (_memobj == null || _memobj.isGuest()) {
            safeEndSession();
        }
    }

    @Override // from PresentsClient
    protected void sessionDidEnd ()
    {
        super.sessionDidEnd();

        if (_memobj != null) {
            MsoyServer.clearMember(_memobj);
            _memobj = null;
        }
    }

    @Override // from CrowdClient
    protected void clearLocation (BodyObject bobj)
    {
        super.clearLocation(bobj);
        try {
            MsoyServer.worldGameReg.leaveWorldGame((MemberObject)bobj);
        } catch (InvocationException e) {
            // a warning will have already been logged
        }
    }
    
    /** A casted reference to the userobject. */
    protected MemberObject _memobj;
}
