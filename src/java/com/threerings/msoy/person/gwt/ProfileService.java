//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.GameRating;
import com.threerings.msoy.group.gwt.GroupCard;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines profile-related services available to the GWT/AJAX web client.
 */
public interface ProfileService extends RemoteService
{
    /** Provides results for {@link #loadProfile}. */
    public static class ProfileResult implements IsSerializable
    {
        /** This user's name and member id. */
        public MemberName name;

        /** This user's total friend count. */
        public int totalFriendCount;

        /** Whether or not the requesting member is a friend of this member. */
        public boolean isOurFriend;

        /** This user's basic profile information. */
        public Profile profile;

        /**
         * This user's featured friends.
         */
        public List<Interest> interests;

        /**
         * This user's featured friends.
         */
        public List<MemberCard> friends;

        /**
         * This user's groups.
         */
        public List<GroupCard> groups;

        /**
         * This user's game ratings.
         */
        public List<GameRating> ratings;

        /**
         * This user's recently earned trophies.
         */
        public List<Trophy> trophies;

        /**
         * This member's recent self feed messages.
         */
        public List<FeedMessage> feed;
    }

    /** Provides results for {@link #loadFriends}. */
    public static class FriendsResult implements IsSerializable
    {
        /** This user's name and member id. */
        public MemberName name;

        /**
         * This user's friends.
         */
        public List<MemberCard> friends;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/profilesvc";

    /**
     * Loads the specified member's profile information.
     */
    public ProfileResult loadProfile (WebIdent ident, int memberId)
        throws ServiceException;

    /**
     * Requests that this user's profile be updated.
     */
    public void updateProfile (WebIdent ident, String displayName, Profile profile)
        throws ServiceException;

    /**
     * Updates the calling user's interests.
     */
    public void updateInterests (WebIdent ident, List<Interest> interests)
        throws ServiceException;

    /**
     * Looks for profiles that match the specified search term. We'll aim to be smart about what we
     * search. Returns a (possibly empty) list of {@link MemberCard} records.
     */
    public List<MemberCard> findProfiles (WebIdent ident, String search)
        throws ServiceException;

    /**
     * Loads up all friends for the specified member.
     */
    public FriendsResult loadFriends (WebIdent ident, int memberId)
        throws ServiceException;

    /**
     * Loads the self feed for the specified member
     */
    public List<FeedMessage> loadSelfFeed (int profileMemberId, int cutoffDays)
        throws ServiceException;
}
