//
// $Id$

package com.threerings.msoy.admin.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.admin.server.ConfigRegistry;

import com.threerings.msoy.admin.data.MoneyConfigObject;
import com.threerings.msoy.admin.data.ServerConfigObject;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoySession;
import com.threerings.msoy.server.persist.HotnessConfig;

import static com.threerings.msoy.Log.log;

/**
 * Provides access to runtime reconfigurable configuration data.
 */
@Singleton
public class RuntimeConfig
{
    /** Contains general server configuration data. */
    public final ServerConfigObject server = new ServerConfigObject();

    /** Contains money configuration data. */
    public final MoneyConfigObject money = new MoneyConfigObject();

    /**
     * Creates and registers the runtime configuration objects.
     */
    public void init (RootDObjectManager omgr, ConfigRegistry confReg)
    {
        registerObject(omgr, confReg, "server", server);
        registerObject(omgr, confReg, "money", money);

        // configure a listener to keep our new and hot dropoff days in sync
        _hconfig.setDropoffDays(server.newAndHotDropoffDays);
        server.addListener(new AttributeChangeListener() {
            public void attributeChanged (AttributeChangedEvent event) {
                if (ServerConfigObject.NEW_AND_HOT_DROPOFF_DAYS.equals(event.getName())) {
                    _hconfig.setDropoffDays(event.getIntValue());
                }
            }
        });
    }

    protected void registerObject (RootDObjectManager omgr, ConfigRegistry confReg,
                                   String key, DObject object)
    {
        // register the object with the distributed object system
        omgr.registerObject(object);
        // set the tight-ass access controller
        object.setAccessController(new AdminAccessController(omgr));
        // register the object with the config object registry
        confReg.registerObject(key, key, object);
    }

    /** An access controller that provides stricter-than-normal access for config objects. */
    protected static class AdminAccessController implements AccessController {
        public AdminAccessController (RootDObjectManager omgr) {
            _omgr = omgr;
        }

        public boolean allowSubscribe (DObject object, Subscriber<?> subscriber) {
            // if the subscriber is a client; make sure they're an admin
            if (MsoySession.class.isInstance(subscriber)) {
                MemberObject user = (MemberObject)
                    MsoySession.class.cast(subscriber).getClientObject();
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

    @Inject protected HotnessConfig _hconfig;
}
