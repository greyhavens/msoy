//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.logging.Level;

import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.panopticon.client.net.LoggingConnection;
import com.threerings.panopticon.common.hessian.Event;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyBaseServer;

import static com.threerings.msoy.Log.log;

/**
 * Provides a concise, type-safe logging interface to Whirled services.  Logging functions are
 * thread-safe. All logging requests will be serialized and delivered to the logging server on a
 * separate thread.
 */
public class MsoyEventLogger
    implements MsoyBaseServer.Shutdowner
{
    /** Initializes the logger; this must happen before any events can be logged. */
    public MsoyEventLogger (final URL serverURL)
    {
        MsoyBaseServer.registerShutdowner(this);

        if (serverURL != null) {
            log.info("Events will be logged to '" + serverURL + "'.");
            _nlogger = new LoggingConnection(
                new InetSocketAddress(serverURL.getHost(), serverURL.getPort()));
            _nlogger.start();

        } else {
            File logloc = new File(new File(ServerConfig.serverRoot, "log"), "events.log");
            log.info("Events will be logged locally to '" + logloc + "'.");
            _llogger = new LocalEventLogger(logloc);
            _llogger.start();
        }
    }

    // from interface MsoyBaseServer.Shutdowner
    public void shutdown ()
    {
        if (_nlogger != null) {
            _nlogger.shutdown();
        }
        if (_llogger != null) {
            _llogger.shutdown();
        }
    }

    /** Event: periodic system snapshot of player counts. */
    public void currentPlayerStats (String serverName, int total, int active, int guests)
    {
        MsoyEvents.CurrentPlayerStats message = new MsoyEvents.CurrentPlayerStats();
        message.serverName = serverName;
        message.total = total;
        message.active = active;
        message.guests = guests;
        post(message);
    }

    /** Event: generic flow transaction. */
    public void flowTransaction (int playerId, int actionType, int deltaFlow, int newTotal,
                                 String details)
    {
        MsoyEvents.FlowTransaction message = new MsoyEvents.FlowTransaction();
        message.playerId = playerId;
        message.actionType = actionType;
        message.deltaFlow = deltaFlow;
        message.details = details;
        post(message);
    }

    /** Event: item purchase. */
    public void itemPurchased (int playerId, byte itemType, int itemId, int flowCost, int goldCost)
    {
        MsoyEvents.ItemPurchase message = new MsoyEvents.ItemPurchase();
        message.playerId = playerId;
        message.itemType = itemType;
        message.itemId = itemId;
        message.flowCost = flowCost;
        message.goldCost = goldCost;
        post(message);
    }

    /** Event: item purchase. */
    public void itemListedInCatalog (int creatorId, byte itemType, int itemId, int flowCost,
                                     int goldCost, int pricing, int salesTarget)
    {
        MsoyEvents.ItemCatalogListing message = new MsoyEvents.ItemCatalogListing();
        message.creatorId = creatorId;
        message.itemType = itemType;
        message.itemId = itemId;
        message.flowCost = flowCost;
        message.goldCost = goldCost;
        message.pricing = pricing;
        message.salesTarget = salesTarget;
        post(message);
    }

    /** Event: registered user authenticated with the specified cookie. */
    public void userAuthenticated (int playerId, boolean firstLogin, String sessionToken)
    {
        MsoyEvents.Login message = new MsoyEvents.Login();
        message.playerId = playerId;
        message.firstLogin = firstLogin;
        message.sessionToken = sessionToken;
        post(message);
    }

    /** Event: guest user logged in with the specified cookie. */
    public void userAuthenticated (String sessionToken)
    {
        MsoyEvents.Login message = new MsoyEvents.Login();
        message.playerId = MemberName.GUEST_ID;
        message.firstLogin = false;
        message.sessionToken = sessionToken;
        post(message);
    }

    /** Event: sent mail from one user to another. */
    public void mailSent (int senderId, int recipientId, int payloadType)
    {
        MsoyEvents.MailSent message = new MsoyEvents.MailSent();
        message.senderId = senderId;
        message.recipientId = recipientId;
        message.payloadType = payloadType;
        post(message);
    }

    /** Event: added a friend. */
    public void friendAdded (int playerId, int friendId)
    {
        MsoyEvents.FriendshipAction message = new MsoyEvents.FriendshipAction();
        message.playerId = playerId;
        message.friendId = friendId;
        message.isAdded = true;
        post(message);
    }

    /** Event: removed a friend. */
    public void friendRemoved (int playerId, int friendId)
    {
        MsoyEvents.FriendshipAction message = new MsoyEvents.FriendshipAction();
        message.playerId = playerId;
        message.friendId = friendId;
        message.isAdded = false;
        post(message);
    }

    /** Event: joined a group. */
    public void groupJoined (int playerId, int groupId)
    {
        MsoyEvents.GroupMembershipAction message = new MsoyEvents.GroupMembershipAction();
        message.playerId = playerId;
        message.groupId = groupId;
        message.isJoined = true;
        post(message);
    }

    /** Event: joined a group. */
    public void groupLeft (int playerId, int groupId)
    {
        MsoyEvents.GroupMembershipAction message = new MsoyEvents.GroupMembershipAction();
        message.playerId = playerId;
        message.groupId = groupId;
        message.isJoined = false;
        post(message);
    }

    /** Event: promotion/demotion in a group. */
    public void groupRankChange (int playerId, int groupId, byte newRank)
    {
        MsoyEvents.GroupRankModification message = new MsoyEvents.GroupRankModification();
        message.playerId = playerId;
        message.groupId = groupId;
        message.newRank = newRank;
        post(message);
    }

    /** Posts a log message to the appropriate place. */
    protected void post (Event message)
    {
        if (_nlogger != null) {
            try {
                _nlogger.send(message);
            } catch (Exception e) {
                // TODO: throttle these errors
                log.log(Level.WARNING, "Failed to send log event " + message + ".", e);
            }

        } else if (_llogger != null) {
            _llogger.log(message);

        } else {
            log.warning("No logger configured! Dropping " + message + ".");
        }
    }

    /** The connection via which we deliver our log messages. */
    protected LoggingConnection _nlogger;

    /** Used to log events if we have no network logger. */
    protected LocalEventLogger _llogger;
}
