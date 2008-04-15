//
// $Id$

package com.threerings.msoy.server;

import java.io.File;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlayerMetrics;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.server.MsoyBaseServer;
import com.threerings.msoy.server.MsoyEvents.MsoyEvent;

import com.threerings.panopticon.client.net.EventLogger;
import com.threerings.panopticon.client.net.EventLoggerConfig;
import com.threerings.panopticon.client.net.EventLoggerFactory;

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
    public MsoyEventLogger (String ident, String host, int port, String username, String password)
    {
        MsoyBaseServer.registerShutdowner(this);

        // log locally (always for now)
        File logloc = new File(
                new File(ServerConfig.serverRoot, "log"), "events_" + ident + ".log");
        log.info("Events logged locally to: " + logloc);
        _local = new LocalEventLogger(logloc);
        _local.start();

        // also, depending on server properties, log remotely
        try {
            if (host != null && host.length() > 0 && port > 0) {
                log.info("Events logged remotely to: " + host + ":" + port);
                
                EventLoggerConfig config = new EventLoggerConfig(host, port, username, password);
                config.setPersistPath(ServerConfig.serverRoot.getAbsolutePath());
                _remote = EventLoggerFactory.createLogger(config);
            } 
        } catch (Exception e) {
            log.severe("Failed to connect to remote logging server, will only log locally. " +
            		"[host=" + host + ", port=" + port + ", exception=" + e + "]"); 
        }
    }

    // from interface MsoyBaseServer.Shutdowner
    public void shutdown ()
    {
        if (_remote != null) {
            _remote.dispose();
        }
        if (_local != null) {
            _local.shutdown();
        }
    }

    public void currentMemberStats (
        String serverName, int total, int active, int guests, int viewers)
    {
        post(new MsoyEvents.CurrentMemberStats(serverName, total, active, guests, viewers));
    }

    public void flowTransaction (UserActionDetails info, int deltaFlow, int newTotal)
    {
        post(new MsoyEvents.FlowTransaction(info.memberId, info.action.getNumber(), info.itemId, 
                                            info.itemType, deltaFlow, newTotal));
    }

    public void itemPurchased (int memberId, byte itemType, int itemId, int flowCost, int goldCost)
    {
        post(new MsoyEvents.ItemPurchase(memberId, itemType, itemId, flowCost, goldCost));
    }

    public void itemListedInCatalog (int creatorId, byte itemType, int itemId, int flowCost,
                                     int goldCost, int pricing, int salesTarget)
    {
        post(new MsoyEvents.ItemCatalogListing(
            creatorId, itemType, itemId, flowCost, goldCost, pricing, salesTarget));
    }

    public void userLoggedIn (int memberId, boolean firstLogin, long createdOn, String sessionToken)
    {
        post(new MsoyEvents.Login(memberId, firstLogin, sessionToken, createdOn));
    }

    public void logPlayerMetrics (MemberObject member, String sessionToken)
    {
        PlayerMetrics.RoomVisit room = member.metrics.room;
        PlayerMetrics.Idle idle = member.metrics.idle;
        post(new MsoyEvents.SessionMetrics(
                 member.getMemberId(), room.timeInMyRoom, room.timeInFriendRooms,
                 room.timeInStrangerRooms, room.timeInWhirleds,
                 idle.timeActive, idle.timeIdle, sessionToken));
    }

    public void mailSent (int senderId, int recipientId, int payloadType)
    {
        post(new MsoyEvents.MailSent(senderId, recipientId, payloadType));
    }

    public void friendAdded (int memberId, int friendId)
    {
        post(new MsoyEvents.FriendshipAction(memberId, friendId, true));
    }

    public void friendRemoved (int memberId, int friendId)
    {
        post(new MsoyEvents.FriendshipAction(memberId, friendId, false));
    }

    public void groupJoined (int memberId, int groupId)
    {
        post(new MsoyEvents.GroupMembershipAction(memberId, groupId, true));
    }

    public void groupLeft (int memberId, int groupId)
    {
        post(new MsoyEvents.GroupMembershipAction(memberId, groupId, false));
    }

    public void groupRankChange (int memberId, int groupId, byte newRank)
    {
        post(new MsoyEvents.GroupRankModification(memberId, groupId, newRank));
    }

    public void gamePlayed (int gameGenre, int gameId, int itemId, int payout, int secondsPlayed) 
    {
        post(new MsoyEvents.GamePlayed(gameGenre, gameId, itemId, payout, secondsPlayed));
    }
    
    public void trophyEarned (int recipientId, int gameId, String trophyIdent)
    {
        post(new MsoyEvents.TrophyEarned(recipientId, gameId, trophyIdent));
    }

    public void prizeEarned (int recipientId, int gameId, String prizeIdent, byte prizeItemType)
    {
        post(new MsoyEvents.PrizeEarned(recipientId, gameId, prizeIdent, prizeItemType));
    }

    public void inviteSent (String inviteId, int inviterId, String recipient)
    {
        post(new MsoyEvents.InviteSent(inviteId, inviterId, recipient));
    }

    public void inviteViewed (String inviteId)
    {
        post(new MsoyEvents.InviteViewed(inviteId));
    }

    public void accountCreated (int newMemberId, String inviteId)
    {
        post(new MsoyEvents.AccountCreated(newMemberId, inviteId));
    }

    public void roomUpdated (int memberId, int sceneId)
    {
        post(new MsoyEvents.RoomUpdated(memberId, sceneId));
    }

    public void profileUpdated (int memberId)
    {
        post(new MsoyEvents.ProfileUpdated(memberId));
    }

    /** Posts a log message to the appropriate place. */
    protected void post (MsoyEvent message)
    {
        // log locally
        _local.log(message);

        // log remotely (if applicable)
        if (_remote != null) {
            _remote.log(message);
        } 
    }

    /** The connection via which we deliver our log messages. */
    protected EventLogger _remote;

    /** Used to log events to the local filesystem. */
    protected LocalEventLogger _local;
    
    /** Timeout value when connecting to the Panopticon server, in milliseconds. */
    protected static final int TIMEOUT = 1000;
    
    /** Queue size for the remote connection. */
    protected static final int QUEUE_SIZE = 100;
    
    /** Remote connection retry interval, in milliseconds. */
    protected static final int RETRY = 1000;
}
