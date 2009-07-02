//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;

/**
 * Defines member-specific services available to the GWT/AJAX web client.
 */
public interface WebMemberService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/membersvc";

    /** Provides results for {@link #loadFriends}. */
    public static class FriendsResult implements IsSerializable
    {
        /** This user's name and member id. */
        public MemberName name;

        /** This user's friends and potentially some greeters. */
        public List<MemberCard> friendsAndGreeters;

        /** The total number of friendsAndGreeters, if requested. */
        public int totalCount;
    }

    /**
     * Looks up a member's card.
     */
    MemberCard getMemberCard (int memberId)
        throws ServiceException;

    /**
     * Figure out whether or not a given member is your friend.
     */
    Friendship getFriendship (int memberId)
        throws ServiceException;

    /**
     * Loads up all friends for the specified member, optionally including some greeters if the
     * friend count is very low.
     * TODO: this is an unbounded query. Include offset and limit parameters and implement paging
     * on the server.
     */
    FriendsResult loadFriends (int memberId, boolean padWithGreeters)
        throws ServiceException;

    /**
     * Loads up all friends for the specified member, optionally including some greeters if the
     * friend count is very low.
     */
    FriendsResult loadGreeters (int offset, int limit)
        throws ServiceException;

    /**
     * Accept a friend invitation.
     */
    void addFriend (int friendId)
        throws ServiceException;

    /**
     * Remove a friend.
     */
    void removeFriend (int friendId)
        throws ServiceException;

    /**
     * Tests if a given member has agreed to automatically accept new friends.
     */
    boolean isAutomaticFriender (int friendId)
        throws ServiceException;

    /**
     * Loads a page of the specified member's mutelist.
     */
    PagedResult<MemberCard> loadMutelist (int memberId, int offset, int limit)
        throws ServiceException;

    /**
     * Update the specified member's mutelist.
     */
    void setMuted (int memberId, int muteeId, boolean muted)
        throws ServiceException;

    /**
     * Loads up the details of an invitation.
     *
     * @param viewing If true, this will ensure that the viewed date in the database has been set.
     * If false, the viewdate will be left alone.
     */
    Invitation getInvitation (String inviteId, boolean viewing)
        throws ServiceException;

    /**
     * Loads up the details of a game invitation. These work a bit differently than regular
     * invitations. The inviter member will not be filled in.
     */
    Invitation getGameInvitation (String inviteId)
        throws ServiceException;

    /**
     * Adds the email address from the given invite to the opt-out list.
     * 
     * @param gameInvite distinguishes between an id from a game invite and that of a normal invite
     */
    void optOut (boolean gameInvite, String inviteId)
        throws ServiceException;

    /**
     * Turns off announcement mailings for the specified member id (if the hash is valid).
     *
     * @return the email address that was opted out.
     */
    String optOutAnnounce (int memberId, String hash)
        throws ServiceException;

    /**
     * Fetch the highest ranked Whirled memebers for display on a leader board.
     * Returns a list of {@link MemberCard} records.
     */
    List<MemberCard> getLeaderList ()
        throws ServiceException;

    /**
     * Notes that a brand new visitor has arrived at our website. We try to catch this before GWT
     * is fired up, but if we can't do that for some reason, GWT will pick up the ball.
     */
    void noteNewVisitor (VisitorInfo info, String pageToken)
        throws ServiceException;

    /**
     * Calculate the visitor's a/b test group (eg 1 or 2) or < 0 for no group.
     */
    int getABTestGroup (VisitorInfo info, String testName, boolean logEvent)
        throws ServiceException;

    /**
     * Log the visitor's a/b test group, usually chosen using LandingTestCookie.
     */
    void logLandingABTestGroup (VisitorInfo info, String testName, int group)
        throws ServiceException;

    /**
     * Reports that the user took an A/B test action.
     */
    void trackTestAction (String test, String action, VisitorInfo info)
        throws ServiceException;

    /**
     * Gets a random facebook template from those with a matching code. If none match, null is
     * returned.
     */
    FacebookTemplateCard getFacebookTemplate (String code)
        throws ServiceException;

    /**
     * Get the price of a barscription.
     */
    PriceQuote getBarscriptionCost ()
        throws ServiceException;

    /**
     * Purchase a barscription.
     */
    PurchaseResult<WebCreds.Role> barscribe (int authedBarCost)
        throws ServiceException;
}
