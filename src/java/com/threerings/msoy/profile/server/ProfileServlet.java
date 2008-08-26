//
// $Id$

package com.threerings.msoy.profile.server;

import static com.threerings.msoy.Log.log;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;

import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;

import com.threerings.msoy.data.CoinAwards;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.UserActionRepository;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MemberHelper;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.web.server.ServletLogic;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.GameRating;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.FavoritesRepository;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.money.server.MoneyNodeActions;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.server.persist.InterestRecord;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.profile.gwt.ProfileService;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends MsoyServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public ProfileResult loadProfile (final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = getAuthedUser();

        try {
            final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            final ProfileResult result = new ProfileResult();
            result.name = tgtrec.getName();

            // load profile info
            result.profile = resolveProfileData(memrec, tgtrec);

            // load up the member's interests
            final List<Interest> interests = Lists.newArrayList();
            for (final InterestRecord iRec : _profileRepo.loadInterests(memberId)) {
                interests.add(iRec.toRecord());
            }
            result.interests = interests;

            // load friend info
            result.friends = resolveFriendsData(memrec, tgtrec);
            final IntSet friendIds = _memberRepo.loadFriendIds(tgtrec.memberId);
            result.isOurFriend = (memrec != null) && friendIds.contains(memrec.memberId);
            result.totalFriendCount = friendIds.size();

            // load stamp info
            result.stamps = Lists.newArrayList(Lists.transform(
                _badgeRepo.loadRecentEarnedBadges(tgtrec.memberId, ProfileResult.MAX_STAMPS),
                EarnedBadgeRecord.TO_BADGE));

            // load rating and trophy info
            result.trophies = resolveTrophyData(memrec, tgtrec);
            result.ratings = resolveRatingsData(memrec, tgtrec);

            // load group info
            result.groups = resolveGroupsData(memrec, tgtrec);

            // load feed
            result.feed = loadFeed(memberId, DEFAULT_FEED_DAYS);

            // load recent favorites
            result.faves = _itemLogic.resolveFavorites(
                _faveRepo.loadRecentFavorites(memberId, MAX_PROFILE_FAVORITES));

            return result;

        } catch (final PersistenceException pe) {
            log.warning("Failure resolving blurbs [who=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public void updateProfile (String displayName, final Profile profile)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();

        if (displayName != null) {
            displayName = displayName.trim();
        }
        if (!MemberName.isValidDisplayName(displayName) ||
                (!memrec.isSupport() && !MemberName.isValidNonSupportName(displayName))) {
            // you'll only see this with a hacked client
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // TODO: whatever filtering and profanity checking that we want

        try {
            // load their old profile record for "first time configuration" purposes
            final ProfileRecord oprof = _profileRepo.loadProfile(memrec.memberId);

            // stuff their updated profile data into the database
            final ProfileRecord nrec = new ProfileRecord(memrec.memberId, profile);
            if (oprof != null) {
                nrec.modifications = oprof.modifications+1;
                nrec.realName = oprof.realName;
            } else {
                log.warning("Account missing old profile [id=" + memrec.memberId + "].");
            }
            _profileRepo.storeProfile(nrec);

            // record that the user updated their profile
            if (nrec.modifications == 1) {
                final MemberMoney money = _moneyLogic.awardCoins(
                    memrec.memberId, 0, 0, null, CoinAwards.CREATED_PROFILE,
                    "", UserAction.CREATED_PROFILE).getNewMemberMoney();
                _moneyNodeActions.moneyUpdated(money);
            } else {
                _userActionRepo.logUserAction(
                    new UserActionDetails(memrec.memberId, UserAction.UPDATED_PROFILE));
            }
            _eventLog.profileUpdated(memrec.memberId);

            // handle a display name change if necessary
            final boolean nameChanged = memrec.name == null || !memrec.name.equals(displayName);
            final boolean photoChanged = !oprof.getPhoto().equals(nrec.getPhoto());
            final boolean statusChanged = oprof.headline != nrec.headline;

            if (nameChanged) {
                _memberRepo.configureDisplayName(memrec.memberId, displayName);
            }

            if (statusChanged || nameChanged || photoChanged) {
                // let the world servers know about the info change
                MemberNodeActions.infoChanged(
                    memrec.memberId, displayName, nrec.getPhoto(), nrec.headline);
            }

        } catch (final PersistenceException pe) {
            log.warning("Failed to update member's profile " +
                    "[who=" + memrec.who() +
                    ", profile=" + StringUtil.fieldsToString(profile) + "].", pe);
        }
    }

    // from interface ProfileService
    public void updateInterests (final List<Interest> interests)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();

        try {
            // store the supplied interests in the repository; blank interests will be deleted
                _profileRepo.storeInterests(memrec.memberId, interests);

        } catch (final PersistenceException pe) {
            log.warning("Failed to update member's interests [who=" + memrec.who() +
                    ", interests=" + StringUtil.toString(interests) + "].", pe);
        }
    }

    // from interface ProfileService
    public List<MemberCard> findProfiles (final String search)
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser();

        try {
            // if the caller is a member, load up their friends set
            final IntSet callerFriendIds = (mrec == null) ? null :
                _memberRepo.loadFriendIds(mrec.memberId);

            // locate the members that match the supplied search
            final IntSet mids = new ArrayIntSet();

            // first check for an email match (and use only that if we have a match)
            final MemberRecord memrec = _memberRepo.loadMember(search);
            if (memrec != null) {
                mids.add(memrec.memberId);

            } else {
                // look for a display name match
                mids.addAll(_memberRepo.findMembersByDisplayName(
                                search, false, MAX_PROFILE_MATCHES));
                // look for a real name match
                mids.addAll(_profileRepo.findMembersByRealName(
                                search, MAX_PROFILE_MATCHES));
                // look for an interests match
                mids.addAll(_profileRepo.findMembersByInterest(search, MAX_PROFILE_MATCHES));
            }

            // finally resolve cards for these members
            List<MemberCard> results = _mhelper.resolveMemberCards(mids, false, callerFriendIds);
            Collections.sort(results, MemberHelper.SORT_BY_LAST_ONLINE);
            return results;

        } catch (final PersistenceException pe) {
            log.warning("Failure finding profiles [search=" + search + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public List<FeedMessage> loadSelfFeed (final int profileMemberId, final int cutoffDays)
        throws ServiceException
    {
        try {
            return loadFeed(profileMemberId, cutoffDays);

        } catch (final PersistenceException pe) {
            log.warning("Load feed failed [memberId=" + profileMemberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Helper function for {@link #loadSelfFeed} and {@link #loadProfile}.
     */
    protected List<FeedMessage> loadFeed (final int profileMemberId, final int cutoffDays)
        throws PersistenceException
    {
        // load up the feed records for the target member
        Timestamp since = new Timestamp(System.currentTimeMillis() - cutoffDays * 24*60*60*1000L);
        return _servletLogic.resolveFeedMessages(_feedRepo.loadMemberFeed(profileMemberId, since));
    }

    protected Profile resolveProfileData (final MemberRecord reqrec, final MemberRecord tgtrec)
        throws PersistenceException
    {
        final ProfileRecord prec = _profileRepo.loadProfile(tgtrec.memberId);
        final int forMemberId = (reqrec == null) ? 0 : reqrec.memberId;
        final Profile profile = (prec == null) ? new Profile() : prec.toProfile(tgtrec, forMemberId);

        // TODO: if they're online right now, show that

        return profile;
    }

    protected List<MemberCard> resolveFriendsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        final Map<Integer,MemberCard> cards = Maps.newLinkedHashMap();
        for (final FriendEntry entry : _memberRepo.loadFriends(
                 tgtrec.memberId, MAX_PROFILE_FRIENDS)) {
            final MemberCard card = new MemberCard();
            card.name = entry.name;
            cards.put(entry.name.getMemberId(), card);
        }
        for (final ProfileRecord profile : _profileRepo.loadProfiles(cards.keySet())) {
            final MemberCard card = cards.get(profile.memberId);
            card.photo = profile.getPhoto();
            card.headline = profile.headline;
        }

        final List<MemberCard> results = Lists.newArrayList();
        results.addAll(cards.values());
        return results;
    }

    protected List<GroupCard> resolveGroupsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        final boolean showExclusive = (reqrec != null && reqrec.memberId == tgtrec.memberId);
        return _groupRepo.getMemberGroups(tgtrec.memberId, showExclusive);
    }

    protected List<GameRating> resolveRatingsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        // fetch all the rating records for the user
        List<RatingRecord> ratings = _ratingRepo.getRatings(tgtrec.memberId, -1, MAX_PROFILE_GAMES);

        // sort them by rating
        Collections.sort(ratings, new Comparator<RatingRecord>() {
            public int compare (final RatingRecord o1, final RatingRecord o2) {
                return (o1.rating > o2.rating) ? -1 : (o1.rating == o2.rating) ? 0 : 1;
            }
        });

        // create GameRating records for all the games we know about
        final List<GameRating> result = Lists.newArrayList();
        final IntMap<GameRating> map = IntMaps.newHashIntMap();
        for (final RatingRecord record : ratings) {
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
            if (GameRecord.isDeveloperVersion(record.gameId)) {
                rrec.singleRating = record.rating;
            } else {
                rrec.multiRating = record.rating;
            }
        }

        // now load up and fill in the game details
        for (final IntMap.IntEntry<GameRating> entry : map.intEntrySet()) {
            final int gameId = entry.getIntKey();
            final GameRecord record = _gameRepo.loadGameRecord(gameId);
            if (record == null) {
                log.warning("Player has rating for non-existent game [id=" + gameId + "].");
                entry.getValue().gameName = "";
            } else {
                entry.getValue().gameName = record.name;
                entry.getValue().gameThumb = record.getThumbMediaDesc();
            }
        }

        return result;
    }

    protected List<Trophy> resolveTrophyData (final MemberRecord reqrec, final MemberRecord tgtrec)
        throws PersistenceException
    {
        final List<Trophy> list = Lists.newArrayList();
        for (final TrophyRecord record :
                 _trophyRepo.loadRecentTrophies(tgtrec.memberId, MAX_PROFILE_TROPHIES)) {
            list.add(record.toTrophy());
        }
        return list;
    }

    // our dependencies
    @Inject protected MoneyNodeActions _moneyNodeActions;
    @Inject protected ServletLogic _servletLogic;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected TrophyRepository _trophyRepo;
    @Inject protected UserActionRepository _userActionRepo;
    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected FavoritesRepository _faveRepo;

    protected static final int MAX_PROFILE_MATCHES = 100;
    protected static final int MAX_PROFILE_FRIENDS = 6;
    protected static final int MAX_PROFILE_GAMES = 10;
    protected static final int MAX_PROFILE_TROPHIES = 6;
    protected static final int MAX_PROFILE_FAVORITES = 4;

    protected static final int DEFAULT_FEED_DAYS = 2;
}
