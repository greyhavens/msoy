//
// $Id$

package com.threerings.msoy.profile.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.gwt.util.ExpanderResult;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.GameRating;
import com.threerings.msoy.group.gwt.BrandDetail;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Defines profile-related services available to the GWT/AJAX web client.
 */
@RemoteServiceRelativePath(ProfileService.REL_PATH)
public interface ProfileService extends RemoteService
{
    /** The various greeter states. */
    public static enum GreeterStatus {NORMAL, GREETER, DISABLED};

    /** Provides results for {@link #loadProfile}. */
    public static class ProfileResult implements IsSerializable
    {
        /** The maximum number of stamps that will be shown on the profile page. */
        public static final int MAX_STAMPS = 6;

        /** This user's name and member id. */
        public MemberName name;

        /** This user's total friend count. */
        public int totalFriendCount;

        /** The full friendship status of this member in relation to the requesting member. */
        public Friendship friendship;

        /** Whether or not the requesting member is a whirled greeter or may become one. */
        public GreeterStatus greeterStatus;

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
         * The user's recent medals.
         */
        public List<Award> medals;

        /**
         * This user's groups.
         */
        public List<GroupCard> groups;

        /**
         * A list of brands in which this player has shares.
         */
        public List<BrandDetail> brands;

        /**
         * A subset of this player's groups in which we have management privileges;
         */
        public Set<Integer> grantable;

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
         * This member's recently favorited items.
         */
        public List<ListingCard> faves;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/profilesvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + ProfileService.ENTRY_POINT;

    /**
     * Loads the specified member's profile information.
     */
    ProfileResult loadProfile (int memberId)
        throws ServiceException;

    /**
     * Loads a page of recent activity (comments and other player history).
     */
    ExpanderResult<Activity> loadActivity (int memberId, long beforeTime, int count)
        throws ServiceException;

    /**
     * Requests that this user's profile be updated.
     */
    void updateProfile (int memberId, String displayName, boolean greeter, Profile profile)
        throws ServiceException;

    /**
     * Updates the calling user's interests.
     */
    void updateInterests (int memberId, List<Interest> interests)
        throws ServiceException;

    /**
     * Looks for profiles that match the specified search term. We'll aim to be smart about what we
     * search. Returns a (possibly empty) list of {@link MemberCard} records.
     */
    List<MemberCard> findProfiles (String search)
        throws ServiceException;

    /**
     * For testing, sends the given user's retention email to the authenticated user's email
     * address.
     */
    void sendRetentionEmail (int profileMemberId)
        throws ServiceException;

    /**
     * Sends a poke to another player.
     * @return The feed message generated
     */
    FeedMessage poke (int memberId)
        throws ServiceException;

    void complainProfile (int memberId, String description)
        throws ServiceException;
}
