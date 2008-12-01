//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;


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
    boolean getFriendStatus (int memberId)
        throws ServiceException;

    /**
     * Loads up all friends for the specified member, optionally including some greeters if the
     * friend count is very low.
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
     * Loads up the details of an invitation.
     *
     * @param viewing If true, this will ensure that the viewed date in the database has been set.
     * If false, the viewdate will be left alone.
     */
    Invitation getInvitation (String inviteId, boolean viewing)
        throws ServiceException;

    /**
     * Adds the email address from the given invite to the opt-out list.
     */
    void optOut (String inviteId)
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
     * Calculate the visitor's a/b test group (eg 1 or 2) or < 0 for no group.
     */
    int getABTestGroup (VisitorInfo info, String testName, boolean logEvent)
        throws ServiceException;

    /**
     * Generic method for tracking a client-side action such as clicking a button.
     */
    void trackClientAction (VisitorInfo info, String actionName, String details)
        throws ServiceException;

    /**
     * Tracking a client-side action such as clicking a button during an a/b test.  If testName
     * is supplied, the visitor's a/b test group will also be tracked.
     */
    void trackTestAction (VisitorInfo info, String actionName, String testName)
        throws ServiceException;

    /**
     * Tracks the creation of a new tracking info structure, for a new visitor.
     */
    void trackVisitorInfoCreation (VisitorInfo info)
        throws ServiceException;

    /**
     * Records that a new entry vector has been recorded.
     */
    void trackVectorAssociation (VisitorInfo info, String vector)
        throws ServiceException;

    /**
     * Records an HTTP referrer has been recorded for a specific visitor.
     */
    void trackHttpReferrerAssociation (VisitorInfo info, String referrer)
        throws ServiceException;

    /**
     * Records a change in session status: is this a registered player or not,
     * and if not, was this a guest visit?
     */
    void trackSessionStatusChange (VisitorInfo info, boolean guest, boolean newInfo);

    /**
     * Small debugging function, writes to text logs on the server. TODO: remove me.
     */
    void debugLog (String stage, String token, String vector);
}
