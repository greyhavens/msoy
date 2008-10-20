//
// $Id$

package com.threerings.msoy.admin.server;

import java.lang.reflect.Field;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.admin.server.ConfigRegistry;

import com.threerings.msoy.admin.data.MoneyConfigObject;
import com.threerings.msoy.admin.data.ServerConfigObject;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyClient;

import static com.threerings.msoy.Log.log;

/**
 * Provides access to runtime reconfigurable configuration data.
 */
public class RuntimeConfig
{
    /** Contains general server configuration data. */
    public static ServerConfigObject server = new ServerConfigObject();

    /** Contains money configuration data. */
    public static MoneyConfigObject money = new MoneyConfigObject();

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
                object.setAccessController(new AdminAccessController(omgr));

                // register the object with the config object registry
                confReg.registerObject(key, key, object);

                // and set our static field
                field.set(null, object);

            } catch (Exception e) {
                log.warning("Failed to set " + key + ".", e);
            }
        }
    }

    /** An access controller that provides stricter-than-normal access for config objects. */
    protected static class AdminAccessController implements AccessController {
        public AdminAccessController (RootDObjectManager omgr) {
            _omgr = omgr;
        }

        public boolean allowSubscribe (DObject object, Subscriber<?> subscriber) {
            // if the subscriber is a client; make sure they're an admin
            if (MsoyClient.class.isInstance(subscriber)) {
                MemberObject user = (MemberObject)
                    MsoyClient.class.cast(subscriber).getClientObject();
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

            DObject obj = _omgr.getObject(sourceOid);
            if (!(obj instanceof MemberObject)) {
                return false;
            }

            // make sure the originator is an admin
            MemberObject user = (MemberObject)obj;
            if (!user.tokens.isAdmin()) {
                return false;
            }

            // non-maintainers can only update reboot related fields
            if (!isRebootUpdate(event) && !user.tokens.isMaintainer()) {
                return false;
            }

            // admins are allowed to change things, but let's log it
            log.info("Admin configuration change [who=" + user.username +
                     ", object=" + object.getClass().getName() + ", change=" + event + "].");
            return true;
        }

        protected boolean isRebootUpdate (DEvent event) {
            if (!(event instanceof AttributeChangedEvent)) {
                return false;
            }
            AttributeChangedEvent ace = (AttributeChangedEvent)event;
            return (ace.getName().equals(ServerConfigObject.CUSTOM_REBOOT_MSG) ||
                    ace.getName().equals(ServerConfigObject.NEXT_REBOOT));
        }

        protected RootDObjectManager _omgr;
    };
}
