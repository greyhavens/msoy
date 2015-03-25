//
// $Id$

package com.threerings.msoy.admin.server;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.sf.ehcache.CacheManager;

import com.samskivert.util.Interval;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.Tuple;

import com.samskivert.depot.PersistenceContext;

import com.threerings.util.MessageBundle;

import com.threerings.presents.annotation.AnyThread;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsServer;
import com.threerings.presents.server.RebootManager;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.admin.server.AdminManager;

import com.threerings.orth.notify.data.GenericNotification;
import com.threerings.orth.notify.data.Notification;

import com.threerings.pulse.server.PresentsPulseManager;

import com.threerings.msoy.admin.client.PeerAdminService;
import com.threerings.msoy.admin.data.PeerAdminMarshaller;
import com.threerings.msoy.admin.data.ServerConfigObject;
import com.threerings.msoy.admin.gwt.StatsModel;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.money.server.MoneyExchange;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.util.MailSender;
// import com.threerings.msoy.web.server.RPCProfiler;

import static com.threerings.msoy.Log.log;

/**
 * Handles administrative bits for the MetaSOY server.
 */
@EventThread @Singleton
public class MsoyAdminManager extends AdminManager
    implements PeerAdminProvider
{
    @Inject public MsoyAdminManager (InvocationManager invmgr)
    {
        super(invmgr);
    }

    /**
     * Prepares the admin manager for operation.
     */
    public void init (InvocationManager invmgr, CacheManager cacheMgr)
    {
        // start up the system "snapshot" logger
        _snapshotLogger = new SnapshotLogger();
        _snapshotLogger.schedule(0, STATS_DELAY);

        // initialize our reboot manager
        _rebmgr.init();

        // register our peer service (now that the peer node object is created)
        _peerMan.getMsoyNodeObject().setPeerAdminService(
            invmgr.registerProvider(this, PeerAdminMarshaller.class));

        // register our stat collectors
        _collectors.put(StatsModel.Type.DEPOT, new DepotStatCollector(_perCtx));
        _collectors.put(StatsModel.Type.DEPOT_QUERIES, new DepotQueriesStatCollector(_perCtx));
        _collectors.put(StatsModel.Type.CACHE, new CacheStatCollector(cacheMgr));
        // _collectors.put(StatsModel.Type.RPC, new RPCStatCollector(_rpcProfiler));

        // start up the pulse recorder
        _pulseMan.init(ServerConfig.nodeName);
    }

    /**
     * Schedules a reboot for the specified number of minutes in the future. Note well: to reboot
     * all peers, DO NOT use this method. You must instead post an event to update
     * {@link ServerConfigObject#nextReboot}.
     */
    public void scheduleReboot (int minutes, String initiator)
    {
        if (minutes == 0) {
            // if this is a zero minute reboot, do it in one second so that we have a chance to
            // send our response back to the requester
            log.info("Performing immediate shutdown", "for", initiator);
            new Interval(_omgr) {
                public void expired () {
                    _lifecycle.shutdown();
                }
            }.schedule(1000L);

        } else if (minutes < 0) {
            log.info("Reverting to regularly scheduled reboot", "for", initiator);
            // revert to our next regularly scheduled reboot
            if (!_rebmgr.scheduleRegularReboot()) {
                // if we have no such thing, schedule one for long time future
                _rebmgr.scheduleReboot(System.currentTimeMillis() + 365*24*60*60*1000L, initiator);
            }

        } else {
            log.info("Scheduling reboot", "mins", minutes, "for", initiator);
            // shave 5 seconds off to avoid rounding up to the next time
            long when = System.currentTimeMillis() + minutes*60*1000L - 5000L;
            _rebmgr.scheduleReboot(when, initiator);
        }
    }

    /**
     * Are we scheduled to reboot soon?
     */
    public boolean willRebootSoon ()
    {
        return _rebmgr.willShutdownSoon();
    }

    /**
     * Compiles statistics from this and the other peers in this network. The returned result will
     * not be ready until responses have been received from all peers.
     */
    @AnyThread
    public Future<StatsModel> compilePeerStatistics (final StatsModel.Type type)
    {
        final StatCollector.Merger merger = _collectors.get(type).createMerger();
        // first queue up requests from all other servers
        merger.pendingNodes = _peerMan.invokeOnNodes(
            new Function<Tuple<Client, NodeObject>, Boolean>() {
            public Boolean apply (Tuple<Client, NodeObject> args) {
                ((MsoyNodeObject)args.right).peerAdminService.compileStatistics(
                    type, merger.makeListener(args.right.nodeName));
                return true;
            }
        });
        try { // then get our info (which will complete immediately if there are no other servers)
            merger.pendingNodes++;
            compileStatistics(null, type, merger.makeListener(ServerConfig.nodeName));
        } catch (InvocationException ie) {
            merger.requestFailed(ie.getMessage());
        }
        return merger;
    }

    // from PeerAdminProvider
    public void compileStatistics (ClientObject caller, StatsModel.Type type,
                                   PeerAdminService.ResultListener listener)
        throws InvocationException
    {
        listener.requestProcessed(_collectors.get(type).compileStats());
    }

    /** Used to manage automatic reboots. */
    @Singleton
    protected static class MsoyRebootManager extends RebootManager
        implements AttributeChangeListener
    {
        @Inject public MsoyRebootManager (PresentsServer server, RootDObjectManager omgr) {
            super(server, omgr);
        }

        @Override // from RebootManager
        public void init ()
        {
            super.init();
            _runtime.server.addListener(this);
            _runtime.server.setCustomRebootMsg("");
        }

        @Override
        public void scheduleReboot (long rebootTime, String initiator) {
            super.scheduleReboot(rebootTime, initiator);
            final Date when = new Date(rebootTime);

            // if we are the server that originated this reboot and it is not automatic,
            // advise the agents
            if (!AUTOMATIC_INITIATOR.equals(initiator)) {
                boolean wasServletReboot = rebootTime == _runtime.server.servletReboot;
                boolean wasOurNodeServlet = _peerMan.getNodeObject().nodeName.equals(
                    _runtime.server.servletRebootNode);
                if (!wasServletReboot || wasOurNodeServlet) {
                    adviseAgents(when, initiator);
                }
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
                if (_runtime.server.nextReboot == _runtime.server.servletReboot) {
                    blame = _runtime.server.servletRebootInitiator;

                } else {
                    blame = AUTOMATIC_INITIATOR;
                }
            } else if (o instanceof MemberObject) {
                blame = String.valueOf(((MemberObject)o).memberName);
            } else {
                blame = o.toString();
            }

            // schedule a reboot
            scheduleReboot(_runtime.server.nextReboot, blame);
        }

        /**
         * Fire off an email to the agents (this is safe to do on the dobject thread). For
         * development deployments, just log a message instead.
         */
        protected void adviseAgents (Date rebootTime, String initiator) {
            final String body = "A Whirled reboot has been scheduled for " + rebootTime +
                " by " + initiator + ".\n\nThank you. Please drive through.";
            _sender.sendEmail(MailSender.By.COMPUTER, ServerConfig.getAgentsAddress(),
                              ServerConfig.getFromAddress(), "Whirled Reboot Scheduled", body);
        }

        protected void broadcast (String message) {
            _memberMan.notifyAll(new GenericNotification(message, Notification.SYSTEM));
        }

        protected int getDayFrequency () {
            // reboot dev nightly, production never (for now)
            return DeploymentConfig.devDeployment ? 1 : -1;
        }

        protected int getRebootHour () {
            // reboot dev at 1am, production (if we ever autoreboot, at 9am)
            return DeploymentConfig.devDeployment ? 1 : 9;
        }

        protected boolean getSkipWeekends () {
            return false;
        }

        protected String getCustomRebootMessage () {
            // for now we don't have auto-reboots, so let's not claim every hand scheduled reboot
            // is a "regularly scheduled reboot"
            return MessageBundle.taint(_runtime.server.customRebootMsg);
        }

        @Inject protected MailSender _sender;
        @Inject protected MemberManager _memberMan;
        @Inject protected MsoyPeerManager _peerMan;
        @Inject protected RuntimeConfig _runtime;
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
                if (memobj.isViewer()) {
                    viewers++;
                } else if (memobj.isPermaguest()) {
                    guests++;
                }
            }
            _eventLog.currentMemberStats(ServerConfig.nodeName, total, active, guests, viewers);

            // now log the current money exchange rate
            _eventLog.moneyExchangeRate(ServerConfig.nodeName, _exchange.getRate());
        }
    }

    /** Logs a snapshot of the running server every 10 minutes. */
    protected SnapshotLogger _snapshotLogger;

    /** A mapping of registered stat collectors. */
    protected Map<StatsModel.Type, StatCollector> _collectors = Maps.newHashMap();

    @Inject protected Lifecycle _lifecycle;
    @Inject protected MemberLocator _locator;
    @Inject protected MoneyExchange _exchange;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MsoyRebootManager _rebmgr;
    @Inject protected PersistenceContext _perCtx;
    @Inject protected PresentsPulseManager _pulseMan;
    // @Inject protected RPCProfiler _rpcProfiler;
    @Inject protected RootDObjectManager _omgr;

    /** 10 minute delay between logged snapshots, in milliseconds. */
    protected static final long STATS_DELAY = 1000 * 60 * 10;
}
