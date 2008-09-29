//
// $Id$

package com.threerings.msoy.server;

import java.util.Date;

import com.threerings.panopticon.common.event.annotations.Event;
import com.threerings.panopticon.common.event.annotations.Field;
import com.threerings.panopticon.common.event.annotations.Index;

import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.money.data.all.Currency;

/**
 * Logging events generated by the Whirled server.
 */
public class MsoyEvents
{
    public interface MsoyEvent
    {
    }

    @Event(name="CurrentMemberStats")
    public static class CurrentMemberStats implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public String serverName;
        @Field final public int total;
        @Field final public int active;
        @Field final public int guests;
        @Field final public int viewers;

        public CurrentMemberStats (
                String serverName, int total, int active, int guests, int viewers)
        {
            this.timestamp = new Date();
            this.serverName = toValue(serverName);
            this.total = total;
            this.active = active;
            this.guests = guests;
            this.viewers = viewers;
        }
    }

    @Event(name="Login")
    public static class Login implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;
        @Field final public boolean firstLogin;
        @Field final public String sessionToken;
        @Field final public long createdOn;

        public Login (int memberId, boolean firstLogin, String sessionToken, long createdOn)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
            this.firstLogin = firstLogin;
            this.sessionToken = toValue(sessionToken);
            this.createdOn = createdOn;
        }
    }

    @Event(name="SessionMetrics")
    public static class SessionMetrics implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;
        @Field final public int inMyRooms;
        @Field final public int inFriendRooms;
        @Field final public int inStrangerRooms;
        @Field final public int inWhirleds;
        @Field final public int totalActive;
        @Field final public int totalIdle;
        @Field final public String sessionToken;

        public SessionMetrics (int memberId, int timeInMyRooms, int timeInFriendRooms,
            int timeInStrangerRooms, int timeInWhirleds, int totalTimeActive, int totalTimeIdle,
            String sessionToken)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
            this.inMyRooms = timeInMyRooms;
            this.inFriendRooms = timeInFriendRooms;
            this.inStrangerRooms = timeInStrangerRooms;
            this.inWhirleds = timeInWhirleds;
            this.totalActive = totalTimeActive;
            this.totalIdle = totalTimeIdle;
            this.sessionToken = toValue(sessionToken);
        }
    }

    @Event(name="MailSent")
    public static class MailSent implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int senderId;
        @Field final public int recipientId;
        @Field final public int payloadType;

        public MailSent (int senderId, int recipientId, int payloadType)
        {
            this.timestamp = new Date();
            this.senderId = senderId;
            this.recipientId = recipientId;
            this.payloadType = payloadType;
        }
    }

    @Event(name="FlowTransaction")
    public static class FlowTransaction implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;
        @Field final public int actionType;
        @Field final public int deltaFlow;
        @Field final public int newtotal;

        public FlowTransaction (int memberId, int actionType, int deltaFlow, int newtotal)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
            this.actionType = actionType;
            this.deltaFlow = deltaFlow;
            this.newtotal = newtotal;
        }
    }

    @Event(name="ItemPurchase")
    public static class ItemPurchase implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;
        @Field final public byte itemType;
        @Field final public int itemId;
        @Field final public int flowCost;
        @Field final public int goldCost;

        public ItemPurchase (
            int memberId, byte itemType, int itemId, Currency currency, int amountPaid)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
            this.itemType = itemType;
            this.itemId = itemId;
            this.flowCost = (currency == Currency.COINS) ? amountPaid : 0;
            this.goldCost = (currency == Currency.BARS) ? amountPaid : 0;
        }
    }

    @Event(name="ItemCatalogListing")
    public static class ItemCatalogListing implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int creatorId;
        @Field final public byte itemType;
        @Field final public int itemId;
        @Field final public int flowCost;
        @Field final public int goldCost;
        @Field final public int pricing;
        @Field final public int salesTarget;

        public ItemCatalogListing (int creatorId, byte itemType,
            int itemId, int flowCost, int goldCost, int pricing, int salesTarget)
        {
            this.timestamp = new Date();
            this.creatorId = creatorId;
            this.itemType = itemType;
            this.itemId = itemId;
            this.flowCost = flowCost;
            this.goldCost = goldCost;
            this.pricing = pricing;
            this.salesTarget = salesTarget;
        }
    }

    @Event(name="FriendshipAction")
    public static class FriendshipAction implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;
        @Field final public int friendId;
        @Field final public boolean isAdded;

        public FriendshipAction (int memberId, int friendId, boolean isAdded)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
            this.friendId = friendId;
            this.isAdded = isAdded;
        }
    }

    @Event(name="GroupMembershipAction")
    public static class GroupMembershipAction implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;
        @Field final public int groupId;
        @Field final public boolean isJoined;

        public GroupMembershipAction (int memberId, int groupId, boolean isJoined)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
            this.groupId = groupId;
            this.isJoined = isJoined;
        }
    }

    @Event(name="GroupRankModification")
    public static class GroupRankModification implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;
        @Field final public int groupId;
        @Field final public byte newRank;

        public GroupRankModification (int memberId, int groupId, byte newRank)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
            this.groupId = groupId;
            this.newRank = newRank;
        }
    }

    @Event(name="RoomExit")
    public static class RoomExit implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int playerId;
        @Field final public int sceneId;
        @Field final public boolean isWhirled;
        @Field final public int secondsInRoom;
        @Field final public int occupantsLeft;
        @Field final public String tracker;

        public RoomExit (
            int playerId, int sceneId, boolean isWhirled, int secondsInRoom, int occupantsLeft,
            String tracker)
        {
            this.timestamp = new Date();
            this.playerId = playerId;
            this.sceneId = sceneId;
            this.isWhirled = isWhirled;
            this.secondsInRoom = secondsInRoom;
            this.occupantsLeft = occupantsLeft;
            this.tracker = tracker;
        }
    }

    @Event(name="AVRGExit")
    public static class AVRGExit implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Index @Field final public int gameId;
        @Field final public int playerId;
        @Field final public int secondsInGame;
        @Field final public int playersLeft;
        @Field final public String tracker;

        public AVRGExit (int playerId, int gameId, int seconds, int playersLeft, String tracker)
        {
            this.timestamp = new Date();
            this.playerId = playerId;
            this.gameId = gameId;
            this.secondsInGame = seconds;
            this.playersLeft = playersLeft;
            this.tracker = tracker;
        }
    }

    @Event(name="GameExit")
    public static class GameExit implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Index @Field final public int gameId;
        @Field final public byte gameGenre;
        @Field final public int playerId;
        @Field final public int secondsInGame;
        @Field final public boolean multiplayer;
        @Field final public String tracker;

        public GameExit (
            int playerId, byte gameGenre, int gameId, int seconds, boolean multiplayer,
            String tracker)
        {
            this.timestamp = new Date();
            this.playerId = playerId;
            this.gameGenre = gameGenre;
            this.gameId = gameId;
            this.secondsInGame = seconds;
            this.multiplayer = multiplayer;
            this.tracker = toValue(tracker);
        }
    }

    @Event(name="GamePlayed")
    public static class GamePlayed implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int gameGenre;
        @Field final public int gameId;
        @Field final public int itemId;
        @Field final public int payout;
        @Field final public int secondsPlayed;
        @Field final public int playerId;

        public GamePlayed (
            int gameGenre, int gameId, int itemId, int payout, int secondsPlayed, int playerId)
        {
            this.timestamp = new Date();
            this.gameGenre = gameGenre;
            this.gameId = gameId;
            this.itemId = itemId;
            this.payout = payout;
            this.secondsPlayed = secondsPlayed;
            this.playerId = playerId;
        }
    }

    @Event(name="TrophyEarned")
    public static class TrophyEarned implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int recipientId;
        @Field final public int gameId;
        @Field final public String trophyIdent;

        public TrophyEarned (int recipientId, int gameId, String trophyIdent)
        {
            this.timestamp = new Date();
            this.recipientId = recipientId;
            this.gameId = gameId;
            this.trophyIdent = toValue(trophyIdent);
        }
    }

    @Event(name="PrizeEarned")
    public static class PrizeEarned implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int recipientId;
        @Field final public int gameId;
        @Field final public String prizeIdent;
        @Field final public byte prizeItemType;

        public PrizeEarned (int recipientId, int gameId, String prizeIdent, byte prizeItemType)
        {
            this.timestamp = new Date();
            this.recipientId = recipientId;
            this.gameId = gameId;
            this.prizeIdent = toValue(prizeIdent);
            this.prizeItemType = prizeItemType;
        }
    }

    @Event(name="InviteSent")
    public static class InviteSent implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public String inviteId;
        @Field final public int inviterId;
        @Field final public String recipient;

        public InviteSent (String inviteId, int inviterId, String recipient)
        {
            this.timestamp = new Date();
            this.inviteId = toValue(inviteId);
            this.inviterId = inviterId;
            this.recipient = toValue(recipient);
        }
    }

    @Event(name="InviteViewed")
    public static class InviteViewed implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public String inviteId;

        public InviteViewed (String inviteId)
        {
            this.timestamp = new Date();
            this.inviteId = toValue(inviteId);
        }
    }

    // @Deprecated
    @Event(name="ReferralCreated")
    public static class ReferralCreated implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public String affiliate;
        @Field final public String vector;
        @Field final public String creative;
        @Index @Field final public String tracker;

        public ReferralCreated (String affiliate, String vector, String creative, String tracker)
        {
            this.timestamp = new Date();
            this.affiliate = toValue(affiliate);
            this.vector = toValue(vector);
            this.creative = toValue(creative);
            this.tracker = toValue(tracker);
        }

        public ReferralCreated ()
        {
            this(null, null, null, null);
        }
    }

    @Event(name="VisitorInfoCreated")
    public static class VisitorInfoCreated implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Index @Field final public String tracker;

        public VisitorInfoCreated (VisitorInfo info)
        {
            this.timestamp = new Date();
            this.tracker = toValue(info.id);
        }
    }

    @Event(name="VectorAssociated")
    public static class VectorAssociated implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Index @Field final public String tracker;
        @Field final public String vector;

        public VectorAssociated (VisitorInfo info, String vector)
        {
            this.timestamp = new Date();
            this.vector = toValue(vector);
            this.tracker = toValue(info.id);
        }
    }

    @Event(name="HttpReferrerAssociated")
    public static class HttpReferrerAssociated implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Index @Field final public String tracker;
        @Field final public String referrer;

        public HttpReferrerAssociated (VisitorInfo info, String referrer)
        {
            this.timestamp = new Date();
            this.referrer = toValue(referrer);
            this.tracker = toValue(info.id);
        }
    }

    @Event(name="AccountCreated")
    public static class AccountCreated implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int newMemberId;
        @Field final public String inviteId;
        @Field final public String tracker;

        public AccountCreated (int newMemberId, String inviteId, String tracker)
        {
            this.timestamp = new Date();
            this.newMemberId = newMemberId;
            this.inviteId = toValue(inviteId);
            this.tracker = toValue(tracker);
        }
    }

    @Event(name="RoomUpdated")
    public static class RoomUpdated implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;
        @Field final public int sceneId;

        public RoomUpdated (int memberId, int sceneId)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
            this.sceneId = sceneId;
        }
    }

    @Event(name="ProfileUpdated")
    public static class ProfileUpdated implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;

        public ProfileUpdated (int memberId)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
        }
    }

    @Event(name="ForumMessagePosted")
    public static class ForumMessagePosted implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public int memberId;
        @Field final public int threadId;
        /**
         * The number of posts that have been added to the related discussion thread.
         * If this is 1, it indicates that this is the first post of a new thread.
         */
        @Field final public int postNumber;

        public ForumMessagePosted (int memberId, int threadId, int postNumber)
        {
            this.timestamp = new Date();
            this.memberId = memberId;
            this.threadId = threadId;
            this.postNumber = postNumber;
        }
    }

    /**
     * Generic event for an action such as a button click performed on the client.
     */
    @Event(name="ClientAction")
    public static class ClientAction implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public String tracker;
        @Index @Field final public String actionName;
        /** Additional information such as which game's button was clicked */
        @Field final public String details;

        public ClientAction (String tracker, String actionName, String details)
        {
            this.timestamp = new Date();
            this.tracker = tracker;
            this.actionName = actionName;
            this.details = toValue(details);
        }
    }

    /**
     * A/B Test-related action such as a button click or hitting an a/b test page.  Used
     * for short term testing.
     */
    @Event(name="TestAction")
    public static class TestAction implements MsoyEvent
    {
        @Index @Field final public Date timestamp;
        @Field final public String tracker;
        @Index @Field final public String actionName;
        @Index @Field final public String testName;
        @Field final public int testGroup;

        public TestAction (String tracker, String actionName, String testName, int testGroup)
        {
            this.timestamp = new Date();
            this.tracker = tracker;
            this.actionName = actionName;
            this.testName = testName;
            this.testGroup = testGroup;
        }
    }

    protected static String toValue (String input) {
        return (input != null) ? input : "";
    }
}
