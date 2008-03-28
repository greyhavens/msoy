//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.net.URL;

import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.server.MsoyBaseServer;
import com.threerings.msoy.server.MsoyEvents.MsoyEvent;

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

    public void userLoggedOut (int memberId, String sessionToken, int activeSeconds, int idleSeconds)
    {
        post(new MsoyEvents.Logout(memberId, sessionToken, activeSeconds, idleSeconds));
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
