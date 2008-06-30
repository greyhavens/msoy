//
// $Id$

package com.threerings.msoy.server;

import java.io.File;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.threerings.presents.server.ShutdownManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlayerMetrics;
import com.threerings.msoy.data.UserActionDetails;
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
@Singleton
public class MsoyEventLogger
    implements ShutdownManager.Shutdowner
{
    @Inject public MsoyEventLogger (ShutdownManager shutmgr)
    {
        shutmgr.registerShutdowner(this);
    }

    /** Initializes the logger; this must happen before any events can be logged. */
    public void init (String ident)
    {
        // log locally (always for now)
        File logloc = new File(new File(ServerConfig.serverRoot, "log"), "events_" + ident + ".log");
        log.info("Events logged locally to: " + logloc);
        _local = new LocalEventLogger(logloc);
        _local.start();

        // if we're configure to log to panopticon, do so
        String host = ServerConfig.eventLogHostname;
        int port = ServerConfig.eventLogPort;
        if (!StringUtil.isBlank(host) && port > 0) {
            try {
                EventLoggerConfig config = new EventLoggerConfig(
                    host, port, ServerConfig.eventLogUsername, ServerConfig.eventLogPassword);

                // if our spool directory is not an absolute path, prefix it with the server root
                File spoolDir = new File(ServerConfig.eventLogSpoolDir);
                if (!spoolDir.getAbsolutePath().equals(ServerConfig.eventLogSpoolDir)) {
                    spoolDir = new File(ServerConfig.serverRoot, ServerConfig.eventLogSpoolDir);
                }
                config.setPersistPath(spoolDir.getAbsolutePath());

                log.info("Events logged remotely to: " + host + ":" + port);
                _remote = EventLoggerFactory.createLogger(config);

            } catch (Exception e) {
                log.warning("Failed to connect to remote logging server.", e);
            }
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

    public void avrgLeft (
        int playerId, int gameId, int seconds, int playersLeft) 
    {
        post(new MsoyEvents.AVRGExit(playerId, gameId, seconds, playersLeft));
    }
        
    public void gameLeft (
        int playerId, byte gameGenre, int gameId, int seconds, boolean multiplayer) 
    {
        post(new MsoyEvents.GameExit(playerId, gameGenre, gameId, seconds, multiplayer));
    }
    
    public void gamePlayed (
        int gameGenre, int gameId, int itemId, int payout, int secondsPlayed, int playerId) 
    {
        post(new MsoyEvents.GamePlayed(
            gameGenre, gameId, itemId, payout, secondsPlayed, playerId));
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
