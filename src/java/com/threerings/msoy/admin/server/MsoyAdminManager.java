//
// $Id$

package com.threerings.msoy.admin.server;

import com.samskivert.util.Interval;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.RebootManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.admin.data.ServerConfigObject;
import com.threerings.msoy.admin.data.StatusObject;

import static com.threerings.msoy.Log.log;

/**
 * Handles administrative bits for the MetaSOY server.
 */
public class MsoyAdminManager
//    implements MsoyAdminProvider
{
    /** Contains server status information published to admins. */
    public StatusObject statObj;

    /**
     * Prepares the admin manager for operation.
     */
    public void init (MsoyServer server)
    {
        _server = server;
        _rebmgr = new MsoyRebootManager(server);

        // create and configure our status object
        statObj = MsoyServer.omgr.registerObject(new StatusObject());
        statObj.serverStartTime = System.currentTimeMillis();
//         statObj.setService((MsoyAdminMarshaller)
//                            MsoyServer.invmgr.registerDispatcher(new MsoyAdminDispatcher(this)));

        // start up our connection manager stat monitor
        _conmgrStatsUpdater.schedule(5000L, true);

        // initialize our reboot manager
        _rebmgr.init();
    }

    /**
     * Schedules a reboot for the specified number of minutes in the future.
     */
    public void scheduleReboot (int minutes, String initiator)
    {
        // if this is a zero minute reboot, just do the deed
        if (minutes == 0) {
            log.info("Performing immediate shutdown [for=" + initiator + "].");
            _server.shutdown();
            return;
        }

        // shave 5 seconds off to avoid rounding up to the next time
        long when = System.currentTimeMillis() + minutes * 60 * 1000L - 5000L;
        _rebmgr.scheduleReboot(when, initiator);
    }

//     // from interface MsoyAdminProvider
//     public void scheduleReboot (ClientObject caller, int minutes)
//     {
//         MemberObject user = (MemberObject)caller;
//         if (!user.tokens.isSupport()) {
//             log.warning("Got reboot schedule request from non-admin/support " +
//                         "[who=" + user.who() + "].");
//             return;
//         }
//         scheduleReboot(minutes, user.who());
//     }

    /** Used to manage automatic reboots. */
    protected class MsoyRebootManager extends RebootManager
        implements AttributeChangeListener
    {
        public MsoyRebootManager (MsoyServer server) {
            super(server);
            RuntimeConfig.server.addListener(this);
            RuntimeConfig.server.setCustomRebootMsg("");
        }

        public void scheduleReboot (long rebootTime, String initiator) {
            super.scheduleReboot(rebootTime, initiator);
            if (rebootTime != RuntimeConfig.server.nextReboot) {
                RuntimeConfig.server.setNextReboot(rebootTime);
                statObj.setServerRebootTime(rebootTime);
            }
        }

        // from interface AttributeChangeListener
        public void attributeChanged (AttributeChangedEvent event)
        {
            if (!ServerConfigObject.NEXT_REBOOT.equals(event.getName())) {
                return;
            }

            // figure out who requested the reboot
            DObject o = MsoyServer.omgr.getObject(event.getSourceOid());
            String blame;
            if (o == null) {
                blame = "automatic";
            } else if (o instanceof MemberObject) {
                blame = String.valueOf(((MemberObject)o).memberName);
            } else {
                blame = o.toString();
            }

            // schedule a reboot
            scheduleReboot(RuntimeConfig.server.nextReboot, blame);
        }

        protected void broadcast (String message) {
            MsoyServer.chatprov.broadcast(null, MsoyCodes.GENERAL_MSGS, message, true, false);
        }

        protected int getDayFrequency () {
            return -1; // no automatically scheduled reboots for now
        }

        protected int getRebootHour () {
            return 8;
        }

        protected boolean getSkipWeekends () {
            return false;
        }

        protected String getCustomRebootMessage () {
            // for now we don't have auto-reboots, so let's not claim every hand scheduled reboot
            // is a "regularly scheduled reboot"
            return MessageBundle.taint(RuntimeConfig.server.customRebootMsg);
        }
    }

    /** This reads the status from the connection manager and stuffs it into
     * our server status object every 5 seconds. Because it reads synchronized
     * data and then just posts an event, it's OK that it runs directly on the
     * Interval dispatch thread. */
    protected Interval _conmgrStatsUpdater = new Interval() {
        public void expired () {
            statObj.setConnStats(MsoyServer.conmgr.getStats());
        }
    };

    protected MsoyServer _server;
    protected MsoyRebootManager _rebmgr;
}
