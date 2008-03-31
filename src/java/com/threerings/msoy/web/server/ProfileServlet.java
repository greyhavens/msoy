//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.parlor.rating.server.persist.RatingRecord;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.server.persist.GameRecord;

import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.person.server.persist.ProfileRecord;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.GameRating;
import com.threerings.msoy.web.data.GroupCard;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends MsoyServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public void updateProfile (WebIdent ident, String displayName, Profile profile)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        if (displayName != null) {
            displayName = displayName.trim();
        }
        if (!Profile.isValidDisplayName(displayName)) {
            // you'll only see this with a hacked client
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // TODO: whatever filtering and profanity checking that we want

        try {
            // load their old profile record for "first time configuration" purposes
            ProfileRecord oprof = MsoyServer.profileRepo.loadProfile(memrec.memberId);

            // stuff their updated profile data into the database
            ProfileRecord nrec = new ProfileRecord(memrec.memberId, profile);
            if (oprof != null) {
                nrec.modifications = oprof.modifications+1;
                nrec.realName = oprof.realName;
            } else {
                log.warning("Account missing old profile [id=" + memrec.memberId + "].");
            }
            MsoyServer.profileRepo.storeProfile(nrec);

            // record that the user updated their profile
            UserAction action = (nrec.modifications == 1) 
                ? UserAction.CREATED_PROFILE : UserAction.UPDATED_PROFILE;

            logUserAction(new UserActionDetails(memrec.memberId, action));
            _eventLog.profileUpdated(memrec.memberId);

            // handle a display name change if necessary
            if (memrec.name == null || !memrec.name.equals(displayName)) {
                MsoyServer.memberRepo.configureDisplayName(memrec.memberId, displayName);
                // let the world servers know about the display name change
                MemberNodeActions.displayNameChanged(new MemberName(displayName, memrec.memberId));
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update member's profile " +
                    "[who=" + memrec.who() +
                    ", profile=" + StringUtil.fieldsToString(profile) + "].", pe);
        }
    }

    // from interface ProfileService
    public ProfileResult loadProfile (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = getAuthedUser(ident);

        try {
            MemberRecord tgtrec = MsoyServer.memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            ProfileResult result = new ProfileResult();
            result.name = tgtrec.getName();

            // load profile info
            result.profile = resolveProfileData(memrec, tgtrec);

            // load friend info
            result.friends = resolveFriendsData(memrec, tgtrec);
            IntSet friendIds = MsoyServer.memberRepo.loadFriendIds(tgtrec.memberId);
            result.isOurFriend = (memrec != null) && friendIds.contains(memrec.memberId);
            result.totalFriendCount = friendIds.size();

            // load rating and trophy info
            result.trophies = resolveTrophyData(memrec, tgtrec);
            result.ratings = resolveRatingsData(memrec, tgtrec);

            // load group info
            result.groups = resolveGroupsData(memrec, tgtrec);

            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure resolving blurbs [who=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public List<MemberCard> findProfiles (WebIdent ident, String search)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            // if the caller is a member, load up their friends set
            IntSet callerFriendIds = (mrec == null) ? null :
                MsoyServer.memberRepo.loadFriendIds(mrec.memberId);

            // locate the members that match the supplied search
            IntSet mids = new ArrayIntSet();

            // first check for an email match (and use only that if we have a match)
            MemberRecord memrec = MsoyServer.memberRepo.loadMember(search);
            if (memrec != null) {
                mids.add(memrec.memberId);

            } else {
                // look for a display name match
                mids.addAll(MsoyServer.memberRepo.findMembersByDisplayName(
                                search, MAX_PROFILE_MATCHES));
                // look for a real name match
                mids.addAll(MsoyServer.profileRepo.findMembersByRealName(
                                search, MAX_PROFILE_MATCHES));
            }

            // finally resolve cards for these members
            List<MemberCard> results = ServletUtil.resolveMemberCards(mids, false, callerFriendIds);
            Collections.sort(results, ServletUtil.SORT_BY_LAST_ONLINE);
            return results;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure finding profiles [search=" + search + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public FriendsResult loadFriends (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            MemberRecord tgtrec = MsoyServer.memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            FriendsResult result = new FriendsResult();
            result.name = tgtrec.getName();
            IntSet friendIds = MsoyServer.memberRepo.loadFriendIds(memberId);
            IntSet callerFriendIds = null;
            if (mrec != null) {
                if (mrec.memberId == memberId) {
                    callerFriendIds = friendIds;
                } else {
                    callerFriendIds = MsoyServer.memberRepo.loadFriendIds(mrec.memberId);
                }
            }
            List<MemberCard> list =
                ServletUtil.resolveMemberCards(friendIds, false, callerFriendIds);
            Collections.sort(list, ServletUtil.SORT_BY_LAST_ONLINE);
            result.friends = list;
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure loading friends [memId=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected Profile resolveProfileData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        ProfileRecord prec = MsoyServer.profileRepo.loadProfile(tgtrec.memberId);
        int forMemberId = (reqrec == null) ? 0 : reqrec.memberId;
        Profile profile = (prec == null) ? new Profile() : prec.toProfile(tgtrec, forMemberId);

        // TODO: if they're online right now, show that

        return profile;
    }

    protected List<MemberCard> resolveFriendsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        IntMap<MemberCard> cards = IntMaps.newHashIntMap();
        for (FriendEntry entry :
                 MsoyServer.memberRepo.loadFriends(tgtrec.memberId, MAX_PROFILE_FRIENDS)) {
            MemberCard card = new MemberCard();
            card.name = entry.name;
            cards.put(entry.name.getMemberId(), card);
        }
        resolveCardData(cards);

        List<MemberCard> results = Lists.newArrayList();
        results.addAll(cards.values());
        return results;
    }

    protected List<GroupCard> resolveGroupsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        boolean showExclusive = (reqrec != null && reqrec.memberId == tgtrec.memberId);
        return MsoyServer.groupRepo.getMemberGroups(tgtrec.memberId, showExclusive);
    }

    protected List<GameRating> resolveRatingsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        // fetch all the rating records for the user
        List<RatingRecord> ratings = MsoyServer.ratingRepo.getRatings(
            tgtrec.memberId, -1, MAX_PROFILE_GAMES);

        // sort them by rating
        Collections.sort(ratings, new Comparator<RatingRecord>() {
            public int compare (RatingRecord o1, RatingRecord o2) {
                return (o1.rating > o2.rating) ? -1 : (o1.rating == o2.rating) ? 0 : 1;
            }
        });

        // create GameRating records for all the games we know about
        List<GameRating> result = Lists.newArrayList();
        IntMap<GameRating> map = IntMaps.newHashIntMap();
        for (RatingRecord record : ratings) {
            GameRating rrec = map.get(Math.abs(record.gameId));
            if (rrec == null) {
                // stop adding results
                if (result.size() >= MAX_PROFILE_MATCHES) {
                    continue;
                }
                rrec = new GameRating();
                rrec.gameId = Math.abs(record.gameId);
                result.add(rrec);
                map.put(rrec.gameId, rrec);
            }
            if (record.gameId < 0) {
                rrec.singleRating = record.rating;
            } else {
                rrec.multiRating = record.rating;
            }
        }

        // now load up and fill in the game details
        for (IntMap.IntEntry<GameRating> entry : map.intEntrySet()) {
            int gameId = entry.getIntKey();
            GameRecord record = MsoyServer.itemMan.getGameRepository().loadGameRecord(gameId);
            if (record == null) {
                log.warning("Player has rating for non-existent game [id=" + gameId + "].");
                entry.getValue().gameName = "";
            } else {
                entry.getValue().gameName = record.name;
                if (record.thumbMediaHash != null) {
                    entry.getValue().gameThumb = new MediaDesc(
                        record.thumbMediaHash, record.thumbMimeType, record.thumbConstraint);
                }
            }
        }

        return result;
    }

    protected List<Trophy> resolveTrophyData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        List<Trophy> list = Lists.newArrayList();
        for (TrophyRecord record :
                 MsoyServer.trophyRepo.loadRecentTrophies(tgtrec.memberId, MAX_PROFILE_TROPHIES)) {
            list.add(record.toTrophy());
        }
        return list;
    }

    protected static void resolveCardData (IntMap<? extends MemberCard> cards)
        throws PersistenceException
    {
        for (ProfileRecord profile : MsoyServer.profileRepo.loadProfiles(cards.intKeySet())) {
            MemberCard card = cards.get(profile.memberId);
            card.photo = profile.getPhoto();
            card.headline = profile.headline;
        }
    }

    protected static final int MAX_PROFILE_MATCHES = 100;
    protected static final int MAX_PROFILE_FRIENDS = 6;
    protected static final int MAX_PROFILE_GAMES = 10;
    protected static final int MAX_PROFILE_TROPHIES = 6;
}
