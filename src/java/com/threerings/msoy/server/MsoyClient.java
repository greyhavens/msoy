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

        _memobj.setAccessController(new AccessController() {
            public boolean allowSubscribe (DObject obj, Subscriber sub) {
                return CrowdObjectAccess.USER.allowSubscribe(obj, sub);
            }

            public boolean allowDispatch (DObject obj, DEvent event) {
                if ((event instanceof MessageEvent) &&
                        "alterTEMP".equals(((MessageEvent) event).getName())) {
                    return true;
                }
                return CrowdObjectAccess.USER.allowDispatch(obj, event);
            }
        });

        // TEMP code to alter avatar/chat styles
        _memobj.addListener(new MessageListener() {
            public void messageReceived (MessageEvent event) {
                if ("alterTEMP".equals(event.getName())) {
                    String frob = (String) event.getArgs()[0];
                    _memobj.alter(frob);

                    PlaceManager plmgr = MsoyServer.plreg.getPlaceManager(
                        _memobj.location);
                    if (plmgr != null) {
                        plmgr.updateOccupantInfo(
                            _memobj.createOccupantInfo(plmgr.getPlaceObject()));
                    }
                }
            }
        });
        // END TEMP
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

    /** A casted reference to the userobject. */
    protected MemberObject _memobj;
}
