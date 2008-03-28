//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.logging.Level;

import com.threerings.panopticon.common.Event;

import com.threerings.msoy.data.UserActionDetails;
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
    public MsoyEventLogger (String ident, URL serverURL)
    {
        MsoyBaseServer.registerShutdowner(this);

        /* LoggingConnection references commented out, while we migrate to the new
           Panopticon connection protocol. They're not being used right now anyway.
           (RZ 3/28/08)
          
        if (serverURL != null) {
            log.info("Events will be logged to '" + serverURL + "'.");
            _nlogger = new LoggingConnection(
                new InetSocketAddress(serverURL.getHost(), serverURL.getPort()));
            _nlogger.start();

        } else
        */
        {
            File logloc = new File(
                new File(ServerConfig.serverRoot, "log"), "events_" + ident + ".log");
            log.info("Events will be logged locally to '" + logloc + "'.");
            _llogger = new LocalEventLogger(logloc);
            _llogger.start();
        }
    }

    // from interface MsoyBaseServer.Shutdowner
    public void shutdown ()
    {
        /*
        if (_nlogger != null) {
            _nlogger.shutdown();
        }
        */
        if (_llogger != null) {
            _llogger.shutdown();
        }
    }

    public void currentMemberStats (
        String serverName, int total, int active, int guests, int viewers)
    {
        MsoyEvents.CurrentMemberStats message = new MsoyEvents.CurrentMemberStats();
        message.serverName = serverName;
        message.total = total;
        message.active = active;
        message.guests = guests;
        message.viewers = viewers;
        post(message);
    }

    public void flowTransaction (UserActionDetails info, int deltaFlow, int newTotal)
    {
        MsoyEvents.FlowTransaction message = new MsoyEvents.FlowTransaction();
        message.memberId = info.memberId;
        message.actionType = info.action.getNumber();
        message.itemType = info.itemType;
        message.itemId = info.itemId;
        message.deltaFlow = deltaFlow;
        message.newtotal = newTotal;
        post(message);
    }

    public void itemPurchased (int memberId, byte itemType, int itemId, int flowCost, int goldCost)
    {
        MsoyEvents.ItemPurchase message = new MsoyEvents.ItemPurchase();
        message.memberId = memberId;
        message.itemType = itemType;
        message.itemId = itemId;
        message.flowCost = flowCost;
        message.goldCost = goldCost;
        post(message);
    }

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

    public void userLoggedIn (int memberId, boolean firstLogin, long createdOn, String sessionToken)
    {
        MsoyEvents.Login message = new MsoyEvents.Login();
        message.memberId = memberId;
        message.firstLogin = firstLogin;
        message.sessionToken = sessionToken;
        message.createdOn = createdOn;
        post(message);
    }

    public void userLoggedOut (int memberId, String sessionToken, int activeSeconds, int idleSeconds)
    {
        MsoyEvents.Logout message = new MsoyEvents.Logout();
        message.memberId = memberId;
        message.sessionToken = sessionToken;
        message.activeSeconds = activeSeconds;
        message.idleSeconds = idleSeconds;
        post(message);
    }

    public void mailSent (int senderId, int recipientId, int payloadType)
    {
        MsoyEvents.MailSent message = new MsoyEvents.MailSent();
        message.senderId = senderId;
        message.recipientId = recipientId;
        message.payloadType = payloadType;
        post(message);
    }

    public void friendAdded (int memberId, int friendId)
    {
        MsoyEvents.FriendshipAction message = new MsoyEvents.FriendshipAction();
        message.memberId = memberId;
        message.friendId = friendId;
        message.isAdded = true;
        post(message);
    }

    public void friendRemoved (int memberId, int friendId)
    {
        MsoyEvents.FriendshipAction message = new MsoyEvents.FriendshipAction();
        message.memberId = memberId;
        message.friendId = friendId;
        message.isAdded = false;
        post(message);
    }

    public void groupJoined (int memberId, int groupId)
    {
        MsoyEvents.GroupMembershipAction message = new MsoyEvents.GroupMembershipAction();
        message.memberId = memberId;
        message.groupId = groupId;
        message.isJoined = true;
        post(message);
    }

    public void groupLeft (int memberId, int groupId)
    {
        MsoyEvents.GroupMembershipAction message = new MsoyEvents.GroupMembershipAction();
        message.memberId = memberId;
        message.groupId = groupId;
        message.isJoined = false;
        post(message);
    }

    public void groupRankChange (int memberId, int groupId, byte newRank)
    {
        MsoyEvents.GroupRankModification message = new MsoyEvents.GroupRankModification();
        message.memberId = memberId;
        message.groupId = groupId;
        message.newRank = newRank;
        post(message);
    }

    public void gamePlayed (int gameGenre, int gameId, int itemId, int payout, int secondsPlayed) 
    {
        MsoyEvents.GamePlayed message = new MsoyEvents.GamePlayed();
        message.gameGenre = gameGenre;
        message.gameId = gameId;
        message.itemId = itemId;
        message.payout = payout;
        message.secondsPlayed = secondsPlayed;
        post(message);
    }
    
    public void trophyEarned (int recipientId, int gameId, String trophyIdent)
    {
        MsoyEvents.TrophyEarned message = new MsoyEvents.TrophyEarned();
        message.recipientId = recipientId;
        message.gameId = gameId;
        message.trophyIdent = trophyIdent;
        post(message);
    }

    public void prizeEarned (int recipientId, int gameId, String prizeIdent, byte prizeItemType)
    {
        MsoyEvents.PrizeEarned message = new MsoyEvents.PrizeEarned();
        message.recipientId = recipientId;
        message.gameId = gameId;
        message.prizeIdent = prizeIdent;
        message.prizeItemType = prizeItemType;
        post(message);
    }

    public void inviteSent (String inviteId, int inviterId, String recipient)
    {
        MsoyEvents.InviteSent message = new MsoyEvents.InviteSent();
        message.inviteId = inviteId;
        message.inviterId = inviterId;
        message.recipient = recipient;
        post(message);
    }

    public void inviteViewed (String inviteId)
    {
        MsoyEvents.InviteViewed message = new MsoyEvents.InviteViewed();
        message.inviteId = inviteId;
        post(message);
    }

    public void accountCreated (int newMemberId, String inviteId)
    {
        MsoyEvents.AccountCreated message = new MsoyEvents.AccountCreated();
        message.newMemberId = newMemberId;
        message.inviteId = inviteId;
        post(message);
    }

    public void roomUpdated (int memberId, int sceneId)
    {
        MsoyEvents.RoomUpdated message = new MsoyEvents.RoomUpdated();
        message.memberId = memberId;
        message.sceneId = sceneId;
        post(message);
    }

    public void profileUpdated (int memberId)
    {
        MsoyEvents.ProfileUpdated message = new MsoyEvents.ProfileUpdated();
        message.memberId = memberId;
        post(message);
    }

    /** Posts a log message to the appropriate place. */
    protected void post (Event message)
    {
        /*
        if (_nlogger != null) {
            try {
                _nlogger.send(message);
            } catch (Exception e) {
                // TODO: throttle these errors
                log.log(Level.WARNING, "Failed to send log event " + message + ".", e);
            }

        } else
        */
        if (_llogger != null) {
            _llogger.log(message);
        } else {
            log.warning("No logger configured! Dropping " + message + ".");
        }
    }

    /** The connection via which we deliver our log messages. */
    // protected LoggingConnection _nlogger;

    /** Used to log events if we have no network logger. */
    protected LocalEventLogger _llogger;
}
