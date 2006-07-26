//
// $Id$

package com.threerings.msoy.server;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;

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

import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MemberObject;

/**
 * Represents an attached Msoy client on the server-side.
 */
public class MsoyClient extends WhirledClient
{
    // documentation inherited
    @Override
    protected BootstrapData createBootstrapData ()
    {
        return new MsoyBootstrapData();
    }

    // documentation inherited
    @Override
    protected void populateBootstrapData (BootstrapData data)
    {
        super.populateBootstrapData(data);

        //((MsoyBootstrapData) data).chatOid = MsoyServer.chatOid;
    }

    @Override
    protected void assignStartingUsername ()
    {
        Name credName = _creds.getUsername();
        if (null == credName) {
            _username = getNextGuestName();

        } else {
            _username = credName;
        }
    }

    @Override
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        _clobj.setAccessController(new AccessController() {
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
    }

    @Override
    public void clientResolved (Name username, ClientObject clobj)
    {
        _clobj = (MemberObject) clobj;

        super.clientResolved(username, clobj);

        // TEMP code to alter avatar/chat styles
        clobj.addListener(new MessageListener() {
            public void messageReceived (MessageEvent event) {
                if ("alterTEMP".equals(event.getName())) {
                    String frob = (String) event.getArgs()[0];
                    _clobj.alter(frob);

                    PlaceManager plmgr = MsoyServer.plreg.getPlaceManager(
                        _clobj.location);
                    if (plmgr != null) {
                        plmgr.updateOccupantInfo(_clobj.createOccupantInfo());
                    }
                }
            }
        });
    }

    @Override
    public void endSession ()
    {
        super.endSession();
        _clobj = null;
    }

    // TEMP: assign sequential guest ids
    protected static Name getNextGuestName ()
    {
        String val = String.valueOf(++_guestCount);
        while (val.length() < 3) {
            val = "0" + val;
        }
        return new Name("guest" + val);
    }
    protected static int _guestCount;
    // END: Temp

    /** A casted reference to the userobject. */
    protected MemberObject _clobj;
}
