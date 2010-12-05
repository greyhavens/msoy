//
// $Id$

package com.threerings.msoy.server;

import com.threerings.crowd.data.BodyObject;

import com.threerings.bureau.data.BureauClientObject;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.ProxySubscriber;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.server.PresentsObjectAccess;

import com.threerings.msoy.data.MemberClientObject;

import com.google.inject.Inject;

import static com.threerings.presents.Log.log;

/**
 * Contains standard access controllers for msoy.
 */
public class MsoyObjectAccess
{
    /** The default access controller. */
    public static AccessController DEFAULT = PresentsObjectAccess.DEFAULT;

    public static AccessController USER = new AccessController ()
    {
        public boolean allowSubscribe (DObject object, Subscriber<?> sub)
        {
            if (sub instanceof ProxySubscriber) {
                ClientObject co = ((ProxySubscriber)sub).getClientObject();
                if (co instanceof BureauClientObject) {
                    return true;
                }
                BodyObject body = null;
                if (co instanceof MemberClientObject) {
                    body = ((MemberClientObject) co).memobj;
                }
                if (object != body && object != co) {
                    log.warning("Refusing ClientObject subscription request",
                        "body", (body != null) ? body.who() : "null", "sub", co.who(),
                        "obj", object);
                    return false;
                }
            }
            return true;
        }

        public boolean allowDispatch (DObject object, DEvent event)
        {
            return PresentsObjectAccess.CLIENT.allowDispatch(object, event);
        }
    };

    /**
     * The player access controller is identical to the user one, except it also allows bureau
     * clients to subscribe.
     */
    public static AccessController PLAYER = new AccessController()
    {
        public boolean allowSubscribe (DObject object, Subscriber<?> sub)
        {
            if (sub instanceof ProxySubscriber) {
                ClientObject co = ((ProxySubscriber)sub).getClientObject();
                if (co instanceof BureauClientObject) {
                    return true;
                }
            }
            return USER.allowSubscribe(object, sub);
        }

        public boolean allowDispatch (DObject object, DEvent event)
        {
            return USER.allowDispatch(object, event);
        }

    };

    /** The game access controller. */
    public static AccessController GAME = new AccessController() {
        public boolean allowSubscribe (DObject object, Subscriber<?> sub)
        {
            return true;
        }

        public boolean allowDispatch (DObject object, DEvent event)
        {
            if (event instanceof MessageEvent) {
                // we actually block messages from clients
                return (event.getSourceOid() == -1);
            }

            return DEFAULT.allowDispatch(object, event);
        }
    };
}
