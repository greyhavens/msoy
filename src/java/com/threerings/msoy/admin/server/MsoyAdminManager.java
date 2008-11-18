//
// $Id$

package com.threerings.msoy.admin.server;

import java.util.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;
import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.util.MessageBundle;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.RebootManager;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.money.server.MoneyExchange;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.admin.data.ServerConfigObject;
import com.threerings.msoy.admin.data.StatusObject;

import static com.threerings.msoy.Log.log;

/**
 * Handles administrative bits for the MetaSOY server.
 */
@EventThread @Singleton
public class MsoyAdminManager
{
    /**
     * Prepares the admin manager for operation.
     */
    public void init ()
    {
        // create our reboot manager
        _rebmgr = new MsoyRebootManager(_shutmgr, _omgr);

        // start up the system "snapshot" logger
        _snapshotLogger = new SnapshotLogger();
        _snapshotLogger.schedule(0, STATS_DELAY);

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
            _shutmgr.shutdown();
            return;
        }

        // shave 5 seconds off to avoid rounding up to the next time
        long when = System.currentTimeMillis() + minutes * 60 * 1000L - 5000L;
        _rebmgr.scheduleReboot(when, initiator);
    }

    /** Used to manage automatic reboots. */
    protected class MsoyRebootManager extends RebootManager
        implements AttributeChangeListener
    {
        public MsoyRebootManager (ShutdownManager shutmgr, RootDObjectManager omgr) {
            super(shutmgr, omgr);
            _runtime.server.addListener(this);
            _runtime.server.setCustomRebootMsg("");
        }

        public void scheduleReboot (long rebootTime, String initiator) {
            super.scheduleReboot(rebootTime, initiator);
            final Date when = new Date(rebootTime);

            // if we are the (production) server that originated this reboot, fire off an email to
            // the agents if we're in production (this is safe to do on the dobject thread)
            if (!DeploymentConfig.devDeployment && !AUTOMATIC_INITIATOR.equals(initiator)) {
                final String body = "A Whirled reboot has been scheduled for " + when +
                    " by " + initiator + ".\n\nThank you. Please drive through.";
                _sender.sendEmail(ServerConfig.getAgentsAddress(), ServerConfig.getFromAddress(),
                                  "Whirled Reboot Scheduled", body);
            }

            log.info("Scheduling reboot on " + when + " for " + initiator + ".");
        }

        // from interface AttributeChangeListener
        public void attributeChanged (AttributeChangedEvent event)
        {
            if (!ServerConfigObject.NEXT_REBOOT.equals(event.getName())) {
                return;
            }

            // figure out who requested the reboot
            DObject o = _omgr.getObject(event.getSourceOid());
            String blame;
            if (o == null) {
                blame = AUTOMATIC_INITIATOR;
            } else if (o instanceof MemberObject) {
                blame = String.valueOf(((MemberObject)o).memberName);
            } else {
                blame = o.toString();
            }

            // schedule a reboot
            scheduleReboot(_runtime.server.nextReboot, blame);
        }

        protected void broadcast (String message) {
            _chatprov.broadcast(null, MsoyCodes.GENERAL_MSGS, message, true, false);
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
            return MessageBundle.taint(_runtime.server.customRebootMsg);
        }
    }

    /** Logs a 'snapshot' of the server state on a regular basis. */
    protected class SnapshotLogger extends Interval
    {
        public SnapshotLogger ()
        {
            super(_omgr);
        }

        public void expired () {
            // iterate over the list of members, adding up a total, as well as counting up subsets
            // of active users and guest users
            int total = 0, active = 0, guests = 0, viewers = 0;
            for (MemberObject memobj : _locator.getMembersOnline()) {
                total++;
                active += (memobj.status == OccupantInfo.ACTIVE) ? 1 : 0;
                if (memobj.isGuest()) {
                    if (memobj.getMemberId() == 0) {
                        viewers++;
                    } else {
                        guests++;
                    }
                }
            }
            _eventLog.currentMemberStats(ServerConfig.nodeName, total, active, guests, viewers);

            // now log the current money exchange rate
            _eventLog.moneyExchangeRate(ServerConfig.nodeName, _exchange.getRate());
        }
    }

    /** Logs a snapshot of the running server every 10 minutes. */
    protected SnapshotLogger _snapshotLogger;

    /** Handles our reboot coordinations. */
    protected MsoyRebootManager _rebmgr;

    @Inject protected RuntimeConfig _runtime;
    @Inject protected ShutdownManager _shutmgr;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MailSender _sender;
    @Inject protected MemberLocator _locator;
    @Inject protected ChatProvider _chatprov;
    @Inject protected MoneyExchange _exchange;

    /** 10 minute delay between logged snapshots, in milliseconds. */
    protected static final long STATS_DELAY = 1000 * 60 * 10;

        protected static final String AUTOMATIC_INITIATOR = "automatic";
}
