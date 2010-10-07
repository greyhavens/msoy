//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;
import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;

/**
 * Defines member-specific services available to the GWT/AJAX web client.
 */
@RemoteServiceRelativePath(WebMemberService.REL_PATH)
public interface WebMemberService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/membersvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + WebMemberService.ENTRY_POINT;

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
     * Notes that a brand new visitor has arrived at our website. We generally create the entry
     * vector on the server before GWT fires up, but only GWT has access to the history token,
     * which is required to make a sensible entry vector. Thus for new visitors, GWT is asked to
     * submit a useful entry vector through this method with 'requested=true'. Furthermore, if
     * for some reason the server-side entry vector generation fails, this method may be called
     * with requested set to false.
     */
    void noteNewVisitor (VisitorInfo info, String pageToken, boolean requested)
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
     * Get the price of a barscription.
     */
    PriceQuote getBarscriptionCost ()
        throws ServiceException;

    /**
     * Purchase a barscription.
     */
    PurchaseResult<WebCreds.Role> barscribe (int authedBarCost)
        throws ServiceException;

    /**
     * Tests to see if we're the manager of the given theme (really just the group).
     */
    Boolean isThemeManager (int themeGroupId)
        throws ServiceException;

    /**
     * Loads the themes the current player is allowed to manage (stamp).
     */
    GroupName[] loadManagedThemes ()
        throws ServiceException;

    /**
     * To be called only for a player who is in a theme, unset the persistent MemberRecord.themeId
     * for that player (but leave her in her current location). This provides a partial escape from
     * the theme for managers who want to e.g. browse the regular shop/stuff while working with
     * one of their themes.
     */
    void escapeTheme ()
        throws ServiceException;
}
