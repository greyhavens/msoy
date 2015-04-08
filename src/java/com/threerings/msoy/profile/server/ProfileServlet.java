//
// $Id$

package com.threerings.msoy.profile.server;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.Calendars;
import com.samskivert.util.ObjectUtil;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;

import com.threerings.gwt.util.ExpanderResult;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.Award.AwardType;
import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.data.all.CoinAwards;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.GameRating;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.gwt.BrandDetail;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.group.server.persist.BrandShareRecord;
import com.threerings.msoy.group.server.persist.EarnedMedalRecord;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.MedalRecord;
import com.threerings.msoy.group.server.persist.MedalRepository;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.FavoritesRepository;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.notify.server.MsoyNotificationManager;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.person.gwt.ProfileCodes;
import com.threerings.msoy.person.gwt.SelfFeedMessage;
import com.threerings.msoy.person.server.FeedLogic;
import com.threerings.msoy.person.server.GalleryLogic;
import com.threerings.msoy.person.server.persist.InterestRecord;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository.MemberSearchRecord;
import com.threerings.msoy.server.persist.UserActionRepository;
import com.threerings.msoy.spam.server.SpamLogic;
import com.threerings.msoy.underwire.server.SupportLogic;
import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.MemberCard.NotOnline;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.server.MsoyServiceServlet;

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
        result.friends = _memberRepo.loadFriends(tgtrec.memberId, MAX_PROFILE_FRIENDS);
        if (memrec != null) {
            result.friendship = _memberRepo.getTwoWayFriendship(memrec.memberId, tgtrec.memberId);
        }
        result.totalFriendCount = _memberRepo.countFullFriends(tgtrec.memberId);

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
        // Map<Integer, Award> medals = Maps.newHashMap();
        // for (EarnedMedalRecord earnedMedalRec :
        //         _medalRepo.loadRecentEarnedMedals(memberId, ProfileResult.MAX_STAMPS)) {
        //     Award medal = new Award();
        //     medal.whenEarned = earnedMedalRec.whenEarned.getTime();
        //     medals.put(earnedMedalRec.medalId, medal);
        //     result.medals.add(medal);
        // }
        // for (MedalRecord medalRec : _medalRepo.loadMedals(medals.keySet())) {
        //     Award medal = medals.get(medalRec.medalId);
        //     medal.name = medalRec.name;
        //     medal.description = medalRec.description;
        //     medal.icon = medalRec.createIconMedia();
        // }

        // load gallery info
        result.galleries = _galleryLogic.loadGalleries(tgtrec.memberId);

        // load rating and trophy info
        result.trophies = resolveTrophyData(memrec, tgtrec);
        result.ratings = resolveRatingsData(memrec, tgtrec);

        // load group info
        result.groups = resolveGroupsData(memrec, tgtrec);

        // figure out which brands we can assign the player shares in
        result.grantable = resolveBrandInvites(result.groups, memrec, tgtrec);

        // figure out which brands the player has a share in
        result.brands = resolveBrandShares(result.grantable, memrec, tgtrec);

        // load recent favorites
        result.faves = _itemLogic.resolveFavorites(
            _faveRepo.loadRecentFavorites(memberId, MAX_PROFILE_FAVORITES));

        return result;
    }

    public ExpanderResult<Activity> loadActivity (int memberId, long beforeTime, int count)
        throws ServiceException
    {
        // Sanity check
        if (count > 100) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        return _feedLogic.loadMemberActivity(memberId, beforeTime, count);
    }

    // from interface ProfileService
    public void updateProfile (
        int memberId, String displayName, boolean greeter, final Profile profile)
        throws ServiceException
    {
        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null || tgtrec.isDeleted()) {
            log.warning("Can't update non-existent or deleted profile", "id", memberId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        MemberRecord memrec = requireAuthedUser();
        if (memrec.memberId != tgtrec.memberId && !memrec.isSupport()) {
            log.warning("Profile update forbidden", "ourId", memrec.memberId, "targetId", memberId);
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        if (!ObjectUtil.equals(tgtrec.name, displayName)) {
            // this will hork with a ServiceException if the name is bogus
            _memberLogic.setDisplayName(memberId, displayName, tgtrec.isSupport());
        }

        // don't let the user become a greeter if it is disabled
        if (!tgtrec.isGreeter() && greeter) {
            int friendCount = _memberRepo.countFullFriends(memberId);
            if (getGreeterStatus(tgtrec, friendCount) == GreeterStatus.DISABLED) {
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
        }

        // TODO: treatment of birthdays/TOS for Facebook imports
        if (profile.birthday != null) {
            // figure out this player's 13th birthday (based on their claim)
            Calendar cal = Calendars.at(ProfileRecord.fromDateVec(profile.birthday)).addYears(13).
                asCalendar();
            // if that's in the future, protest
            if (cal.after(Calendar.getInstance())) {
                throw new ServiceException(ProfileCodes.E_TOO_YOUNG);
            }
        }

        // load their old profile record for "first time configuration" purposes
        final ProfileRecord orec = _profileRepo.loadProfile(memberId);

        // stuff their updated profile data into the database
        final ProfileRecord nrec = new ProfileRecord(memberId, profile);
        if (orec != null) {
            nrec.modifications = orec.modifications+1;
            nrec.realName = orec.realName;
        } else {
            log.warning("Account missing old profile", "id", memberId);
        }
        _profileRepo.storeProfile(nrec);

        // record that the user updated their profile
        UserAction action = UserAction.createdProfile(memberId);
        if (nrec.modifications == 1) {
            _moneyLogic.awardCoins(memberId, CoinAwards.CREATED_PROFILE, true, action);

            // now that the member has a name, try sending the affiliate a friend request
            int affiliateId = tgtrec.affiliateMemberId;
            if (tgtrec.isSet(MemberRecord.Flag.FRIEND_AFFILIATE) && affiliateId > 0 &&
                _memberRepo.getFriendship(memberId, affiliateId) == Friendship.NOT_FRIENDS) {
                try {
                    _memberLogic.inviteToBeFriend(memberId, affiliateId);

                } catch (Exception e) {
                    log.warning("Failed to send affiliate friend request", "memberId", memberId,
                        "affilateMemberId", affiliateId, e);
                }
            }

        } else {
            _userActionRepo.logUserAction(action);
        }
        _eventLog.profileUpdated(memberId, tgtrec.visitorId);

        final boolean photoChanged = !orec.getPhoto().equals(nrec.getPhoto());
        final boolean statusChanged = !ObjectUtil.equals(orec.headline, nrec.headline);
        final boolean greeterChanged = (tgtrec.isGreeter() != greeter);
        if (greeterChanged) {
            tgtrec.setFlag(MemberRecord.Flag.GREETER, greeter);
            _memberRepo.storeFlags(tgtrec);
            // let the world servers know about the info change
            MemberNodeActions.tokensChanged(memberId, tgtrec.toTokenRing());
        }
        if (statusChanged || photoChanged) {
            // let the world servers know about the info change
            // (the name is null, it's changed by other code, above.)
            MemberNodeActions.infoChanged(memberId, null, nrec.getPhoto(), nrec.headline);
        }
    }

    // from interface ProfileService
    public void updateInterests (int memberId, List<Interest> interests)
        throws ServiceException
    {
        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null || tgtrec.isDeleted()) {
            log.warning("Can't update interests for non-existent/deleted player", "id", memberId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        MemberRecord memrec = requireAuthedUser();
        if (memrec.memberId != tgtrec.memberId && !memrec.isSupport()) {
            log.warning("Profile update forbidden", "ourId", memrec.memberId, "targetId", memberId);
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        // store the supplied interests in the repository; blank interests will be deleted
        _profileRepo.storeInterests(memberId, interests);
    }

    // from interface ProfileService
    public List<MemberCard> findProfiles (final String search)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

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
        Map<Integer, Friendship> friendships = (mrec == null) ? null :
            _memberRepo.loadFriendships(mrec.memberId, mids.keySet());
        List<MemberCard> results = _mhelper.resolveMemberCards(mids.keySet(), false, friendships);

        // for each successful result, potentially tweak search ranking by recent-ness
        for (MemberCard result : results) {
            if (!(result.status instanceof NotOnline)) {
                continue;
            }
            // how long ago were they on?
            double age = System.currentTimeMillis() - ((NotOnline) result.status).lastLogon;
            // measured in years?
            age /= (365L * 24 * 3600 * 1000L);

            int memberId = result.name.getId();
            // if they were on 0 ms, divide by 1, if they were on a year ago, divide by 2
            mids.put(memberId, rankFromId.apply(memberId) / (1 + age));
        }

        // finally sort the results using our rank mapping for the ordering
        Collections.sort(results, new Comparator<MemberCard>() {
            public int compare (MemberCard o1, MemberCard o2) {
                // note: ascending sort order, not descending
                return Double.compare(
                    rankFromId.apply(o2.name.getId()),
                    rankFromId.apply(o1.name.getId()));
            }
        });

        return results;
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

    protected Profile resolveProfileData (MemberRecord reqrec, MemberRecord tgtrec)
    {
        ProfileRecord prec = _profileRepo.loadProfile(tgtrec.memberId);
        int forMemberId = (reqrec == null) ? 0 : reqrec.memberId;
        boolean amSupport = (reqrec != null) && reqrec.isSupport();
        Profile profile = (prec == null) ? new Profile() :
            prec.toProfile(tgtrec, forMemberId == tgtrec.memberId || amSupport);

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

    public FeedMessage poke (int memberId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        MemberRecord target = _memberRepo.loadMember(memberId);
        MemberCard myCard = _memberRepo.loadMemberCard(mrec.memberId, true);

        boolean poked = _feedLogic.publishSelfMessage(
            memberId, mrec.memberId, true, FeedMessageType.SELF_POKE,
            target.memberId, target.name, myCard.photo);

        if (poked) {
            _notifyMan.notifyPoke(memberId, myCard.name);
        }

        // Mock up a feed message for the sole purpose of giving the client something to display
        return new SelfFeedMessage(FeedMessageType.SELF_POKE, myCard.name,
            new String[] { ""+target.memberId, target.name, myCard.photo.toString() },
            System.currentTimeMillis());
    }

    public void complainProfile (int memberId, String description)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        _supportLogic.addProfileComplaint(mrec.getName(), memberId, description);
    }

    protected List<GroupCard> resolveGroupsData (MemberRecord reqrec, MemberRecord tgtrec)
    {
        boolean showExclusive = (reqrec != null && reqrec.memberId == tgtrec.memberId);
        return _groupRepo.getMemberGroups(tgtrec.memberId, showExclusive);
    }

    protected List<BrandDetail> resolveBrandShares (Set<Integer> invites, MemberRecord reqrec, MemberRecord tgtrec)
    {
        List<BrandDetail> result = Lists.newArrayList();

        // load details for the brands we can invite to
        for (Integer invite : invites) {
            result.add(_groupLogic.loadBrandDetail(invite));
        }

        // and also for any brand the player's already a shareholder in
        for (BrandShareRecord brandRecord : _groupRepo.getBrands(tgtrec.memberId)) {
            if (!invites.contains(brandRecord.groupId)) {
                result.add(_groupLogic.loadBrandDetail(brandRecord.groupId));
            }
        }
        return result;
    }

    protected Set<Integer> resolveBrandInvites (
        List<GroupCard> groups, MemberRecord reqrec, MemberRecord tgtrec)
    {
        Set<Integer> result = Sets.newHashSet();

        // bail early we are not logged in
        if (reqrec == null) {
            return result;
        }

        // fetch our managed groups from DB
        Set<Integer> managedGroups = Sets.newHashSet();

        for (GroupMembershipRecord grec :
                _groupRepo.getMemberships(reqrec.memberId, Rank.MANAGER)) {
            managedGroups.add(grec.groupId);
        }

        // any group the target's in and which we admin, we can grant/revoke shares
        for (GroupCard card : groups) {
            int groupId = card.name.getGroupId();
            if (managedGroups.contains(groupId)) {
                result.add(groupId);
            }
        }

        return result;
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
        final Map<Integer, GameRating> map = Maps.newHashMap();
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
            if (record.gameId < 0) {
                rrec.singleRating = record.rating;
            } else {
                rrec.multiRating = record.rating;
            }
        }

        // now load up and fill in the game details
        for (final Map.Entry<Integer, GameRating> entry : map.entrySet()) {
            final int gameId = entry.getKey();
            final GameInfoRecord record = _mgameRepo.loadGame(gameId);
            if (record == null) {
                log.info("Player has rating for non-existent game", "id", gameId);
                result.remove(entry.getValue());
            } else {
                entry.getValue().gameName = record.name;
                entry.getValue().gameThumb = record.getThumbMedia();
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
    @Inject protected GalleryLogic _galleryLogic;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MedalRepository _medalRepo;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MsoyNotificationManager _notifyMan;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected SpamLogic _spamLogic;
    @Inject protected SupportLogic _supportLogic;
    @Inject protected TrophyRepository _trophyRepo;
    @Inject protected UserActionRepository _userActionRepo;

    protected static final int MAX_PROFILE_MATCHES = 100;
    protected static final int MAX_PROFILE_FRIENDS = 6;
    protected static final int MAX_PROFILE_GAMES = 10;
    protected static final int MAX_PROFILE_TROPHIES = 6;
    protected static final int MAX_PROFILE_FAVORITES = 4;
    protected static final int MIN_GREETER_LEVEL = DeploymentConfig.devDeployment ? 5 : 10;
    protected static final int MIN_GREETER_FRIENDS = DeploymentConfig.devDeployment ? 3 : 20;

    protected static final int MAX_FEED_ENTRIES = 10;
}
