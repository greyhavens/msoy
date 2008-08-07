//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;

import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceException;

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

        /** This user's friends. */
        public List<MemberCard> friends;
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
     * Loads up all friends for the specified member.
     */
    FriendsResult loadFriends (int memberId)
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
     * Fetch the highest ranked Whirled memebers for display on a leader board.
     * Returns a list of {@link MemberCard} records.
     */
    List<MemberCard> getLeaderList ()
        throws ServiceException;

    /**
     * Calculate the visitor's a/b test group (eg 1 or 2) or < 0 for no group.
     */
    int getABTestGroup (ReferralInfo info, String testName, boolean logEvent)
        throws ServiceException;

    /**
     * Generic method for tracking a client-side action such as clicking a button.
     */
    void trackClientAction (ReferralInfo info, String actionName, String details)
        throws ServiceException;

    /**
     * Tracking a client-side action such as clicking a button during an a/b test.  If testName
     * is supplied, the visitor's a/b test group will also be tracked.
     */
    void trackTestAction (ReferralInfo info, String actionName, String testName)
        throws ServiceException;

    /**
     * Tracks the creation of a new referral info structure, for a new visitor.
     */
    void trackReferralCreation (ReferralInfo info)
        throws ServiceException;
}
