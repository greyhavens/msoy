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

import com.samskivert.depot.PersistenceContext;
import com.samskivert.util.Interval;
import com.samskivert.util.Tuple;

import net.sf.ehcache.CacheManager;

import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.crowd.data.OccupantInfo;
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
import com.threerings.presents.server.RebootManager;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.web.server.RPCProfiler;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.money.server.MoneyExchange;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.admin.client.PeerAdminService;
import com.threerings.msoy.admin.data.ServerConfigObject;
import com.threerings.msoy.admin.gwt.StatsModel;

import static com.threerings.msoy.Log.log;

/**
 * Handles administrative bits for the MetaSOY server.
 */
@EventThread @Singleton
public class MsoyAdminManager
    implements PeerAdminProvider
{
    /**
     * Prepares the admin manager for operation.
     */
    public void init (InvocationManager invmgr, CacheManager cacheMgr)
    {
        // create our reboot manager
        _rebmgr = new MsoyRebootManager(_shutmgr, _omgr);

        // start up the system "snapshot" logger
        _snapshotLogger = new SnapshotLogger();
        _snapshotLogger.schedule(0, STATS_DELAY);

        // initialize our reboot manager
        _rebmgr.init();

        // register our peer service
        ((MsoyNodeObject)_peerMan.getNodeObject()).setPeerAdminService(
            invmgr.registerDispatcher(new PeerAdminDispatcher(this)));

        // register our stat collectors
        _collectors.put(StatsModel.Type.DEPOT, new DepotStatCollector(_perCtx));
        _collectors.put(StatsModel.Type.DEPOT_QUERIES, new DepotQueriesStatCollector(_perCtx));
        _collectors.put(StatsModel.Type.CACHE, new CacheStatCollector(cacheMgr));
        _collectors.put(StatsModel.Type.RPC, new RPCStatCollector(_rpcProfiler));
    }

    /**
     * Schedules a reboot for the specified number of minutes in the future.
     */
    public void scheduleReboot (int minutes, String initiator)
    {
        if (minutes == 0) {
            // if this is a zero minute reboot, do it in one second so that we have a chance to
            // send our response back to the requester
            log.info("Performing immediate shutdown", "for", initiator);
            new Interval(_omgr) {
                public void expired () {
                    _shutmgr.shutdown();
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
     * Compiles statistics from this and the other peers in this network. The returned result will
     * not be ready until responses have been received from all peers.
     */
    @AnyThread
    public Future<StatsModel> compilePeerStatistics (final StatsModel.Type type)
    {
        final StatCollector.Merger merger = _collectors.get(type).createMerger();
        // first queue up requests from all other servers
        _peerMan.invokeOnNodes(new Function<Tuple<Client, NodeObject>, Void>() {
            public Void apply (Tuple<Client, NodeObject> args) {
                merger.pendingNodes++;
                ((MsoyNodeObject)args.right).peerAdminService.compileStatistics(
                    args.left, type, merger.makeListener(args.right.nodeName));
                return null;
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

    /** A mapping of registered stat collectors. */
    protected Map<StatsModel.Type, StatCollector> _collectors = Maps.newHashMap();

    @Inject protected RuntimeConfig _runtime;
    @Inject protected PersistenceContext _perCtx;
    @Inject protected RPCProfiler _rpcProfiler;
    @Inject protected ShutdownManager _shutmgr;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MailSender _sender;
    @Inject protected MemberLocator _locator;
    @Inject protected ChatProvider _chatprov;
    @Inject protected MoneyExchange _exchange;

    /** 10 minute delay between logged snapshots, in milliseconds. */
    protected static final long STATS_DELAY = 1000 * 60 * 10;

        protected static final String AUTOMATIC_INITIATOR = "automatic";
}
