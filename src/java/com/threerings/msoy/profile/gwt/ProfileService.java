//
// $Id$

package com.threerings.msoy.profile.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.GameRating;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.Interest;

/**
 * Defines profile-related services available to the GWT/AJAX web client.
 */
public interface ProfileService extends RemoteService
{
    /** Provides results for {@link #loadProfile}. */
    public static class ProfileResult implements IsSerializable
    {
        /** The maximum number of stamps that will be shown on the profile page. */
        public static final int MAX_STAMPS = 6;

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
         * This user's recent stamps.
         */
        public List<EarnedBadge> stamps;

        /**
         * This user's groups.
         */
        public List<GroupCard> groups;

        /**
         * This user's game ratings.
         */
        public List<GameRating> ratings;

        /**
         * This user's list of galleries
         */
        public List<Gallery> galleries;

        /**
         * This user's recently earned trophies.
         */
        public List<Trophy> trophies;

        /**
         * This member's recent self feed messages.
         */
        public List<FeedMessage> feed;

        /**
         * This member's recently favorited items.
         */
        public List<ListingCard> faves;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/profilesvc";

    /**
     * Loads the specified member's profile information.
     */
    ProfileResult loadProfile (int memberId)
        throws ServiceException;

    /**
     * Requests that this user's profile be updated.
     */
    void updateProfile (String displayName, Profile profile)
        throws ServiceException;

    /**
     * Updates the calling user's interests.
     */
    void updateInterests (List<Interest> interests)
        throws ServiceException;

    /**
     * Looks for profiles that match the specified search term. We'll aim to be smart about what we
     * search. Returns a (possibly empty) list of {@link MemberCard} records.
     */
    List<MemberCard> findProfiles (String search)
        throws ServiceException;

    /**
     * Loads the self feed for the specified member
     */
    List<FeedMessage> loadSelfFeed (int profileMemberId, int cutoffDays)
        throws ServiceException;
}
