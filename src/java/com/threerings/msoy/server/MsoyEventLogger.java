//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.threerings.panopticon.client.EventLogger;
import com.threerings.panopticon.client.EventLoggerConfig;
import com.threerings.panopticon.client.EventLoggerFactory;

import com.threerings.msoy.money.data.all.Currency;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlayerMetrics;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.PanopticonStatus;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MsoyEvents.Experience.Type;
import com.threerings.msoy.server.MsoyEvents.MsoyEvent;

import static com.threerings.msoy.Log.log;

/**
 * Provides a concise, type-safe logging interface to Whirled services. Logging functions are
 * thread-safe. All logging requests will be serialized and delivered to the logging server on a
 * separate thread.
 */
@Singleton
public class MsoyEventLogger
{
    /**
     * ID used in cases when the real member ID is not known (eg. guest viewing GWT pages).
     * This isn't really a meaningful number, but it passes the test of:
     * <code>MemberName.isGuest(UNKNOWN_MEMBER_ID) &&
     * !MemberName.isVisitor(UNKNOWN_MEMBER_ID)</code>
     */
    public static final int UNKNOWN_MEMBER_ID = -1;

    /**
     * Initializes the logger; this must happen before any events can be logged.
     */
    public void init (String ident)
    {
        // log locally (always for now)
        File logloc = new File(new File(ServerConfig.serverRoot, "log"), "events_" + ident
            + ".log");
        log.info("Events logged locally to: " + logloc);
        _local = new LocalEventLogger(logloc);
        _local.start();

        // do we want local debug?
        _debugDisplayEnabled = ServerConfig.eventLogDebugDisplay;

        // if we're configure to log to panopticon, do so
        String host = ServerConfig.eventLogHostname;
        int port = ServerConfig.eventLogPort;
        if (!StringUtil.isBlank(host) && port > 0) {
            try {
                final String eventstore = "eventspool_" + ident;
                EventLoggerConfig config = new EventLoggerConfig(host, port,
                    ServerConfig.eventLogUsername, ServerConfig.eventLogPassword, eventstore);

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

    public void currentMemberStats (String serverName, int total, int active, int guests,
        int viewers)
    {
        post(new MsoyEvents.CurrentMemberStats(serverName, total, active, guests, viewers));
    }

    public void moneyTransaction (UserAction action, Currency currency, int amountDelta)
    {
        post(new MsoyEvents.FlowTransaction(
            action.memberId, action.type.getNumber(), currency, amountDelta));
    }

    public void moneyExchangeRate (String serverName, float rate)
    {
        post(new MsoyEvents.ExchangeRate(serverName, rate));
    }

    public void itemPurchased (int memberId, byte itemType, int itemId, Currency currency,
        int amountPaid)
    {
        post(new MsoyEvents.ItemPurchase(memberId, itemType, itemId, currency, amountPaid));
    }

    public void itemUploaded (int creatorId, String tracker)
    {
        post(new MsoyEvents.Experience(Type.ITEM_UPLOADED, creatorId, tracker));
    }

    public void itemListedInCatalog (int creatorId, String tracker, byte itemType, int itemId,
        Currency currency, int cost, int pricing, int salesTarget)
    {
        int flowCost = (currency == Currency.COINS) ? cost : 0;
        int goldCost = (currency == Currency.BARS) ? cost : 0;
        post(new MsoyEvents.ItemCatalogListing(creatorId, itemType, itemId, flowCost, goldCost,
            pricing, salesTarget));
        post(new MsoyEvents.Experience(Type.ITEM_LISTED, creatorId, tracker));
    }

    public void userLoggedIn (int memberId, String tracker, boolean firstLogin, long createdOn)
    {
        if (!MemberName.isViewer(memberId)) {
            post(new MsoyEvents.Experience(Type.ACCOUNT_LOGIN, memberId, tracker));
        }
        post(new MsoyEvents.Login(memberId, firstLogin, createdOn, tracker));
    }

    public void logPlayerMetrics (MemberObject member, String sessionToken)
    {
        MemberLocal local = member.getLocal(MemberLocal.class);
        PlayerMetrics.RoomVisit room = local.metrics.room;
        PlayerMetrics.Idle idle = local.metrics.idle;
        post(new MsoyEvents.SessionMetrics(member.getMemberId(), room.timeInMyRoom,
            room.timeInFriendRooms, room.timeInStrangerRooms, room.timeInWhirleds,
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

    public void roomEntered (int memberId, boolean isWhirled, String tracker)
    {
        if (!MemberName.isViewer(memberId)) {
            Type type = isWhirled ? Type.VISIT_WHIRLED : Type.VISIT_ROOM;
            post(new MsoyEvents.Experience(type, memberId, tracker));
        }
    }

    public void roomLeft (int playerId, int sceneId, boolean isWhirled, int secondsInRoom,
        int occupantsLeft, String tracker)
    {
        post(new MsoyEvents.RoomExit(playerId, sceneId, isWhirled, secondsInRoom, occupantsLeft,
            tracker));
    }

    public void avrgEntered (int playerId, String tracker)
    {
        post(new MsoyEvents.Experience(Type.GAME_AVRG, playerId, tracker));
    }

    public void avrgLeft (int playerId, int gameId, int seconds, int playersLeft, String tracker)
    {
        post(new MsoyEvents.AVRGExit(playerId, gameId, seconds, playersLeft, tracker));
    }

    public void gameEntered (int playerId, boolean multiplayer, String tracker)
    {
        Type type = multiplayer ? Type.GAME_MULTIPLAYER : Type.GAME_SINGLEPLAYER;
        post(new MsoyEvents.Experience(type, playerId, tracker));
    }

    public void gameLeft (int playerId, byte gameGenre, int gameId, int seconds,
        boolean multiplayer, String tracker)
    {
        post(new MsoyEvents.GameExit(playerId, gameGenre, gameId, seconds, multiplayer, tracker));
    }

    public void gamePlayed (int gameGenre, int gameId, int itemId, int payout, int secondsPlayed,
        int playerId)
    {
        post(new MsoyEvents.GamePlayed(gameGenre, gameId, itemId, payout, secondsPlayed, playerId));
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

    public void visitorInfoCreated (VisitorInfo info, boolean fromWeb)
    {
        if (info != null) {
            post(new MsoyEvents.VisitorInfoCreated(info, fromWeb));
        }
    }

    public void vectorAssociated (VisitorInfo info, String vector)
    {
        if (info != null && vector != null) {
            post(new MsoyEvents.VectorAssociated(info, vector));
            log.info(String.format("WRLD-465 stage 3 [? %s]", vector));
        } else {
            log.warning("Unexpected null VisitorInfo for vector: " + vector);
        }
    }

    public void referrerAssociated (VisitorInfo info, String referrer)
    {
        if (info != null) {
            post(new MsoyEvents.HttpReferrerAssociated(info, referrer));
        } else {
            log.warning("Unexpected null VisitorInfo for vector: " + referrer);
        }
    }

    public void webSessionStatusChanged (VisitorInfo info, boolean guest, boolean newInfo)
    {
        if (info == null) {
            log.warning("Got null VisitorInfo during web status change", "guest", guest);
        } else if (info.id == null) {
            log.warning("Got null VisitorInfo.id during web status change", "guest", guest);
        } else {
            boolean player = info.isAuthoritative;
            boolean other = player ? false : !guest;
            post(new MsoyEvents.WebSessionStatusChanged(info.id, player, guest, other, newInfo));
        }
    }

    public void accountCreated (int newMemberId, String inviteId, int affiliateId, String tracker)
    {
        post(new MsoyEvents.Experience(Type.ACCOUNT_CREATED, newMemberId, tracker));
        post(new MsoyEvents.AccountCreated(newMemberId, inviteId, affiliateId, tracker));
    }

    public void roomUpdated (int memberId, int sceneId, String tracker)
    {
        post(new MsoyEvents.Experience(Type.EDIT_ROOM, memberId, tracker));
        post(new MsoyEvents.RoomUpdated(memberId, sceneId));
    }

    public void profileUpdated (int memberId, String tracker)
    {
        post(new MsoyEvents.Experience(Type.EDIT_PROFILE, memberId, tracker));
        post(new MsoyEvents.ProfileUpdated(memberId));
    }

    public void forumMessageRead (int memberId, String tracker)
    {
        post(new MsoyEvents.Experience(Type.FORUMS_READ, memberId, tracker));
    }

    public void forumMessagePosted (int memberId, String tracker, int threadId, int postCount)
    {
        post(new MsoyEvents.Experience(Type.FORUMS_POSTED, memberId, tracker));
        post(new MsoyEvents.ForumMessagePosted(memberId, threadId, postCount));
    }

    public void shopPurchase (int memberId, String tracker)
    {
        post(new MsoyEvents.Experience(Type.SHOP_PURCHASED, memberId, tracker));
    }

    public void shopPageBrowsed (int memberId, String tracker)
    {
        post(new MsoyEvents.Experience(Type.SHOP_BROWSED, memberId, tracker));
    }

    public void shopDetailsViewed (int memberId, String tracker)
    {
        post(new MsoyEvents.Experience(Type.SHOP_DETAILS, memberId, tracker));
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
     * Generic action such as clicking a particular button, viewing an a/b test or landing on a
     * page during an a/b test. Used for short term testing.
     * @param tracker Assigned to every visitor who lands in GWT or Flash
     * @param actionName Identifier such as "LostPasswordButtonClicked"
     * @param testName Optionally record the name of a related a/b test group
     * @param abTestGroup The visitor's a/b group if this is part of an a/b test, or < 0.
     */
    public void testAction (String tracker, String actionName, String testName, int abTestGroup)
    {
        post(new MsoyEvents.TestAction(tracker, actionName, testName, abTestGroup));
    }
    
    /**
     * Gets a snapshot of the current Panopticon status.
     */
    public PanopticonStatus getStatus ()
    {
        PanopticonStatus status = new PanopticonStatus();
        status.currentlyQueued = _remote.getStats().getCurrentlyQueued();
        status.totalQueued = _remote.getStats().getTotalQueued();
        status.dropped = _remote.getStats().getDropped();
        status.totalSent = _remote.getStats().getTotalSent();
        status.overflowed = _remote.getStats().getOverflowedCount();
        status.lastTimeQueued = _remote.getStats().getLastTimeQueued();
        status.lastTimeSent = _remote.getStats().getLastTimeSent();
        status.lastTimeDropped = _remote.getStats().getLastTimeDropped();
        status.lastTimeOverflowed = _remote.getStats().getLastTimeOverflowed();
        status.lastTimeQueueOverflowed = _remote.getStats().getLastTimeQueueOverflowed();
        status.timeStarted = _remote.getStats().getTimeStarted();
        status.lastTimeEnteredRetryMode = _remote.getStats().getLastTimeEnteredRetryMode();
        status.lastTimeRecoveredFromRetryMode = _remote.getStats().getLastTimeRecoveredFromRetryMode();
        status.lastTimeTempFailed = _remote.getStats().getLastTimeTempFailed();
        if (_remote.getStats().getLastTempFailure() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            _remote.getStats().getLastTempFailure().printStackTrace(pw);
            status.lastTempFailureInfo = sw.toString();
        }
        if (_remote.getStats().getLastPermFailure() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            _remote.getStats().getLastPermFailure().printStackTrace(pw);
            status.lastPermFailureInfo = sw.toString();
        }
        
        status.inRetryMode = _remote.isInRetryMode();
        status.disposed = _remote.isDisposed();
        status.connectedToServer = _remote.isConnectedToServer();
        status.senderDisposed = _remote.isSenderDisposed();
        status.persistenceManagerDisposed = _remote.isPersistenceManagerDisposed();
        
        return status;
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
        StringBuffer sb = new StringBuffer("MsoyEventLogger posting event:\n  ");
        sb.append(message.getClass().getSimpleName());
        sb.append("\n  [ ");

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

        log.info(sb.toString());
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
