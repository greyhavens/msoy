//
// $Id$

package com.threerings.msoy.admin.server;

import java.lang.reflect.Field;
import java.util.logging.Level;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.admin.server.ConfigRegistry;

import com.threerings.msoy.admin.data.ServerConfigObject;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyClient;
import com.threerings.msoy.server.MsoyBaseServer;

import static com.threerings.msoy.Log.log;

/**
 * Provides access to runtime reconfigurable configuration data.
 */
public class RuntimeConfig
{
    /** Contains general server configuration data. */
    public static ServerConfigObject server = new ServerConfigObject();

    /**
     * Creates and registers the runtime configuration objects.
     */
    public static void init (RootDObjectManager omgr, ConfigRegistry confReg)
    {
        Field[] fields = RuntimeConfig.class.getDeclaredFields();
        for (int ii = 0; ii < fields.length; ii++) {
            final Field field = fields[ii];
            final Class<?> oclass = field.getType();
            if (!DObject.class.isAssignableFrom(oclass)) {
                continue;
            }

            String key = field.getName();
            try {
                // create and register the object
                DObject object = omgr.registerObject((DObject)field.get(null));

                // set the tight-ass access controller
                object.setAccessController(ADMIN_CONTROLLER);

                // register the object with the config object registry
                confReg.registerObject(key, key, object);

                // and set our static field
                field.set(null, object);

            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to set " + key + ".", e);
            }
        }
    }

    /** An access controller that provides stricter-than-normal access to these configuration
     * objects. */
    protected static AccessController ADMIN_CONTROLLER = new AccessController()
    {
        public boolean allowSubscribe (DObject object, Subscriber subscriber) {
            // if the subscriber is a client; make sure they're an admin
            if (subscriber instanceof MsoyClient) {
                MemberObject user = (MemberObject)((MsoyClient)subscriber).getClientObject();
                return user.tokens.isAdmin();
            }
            return true;
        }

        public boolean allowDispatch (DObject object, DEvent event) {
            // look up the user object of the event originator
            int sourceOid = event.getSourceOid();
            if (sourceOid == -1) {
                return true; // server: ok
            }

            // make sure the originator is an admin
            DObject obj = MsoyBaseServer.omgr.getObject(sourceOid);
            if (!(obj instanceof MemberObject)) {
                return false;
            }
            MemberObject user = (MemberObject)obj;
            if (!user.tokens.isAdmin()) {
                return false;
            }

            // admins are allowed to change things, but let's log it
            MsoyBaseServer.generalLog("admin_config changed " + user.username + " " +
                                      object.getClass().getName() + " " + event);
            return true;
        }
    };
}
