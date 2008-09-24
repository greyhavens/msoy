//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.lang.reflect.Field;

import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlayerMetrics;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.data.all.VisitorInfo;
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
{
    /**
     * Initializes the logger; this must happen before any events can be logged.
     */
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
                final String eventstore = "eventspool_" + ident;
                EventLoggerConfig config =
                    new EventLoggerConfig(host, port, ServerConfig.eventLogUsername,
                                          ServerConfig.eventLogPassword, eventstore);

                // if our spool directory is not an absolute path, prefix it with the server root
                File spoolDir = new File(ServerConfig.eventLogSpoolDir);
                if (!spoolDir.getAbsolutePath().equals(ServerConfig.eventLogSpoolDir)) {
                    spoolDir = new File(ServerConfig.serverRoot, ServerConfig.eventLogSpoolDir);
                }
                config.setPersistPath(spoolDir.getAbsolutePath());

                // do we want local debug?
                _debugDisplayEnabled = ServerConfig.eventLogDebugDisplay;

                log.info("Events logged remotely to: " + host + ":" + port);
                _remote = EventLoggerFactory.createLogger(config);

            } catch (Exception e) {
                log.warning("Failed to connect to remote logging server.", e);
            }
        }
    }

    /**
     * Shuts down our event logger. This is called by MsoyBaseServer after everything else has
     * shutdown so that we can log events during the shutdown process.
     */
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

    public void roomLeft (
        int playerId, int sceneId, boolean isWhirled, int secondsInRoom,
        int occupantsLeft, String tracker)
    {
        post(new MsoyEvents.RoomExit(
            playerId, sceneId, isWhirled, secondsInRoom, occupantsLeft, tracker));
    }

    public void avrgLeft (
        int playerId, int gameId, int seconds, int playersLeft, String tracker)
    {
        post(new MsoyEvents.AVRGExit(playerId, gameId, seconds, playersLeft, tracker));
    }

    public void gameLeft (
        int playerId, byte gameGenre, int gameId, int seconds, boolean multiplayer,
        String tracker)
    {
        post(new MsoyEvents.GameExit(playerId, gameGenre, gameId, seconds, multiplayer, tracker));
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

    // @Deprecated
    public void referralCreated (ReferralInfo info)
    {
        if (info != null) {
            post(new MsoyEvents.ReferralCreated(
                info.affiliate, info.vector, info.creative, info.tracker));
        } else {
            post(new MsoyEvents.ReferralCreated());
        }
    }

    public void referralCreated (VisitorInfo info, String vector)
    {
        if (info != null) {
            post(new MsoyEvents.ReferralCreated("", vector, "", info.id));
        } else {
            log.warning("Unexpected null VisitorInfo for vector: " + vector);
            post(new MsoyEvents.ReferralCreated());
        }
    }

    public void accountCreated (int newMemberId, String inviteId, String tracker)
    {
        post(new MsoyEvents.AccountCreated(newMemberId, inviteId, tracker));
    }

    public void roomUpdated (int memberId, int sceneId)
    {
        post(new MsoyEvents.RoomUpdated(memberId, sceneId));
    }

    public void profileUpdated (int memberId)
    {
        post(new MsoyEvents.ProfileUpdated(memberId));
    }

    /**
     * @param memberId who posted the message.
     * @param threadId the ID for the discussion thread.
     * @param postCount the current total number of posts to the thread.
     */
    public void forumMessagePosted (int memberId, int threadId, int postCount)
    {
    	post(new MsoyEvents.ForumMessagePosted(memberId, threadId, postCount));
    }

    /**
     * Action performed on the client such as clicking a particular button.
     * @param tracker The visitors' tracking ID
     * @param actionName Identifier for the action performed eg "landingPlayButtonClicked"
     * @param details More info on the action, eg the id of the game clicked upon
     */
    public void clientAction (String tracker, String actionName, String details)
    {
        post(new MsoyEvents.ClientAction(tracker, actionName, details));
    }

    /**
     * Generic action such as clicking a particular button, viewing an a/b test or landing
     * on a page during an a/b test.  Used for short term testing.
     * @param tracker Assigned to every visitor who lands in GWT or Flash
     * @param actionName Identifier such as "LostPasswordButtonClicked"
     * @param testName Optionally record the name of a related a/b test group
     * @param abTestGroup The visitor's a/b group if this is part of an a/b test, or < 0.
     */
    public void testAction (String tracker, String actionName, String testName, int abTestGroup)
    {
        post(new MsoyEvents.TestAction(tracker, actionName, testName, abTestGroup));
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

        // display, perhaps
        if (_debugDisplayEnabled) {
            dumpMessage(message);
        }
    }

    /** Dump a message to logs. */
    protected void dumpMessage (MsoyEvent message)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(message.getClass().getSimpleName());
        sb.append(" [ ");

        for (Field field : message.getClass().getFields()) {
            sb.append(field.getName());
            sb.append("=");
            try {
                sb.append(field.get(message).toString());
            } catch (Exception e) {
                // skip this one
            }
            sb.append(" ");
        }
        sb.append("]");

        log.info("MsoyEventLogger posting event: " + sb.toString());
    }

    /** Should we display debug info about what's being logged? */
    protected boolean _debugDisplayEnabled;

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
