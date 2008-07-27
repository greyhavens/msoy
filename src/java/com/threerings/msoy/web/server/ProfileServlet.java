//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

import octazen.addressbook.AddressBookAuthenticationException;
import octazen.addressbook.AddressBookException;
import octazen.addressbook.Contact;
import octazen.addressbook.SimpleAddressBookImporter;
import octazen.addressbook.UnexpectedFormatException;
import octazen.http.HttpException;
import octazen.http.UserInputRequiredException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.GameRating;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.person.gwt.Profile;
import com.threerings.msoy.person.gwt.ProfileCodes;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.server.persist.InterestRecord;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.EmailContact;
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
    public ProfileResult loadProfile (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.getAuthedUser(ident);

        try {
            MemberRecord tgtrec = _memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            ProfileResult result = new ProfileResult();
            result.name = tgtrec.getName();

            // load profile info
            result.profile = resolveProfileData(memrec, tgtrec);

            // load up the member's interests
            List<Interest> interests = Lists.newArrayList();
            for (InterestRecord iRec : _profileRepo.loadInterests(memberId)) {
                interests.add(iRec.toRecord());
            }
            result.interests = interests;

            // load friend info
            result.friends = resolveFriendsData(memrec, tgtrec);
            IntSet friendIds = _memberRepo.loadFriendIds(tgtrec.memberId);
            result.isOurFriend = (memrec != null) && friendIds.contains(memrec.memberId);
            result.totalFriendCount = friendIds.size();

            // load rating and trophy info
            result.trophies = resolveTrophyData(memrec, tgtrec);
            result.ratings = resolveRatingsData(memrec, tgtrec);

            // load group info
            result.groups = resolveGroupsData(memrec, tgtrec);

            // load feed
            result.feed = loadFeed(memberId, DEFAULT_FEED_DAYS);

            return result;

        } catch (PersistenceException pe) {
            log.warning("Failure resolving blurbs [who=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public void updateProfile (WebIdent ident, String displayName, Profile profile)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);

        if (displayName != null) {
            displayName = displayName.trim();
        }
        if (!Profile.isValidDisplayName(displayName) ||
                (!memrec.isSupport() && !Profile.isValidNonSupportName(displayName))) {
            // you'll only see this with a hacked client
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // TODO: whatever filtering and profanity checking that we want

        try {
            // load their old profile record for "first time configuration" purposes
            ProfileRecord oprof = _profileRepo.loadProfile(memrec.memberId);

            // stuff their updated profile data into the database
            ProfileRecord nrec = new ProfileRecord(memrec.memberId, profile);
            if (oprof != null) {
                nrec.modifications = oprof.modifications+1;
                nrec.realName = oprof.realName;
            } else {
                log.warning("Account missing old profile [id=" + memrec.memberId + "].");
            }
            _profileRepo.storeProfile(nrec);

            // record that the user updated their profile
            UserAction action = (nrec.modifications == 1)
                ? UserAction.CREATED_PROFILE : UserAction.UPDATED_PROFILE;

            logUserAction(new UserActionDetails(memrec.memberId, action));
            _eventLog.profileUpdated(memrec.memberId);

            // handle a display name change if necessary
            boolean nameChanged = memrec.name == null || !memrec.name.equals(displayName);
            boolean photoChanged = !oprof.getPhoto().equals(nrec.getPhoto());
            boolean statusChanged = oprof.headline != nrec.headline;

            if (nameChanged) {
                _memberRepo.configureDisplayName(memrec.memberId, displayName);
            }

            if (statusChanged || nameChanged || photoChanged) {
                // let the world servers know about the info change
                MemberNodeActions.infoChanged(
                    memrec.memberId, displayName, nrec.getPhoto(), nrec.headline);
            }

        } catch (PersistenceException pe) {
            log.warning("Failed to update member's profile " +
                    "[who=" + memrec.who() +
                    ", profile=" + StringUtil.fieldsToString(profile) + "].", pe);
        }
    }

    // from interface ProfileService
    public void updateInterests (WebIdent ident, List interests)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);

        try {
            // store the supplied interests in the repository; blank interests will be deleted
            @SuppressWarnings("unchecked") List<Interest> tinterests = interests;
            _profileRepo.storeInterests(memrec.memberId, tinterests);

        } catch (PersistenceException pe) {
            log.warning("Failed to update member's interests [who=" + memrec.who() +
                    ", interests=" + StringUtil.toString(interests) + "].", pe);
        }
    }

    // from interface ProfileService
    public List<MemberCard> findProfiles (WebIdent ident, String search)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            // if the caller is a member, load up their friends set
            IntSet callerFriendIds = (mrec == null) ? null :
                _memberRepo.loadFriendIds(mrec.memberId);

            // locate the members that match the supplied search
            IntSet mids = new ArrayIntSet();

            // first check for an email match (and use only that if we have a match)
            MemberRecord memrec = _memberRepo.loadMember(search);
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

        } catch (PersistenceException pe) {
            log.warning("Failure finding profiles [search=" + search + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public FriendsResult loadFriends (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            MemberRecord tgtrec = _memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            FriendsResult result = new FriendsResult();
            result.name = tgtrec.getName();
            IntSet friendIds = _memberRepo.loadFriendIds(memberId);
            IntSet callerFriendIds = null;
            if (mrec != null) {
                if (mrec.memberId == memberId) {
                    callerFriendIds = friendIds;
                } else {
                    callerFriendIds = _memberRepo.loadFriendIds(mrec.memberId);
                }
            }
            List<MemberCard> list = _mhelper.resolveMemberCards(friendIds, false, callerFriendIds);
            Collections.sort(list, MemberHelper.SORT_BY_LAST_ONLINE);
            result.friends = list;
            return result;

        } catch (PersistenceException pe) {
            log.warning("Failure loading friends [memId=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from ProfileService
    public List<EmailContact> getWebMailAddresses (WebIdent ident, String email, String password)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);

        try {
            // don't let someone attempt more than 5 imports in a 5 minute period
            long now = System.currentTimeMillis();
            if (now > _waCleared + WEB_ACCESS_CLEAR_INTERVAL) {
                _webmailAccess.clear();
                _waCleared = now;
            }
            if (_webmailAccess.increment(memrec.memberId, 1) > MAX_WEB_ACCESS_ATTEMPTS) {
                throw new ServiceException(ProfileCodes.E_MAX_WEBMAIL_ATTEMPTS);
            }
            List<Contact> contacts = SimpleAddressBookImporter.fetchContacts(email, password);
            List<EmailContact> results = Lists.newArrayList();

            for (Contact contact : contacts) {
                EmailContact ec = new EmailContact();
                ec.name = contact.getName();
                ec.email = contact.getEmail();
                MemberRecord member = _memberRepo.loadMember(ec.email);
                if (member != null) {
                    if (_memberRepo.getFriendStatus(memrec.memberId, member.memberId)) {
                        // just skip people who are already friends
                        continue;
                    }
                    ec.mname = member.getName();
                }
                results.add(ec);
            }

            return results;

        } catch (AddressBookAuthenticationException e) {
            throw new ServiceException(ProfileCodes.E_BAD_USERNAME_PASS);
        } catch (UnexpectedFormatException e) {
            log.warning("getWebMailAddresses failed [email=" + email + "].", e);
            throw new ServiceException(ProfileCodes.E_INTERNAL_ERROR);
        } catch (AddressBookException e) {
            throw new ServiceException(ProfileCodes.E_UNSUPPORTED_WEBMAIL);
        } catch (UserInputRequiredException e) {
            throw new ServiceException(ProfileCodes.E_USER_INPUT_REQUIRED);
        } catch (IOException e) {
            log.warning("getWebMailAddresses failed [email=" + email + "].", e);
            throw new ServiceException(ProfileCodes.E_INTERNAL_ERROR);
        } catch (HttpException e) {
            log.warning("getWebMailAddresses failed [email=" + email + "].", e);
            throw new ServiceException(ProfileCodes.E_INTERNAL_ERROR);
        } catch (PersistenceException pe) {
            log.warning("getWebMailAddresses failed [who=" + memrec.who() +
                    ", email=" + email + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public List<FeedMessage> loadSelfFeed (int profileMemberId, int cutoffDays)
        throws ServiceException
    {
        try {
            return loadFeed(profileMemberId, cutoffDays);

        } catch (PersistenceException pe) {
            log.warning("Load feed failed [memberId=" + profileMemberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Helper function for {@link #loadSelfFeed} and {@link #loadProfile}.
     */
    protected List<FeedMessage> loadFeed (int profileMemberId, int cutoffDays)
        throws PersistenceException
    {
        // load up the feed records for the target member
        Timestamp since = new Timestamp(System.currentTimeMillis() - cutoffDays * 24*60*60*1000L);
        return _servletLogic.resolveFeedMessages(_feedRepo.loadMemberFeed(profileMemberId, since));
    }

    protected Profile resolveProfileData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        ProfileRecord prec = _profileRepo.loadProfile(tgtrec.memberId);
        int forMemberId = (reqrec == null) ? 0 : reqrec.memberId;
        Profile profile = (prec == null) ? new Profile() : prec.toProfile(tgtrec, forMemberId);

        // TODO: if they're online right now, show that

        return profile;
    }

    protected List<MemberCard> resolveFriendsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        Map<Integer,MemberCard> cards = Maps.newLinkedHashMap();
        for (FriendEntry entry : _memberRepo.loadFriends(
                 tgtrec.memberId, MAX_PROFILE_FRIENDS)) {
            MemberCard card = new MemberCard();
            card.name = entry.name;
            cards.put(entry.name.getMemberId(), card);
        }
        for (ProfileRecord profile : _profileRepo.loadProfiles(cards.keySet())) {
            MemberCard card = cards.get(profile.memberId);
            card.photo = profile.getPhoto();
            card.headline = profile.headline;
        }

        List<MemberCard> results = Lists.newArrayList();
        results.addAll(cards.values());
        return results;
    }

    protected List<GroupCard> resolveGroupsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        boolean showExclusive = (reqrec != null && reqrec.memberId == tgtrec.memberId);
        return _groupRepo.getMemberGroups(tgtrec.memberId, showExclusive);
    }

    protected List<GameRating> resolveRatingsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        // fetch all the rating records for the user
        List<RatingRecord> ratings = _ratingRepo.getRatings(tgtrec.memberId, -1, MAX_PROFILE_GAMES);

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
            if (GameRecord.isDeveloperVersion(record.gameId)) {
                rrec.singleRating = record.rating;
            } else {
                rrec.multiRating = record.rating;
            }
        }

        // now load up and fill in the game details
        for (IntMap.IntEntry<GameRating> entry : map.intEntrySet()) {
            int gameId = entry.getIntKey();
            GameRecord record = _gameRepo.loadGameRecord(gameId);
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
                 _trophyRepo.loadRecentTrophies(tgtrec.memberId, MAX_PROFILE_TROPHIES)) {
            list.add(record.toTrophy());
        }
        return list;
    }

    protected IntIntMap _webmailAccess = new IntIntMap();
    protected long _waCleared = System.currentTimeMillis();

    // our dependencies
    @Inject protected ServletLogic _servletLogic;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected TrophyRepository _trophyRepo;

    protected static final int MAX_PROFILE_MATCHES = 100;
    protected static final int MAX_PROFILE_FRIENDS = 6;
    protected static final int MAX_PROFILE_GAMES = 10;
    protected static final int MAX_PROFILE_TROPHIES = 6;

    protected static final int MAX_WEB_ACCESS_ATTEMPTS = 5;
    protected static final long WEB_ACCESS_CLEAR_INTERVAL = 5L * 60 * 1000;

    protected static final int DEFAULT_FEED_DAYS = 2;
}
