//
// $Id$

package com.threerings.msoy.profile.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.ObjectUtil;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.data.all.CoinAwards;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.Award.AwardType;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.UserActionRepository;
import com.threerings.msoy.server.persist.MemberRepository.MemberSearchRecord;
import com.threerings.msoy.spam.server.SpamLogic;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.MemberCard.NotOnline;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.GameRating;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.server.persist.EarnedMedalRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.MedalRecord;
import com.threerings.msoy.group.server.persist.MedalRepository;

import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.FavoritesRepository;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.person.server.FeedLogic;
import com.threerings.msoy.person.server.GalleryLogic;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.server.persist.InterestRecord;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.profile.gwt.ProfileService;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends MsoyServiceServlet
    implements ProfileService
{
    /**
     * Tests if the supplied member may become a greeter or already is a greeter.
     */
    public static GreeterStatus getGreeterStatus (MemberRecord memrec, int numFriends)
    {
        if (memrec.isGreeter()) {
            return GreeterStatus.GREETER;

        } else if (memrec.isTroublemaker() || memrec.level < MIN_GREETER_LEVEL ||
            numFriends < MIN_GREETER_FRIENDS) {
            return GreeterStatus.DISABLED;

        } else {
            return GreeterStatus.NORMAL;
        }
    }

    // from interface ProfileService
    public ProfileResult loadProfile (final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = getAuthedUser();
        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null || tgtrec.isDeleted()) {
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

        // load greeter info
        result.greeterStatus = getGreeterStatus(tgtrec, result.totalFriendCount);

        // load stamp info
        result.stamps = Lists.newArrayList(Iterables.transform(Iterables.filter(
            _badgeRepo.loadRecentEarnedBadges(tgtrec.memberId, ProfileResult.MAX_STAMPS),
            new Predicate<EarnedBadgeRecord>() {
                public boolean apply (EarnedBadgeRecord badge) {
                    return BadgeType.getType(badge.badgeCode) != BadgeType.OUTSPOKEN;
                }
            }), EarnedBadgeRecord.TO_BADGE));

        // load medal info
        result.medals = Lists.newArrayList();
        Map<Integer, Award> medals = Maps.newHashMap();
        for (EarnedMedalRecord earnedMedalRec :
                _medalRepo.loadRecentEarnedMedals(memberId, ProfileResult.MAX_STAMPS)) {
            Award medal = new Award();
            medal.whenEarned = earnedMedalRec.whenEarned.getTime();
            medals.put(earnedMedalRec.medalId, medal);
            result.medals.add(medal);
        }
        for (MedalRecord medalRec : _medalRepo.loadMedals(medals.keySet())) {
            Award medal = medals.get(medalRec.medalId);
            medal.name = medalRec.name;
            medal.description = medalRec.description;
            medal.icon = medalRec.createIconMedia();
        }

        // load gallery info
        result.galleries = _galleryLogic.loadGalleries(tgtrec.memberId);

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
    }

    // from interface ProfileService
    public void updateProfile (String displayName, boolean greeter, final Profile profile)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();

        if (!ObjectUtil.equals(memrec.name, displayName)) {
            // this will hork with a ServiceException if the name is bogus
            _memberLogic.setDisplayName(memrec.memberId, displayName, memrec.isSupport());
        }

        // don't let the user become a greeter if it is disabled
        if (!memrec.isGreeter() && greeter) {
            int friendCount = _memberRepo.loadFriendIds(memrec.memberId).size();
            if (getGreeterStatus(memrec, friendCount) == GreeterStatus.DISABLED) {
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
        }

        // TODO: whatever filtering and profanity checking that we want

        // load their old profile record for "first time configuration" purposes
        final ProfileRecord orec = _profileRepo.loadProfile(memrec.memberId);

        // stuff their updated profile data into the database
        final ProfileRecord nrec = new ProfileRecord(memrec.memberId, profile);
        if (orec != null) {
            nrec.modifications = orec.modifications+1;
            nrec.realName = orec.realName;
        } else {
            log.warning("Account missing old profile [id=" + memrec.memberId + "].");
        }
        _profileRepo.storeProfile(nrec);

        // record that the user updated their profile
        if (nrec.modifications == 1) {
            _moneyLogic.awardCoins(memrec.memberId, CoinAwards.CREATED_PROFILE, true,
                                   UserAction.createdProfile(memrec.memberId));
        } else {
            _userActionRepo.logUserAction(UserAction.updatedProfile(memrec.memberId));
        }
        _eventLog.profileUpdated(memrec.memberId, memrec.visitorId);

        final boolean photoChanged = !orec.getPhoto().equals(nrec.getPhoto());
        final boolean statusChanged = !ObjectUtil.equals(orec.headline, nrec.headline);
        final boolean greeterChanged = memrec.isGreeter() != greeter;
        if (greeterChanged) {
            memrec.setFlag(MemberRecord.Flag.GREETER, greeter);
            _memberRepo.storeFlags(memrec);
            // let the world servers know about the info change
            MemberNodeActions.tokensChanged(memrec.memberId, memrec.toTokenRing());
        }
        if (statusChanged || photoChanged) {
            // let the world servers know about the info change
            // (the name is null, it's changed by other code, above.)
            MemberNodeActions.infoChanged(memrec.memberId, null, nrec.getPhoto(), nrec.headline);
        }
    }

    // from interface ProfileService
    public void updateInterests (final List<Interest> interests)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();
        // store the supplied interests in the repository; blank interests will be deleted
        _profileRepo.storeInterests(memrec.memberId, interests);
    }

    // from interface ProfileService
    public List<MemberCard> findProfiles (final String search)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        // if the caller is a member, load up their friends set
        IntSet friendIds = (mrec == null) ? null : _memberRepo.loadFriendIds(mrec.memberId);

        // ccumulate & refine a map of memberId -> that member's current search rank
        final Map<Integer, Double> mids = Maps.newHashMap();

        // convenience Function for getting the search rank for a member while defaulting to 0
        final Function<Integer, Double> rankFromId = Functions.forMap(mids, 0.0);

        // first check for an email match (and use only that if we have a match)
        MemberRecord memrec = _memberRepo.loadMember(search);
        if (memrec != null) {
            mids.put(memrec.memberId, 1.0);

        } else {
            // look for a display name match
            for (MemberSearchRecord rec :
                    _memberRepo.findMembersByDisplayName(search, MAX_PROFILE_MATCHES)) {
                mids.put(rec.memberId, Math.max(rankFromId.apply(rec.memberId), rec.rank));
            }

            // look for a real name match
            for (MemberSearchRecord rec :
                _profileRepo.findMembersByRealName(search, MAX_PROFILE_MATCHES)) {
                mids.put(rec.memberId, Math.max(rankFromId.apply(rec.memberId), rec.rank));
            }
        }

        // now resolve cards for these members
        List<MemberCard> results = _mhelper.resolveMemberCards(mids.keySet(), false, friendIds);

        // for each successful result, potentially tweak search ranking by recent-ness
        for (MemberCard result : results) {
            if (!(result.status instanceof NotOnline)) {
                continue;
            }
            // how long ago were they on?
            double age = System.currentTimeMillis() - ((NotOnline) result.status).lastLogon;
            // measured in years?
            age /= (365L * 24 * 3600 * 1000L);

            int memberId = result.name.getMemberId();
            // if they were on 0 ms, divide by 1, if they were on a year ago, divide by 2
            mids.put(memberId, rankFromId.apply(memberId) / (1 + age));
        }
        
        // finally sort the results using our rank mapping for the ordering
        Collections.sort(results, new Comparator<MemberCard>() {
            public int compare (MemberCard o1, MemberCard o2) {
                // note: ascending sort order, not descending
                return Double.compare(
                    rankFromId.apply(o2.name.getMemberId()),
                    rankFromId.apply(o1.name.getMemberId()));
            }
        });

        return results;
    }

    // from interface ProfileService
    public List<FeedMessage> loadSelfFeed (final int profileMemberId, final int cutoffDays)
        throws ServiceException
    {
        return loadFeed(profileMemberId, cutoffDays);
    }

    // from interface ProfileService
    public void sendRetentionEmail (int profileMemberId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        if (!mrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }
        _spamLogic.testRetentionEmail(profileMemberId, mrec.accountName);
    }

    /**
     * Helper function for {@link #loadSelfFeed} and {@link #loadProfile}.
     */
    protected List<FeedMessage> loadFeed (final int profileMemberId, final int cutoffDays)
    {
        // load up the feed records for the target member
        long since = System.currentTimeMillis() - cutoffDays * 24*60*60*1000L;
        return _feedLogic.resolveFeedMessages(_feedRepo.loadMemberFeed(profileMemberId, since));
    }

    protected Profile resolveProfileData (MemberRecord reqrec, MemberRecord tgtrec)
    {
        ProfileRecord prec = _profileRepo.loadProfile(tgtrec.memberId);
        int forMemberId = (reqrec == null) ? 0 : reqrec.memberId;
        Profile profile = (prec == null) ? new Profile() : prec.toProfile(tgtrec, forMemberId);

        if (profile.award != null && profile.award.type == AwardType.BADGE) {
            EarnedBadgeRecord earnedBadgeRec =
                _badgeRepo.loadEarnedBadge(tgtrec.memberId, profile.award.awardId);
            profile.award.name = Badge.getLevelName(earnedBadgeRec.level);
            profile.award.whenEarned = earnedBadgeRec.whenEarned.getTime();
            profile.award.icon = EarnedBadge.getImageMedia(
                earnedBadgeRec.badgeCode, earnedBadgeRec.level);

        } else if (profile.award != null && profile.award.type == AwardType.MEDAL) {
            EarnedMedalRecord earnedMedalRec =
                _medalRepo.loadEarnedMedal(tgtrec.memberId, profile.award.awardId);
            MedalRecord medalRec = _medalRepo.loadMedal(profile.award.awardId);
            if (medalRec != null) {
                profile.award.whenEarned = earnedMedalRec.whenEarned.getTime();
                profile.award.name = medalRec.name;
                profile.award.icon = medalRec.createIconMedia();
            } else {
                profile.award = null;
            }
        }

        // TODO: if they're online right now, show that

        return profile;
    }

    protected List<MemberCard> resolveFriendsData (MemberRecord reqrec, MemberRecord tgtrec)
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
    {
        final boolean showExclusive = (reqrec != null && reqrec.memberId == tgtrec.memberId);
        return _groupRepo.getMemberGroups(tgtrec.memberId, showExclusive);
    }

    protected List<GameRating> resolveRatingsData (MemberRecord reqrec, MemberRecord tgtrec)
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
            final GameRecord record = _mgameRepo.loadGameRecord(gameId);
            if (record == null) {
                log.info("Player has rating for non-existent game [id=" + gameId + "].");
                result.remove(entry.getValue());
            } else {
                entry.getValue().gameName = record.name;
                entry.getValue().gameThumb = record.getThumbMediaDesc();
            }
        }

        return result;
    }

    protected List<Trophy> resolveTrophyData (final MemberRecord reqrec, final MemberRecord tgtrec)
    {
        final List<Trophy> list = Lists.newArrayList();
        for (final TrophyRecord record :
                 _trophyRepo.loadRecentTrophies(tgtrec.memberId, MAX_PROFILE_TROPHIES)) {
            list.add(record.toTrophy());
        }
        return list;
    }

    // our dependencies
    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected FavoritesRepository _faveRepo;
    @Inject protected FeedLogic _feedLogic;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected GalleryLogic _galleryLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MedalRepository _medalRepo;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected SpamLogic _spamLogic;
    @Inject protected TrophyRepository _trophyRepo;
    @Inject protected UserActionRepository _userActionRepo;

    protected static final int MAX_PROFILE_MATCHES = 100;
    protected static final int MAX_PROFILE_FRIENDS = 6;
    protected static final int MAX_PROFILE_GAMES = 10;
    protected static final int MAX_PROFILE_TROPHIES = 6;
    protected static final int MAX_PROFILE_FAVORITES = 4;
    protected static final int MIN_GREETER_LEVEL = DeploymentConfig.devDeployment ? 5 : 10;
    protected static final int MIN_GREETER_FRIENDS = DeploymentConfig.devDeployment ? 3 : 20;

    protected static final int DEFAULT_FEED_DAYS = 2;
}
