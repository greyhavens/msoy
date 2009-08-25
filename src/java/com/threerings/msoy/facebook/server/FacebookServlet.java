//
// $Id$

package com.threerings.msoy.facebook.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.inject.Inject;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.ProfileField;
import com.google.code.facebookapi.schema.User;
import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.samskivert.util.Comparators;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.RandomUtil;

import com.threerings.parlor.rating.server.persist.RatingRecord;
import com.threerings.parlor.rating.server.persist.RatingRepository;

import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.server.persist.ExternalMapRecord;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.facebook.gwt.FacebookFriendInfo;
import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.server.FacebookLogic.SessionInfo;
import com.threerings.msoy.facebook.server.KontagentLogic.TrackingId;
import com.threerings.msoy.facebook.server.KontagentLogic.LinkType;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.facebook.server.persist.FacebookTemplateRecord;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.MochiGameInfo;
import com.threerings.msoy.game.server.persist.ArcadeEntryRecord;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;

import com.threerings.msoy.money.server.persist.MoneyRepository;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.SharedNaviUtil;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Implementation of FacebookService.
 */
public class FacebookServlet extends MsoyServiceServlet
    implements FacebookService
{
    @Override // from FacebookService
    public StoryFields getTrophyStoryFields ()
        throws ServiceException
    {
        StoryFields fields = loadBasicStoryFields(new StoryFields(), requireSession(), "trophy");
        if (fields.template == null) {
            throw new ServiceException(MsoyCodes.E_INTERNAL_ERROR);
        }
        return fields;
    }

    @Override // from FacebookService
    public void trophyPublished (int gameId, String ident, String trackingId)
        throws ServiceException
    {
        SessionInfo session = requireSession();
        _facebookRepo.noteTrophyPublished(session.memRec.memberId, gameId, ident);
        _tracker.trackFeedStoryPosted(session.fbid, trackingId);
    }

    @Override // from FacebookService
    public List<FacebookFriendInfo> getAppFriendsInfo ()
        throws ServiceException
    {
        // set up return map
        IntMap<FacebookFriendInfo> friendsInfo = getInitialFriendInfo();

        // insert level
        for (MemberCardRecord friend : _memberRepo.loadMemberCards(friendsInfo.keySet())) {
            FacebookFriendInfo info = friendsInfo.get(friend.memberId);
            info.level = friend.level;
        }

        // find the last played games & ratings (1p only) + collect game ids
        IntMap<FacebookFriendInfo.Thumbnail> games = IntMaps.newHashIntMap();
        IntMap<RatingRecord> lastGames = IntMaps.newHashIntMap();

        Set<Integer> approvedGames = Sets.newHashSet();
        for (Integer gameId : Lists.transform(_mgameRepo.loadArcadeEntries(
            ArcadeData.Portal.FACEBOOK, true), ArcadeEntryRecord.TO_GAME_ID)) {
            approvedGames.add(-gameId);
        }

        for (RatingRecord rating : _ratingRepo.getMostRecentRatings(
            friendsInfo.keySet(), approvedGames, 0)) {
            lastGames.put(rating.playerId, rating);
            FacebookFriendInfo.Thumbnail thumbnail = new FacebookFriendInfo.Thumbnail();
            thumbnail.id = Math.abs(rating.gameId);
            games.put(thumbnail.id, thumbnail);
        }

        // load up games
        for (GameInfoRecord grec : _mgameRepo.loadGenre(GameGenre.ALL, 200)) {
            FacebookFriendInfo.Thumbnail thumbnail = games.get(grec.gameId);
            if (thumbnail == null) {
                continue;
            }
            thumbnail.media = grec.getThumbMedia();
            thumbnail.name = grec.name;
        }

        // build final list and assign last games
        List<FacebookFriendInfo> result = Lists.newArrayList();
        for (FacebookFriendInfo info : friendsInfo.values()) {
            RatingRecord lastGame = lastGames.get(info.memberId);
            FacebookFriendInfo.Thumbnail thumbnail = lastGame != null ?
                games.get(Math.abs(lastGame.gameId)) : null;
            if (thumbnail == null || thumbnail.media == null) {
                // no point in returning folks with no last game or whose last game was deleted
                // or something
                log.info("Null thing", "thumbnail", thumbnail, "lastGame", lastGame);
                continue;
            }
            info.lastGame = thumbnail;
            result.add(info);
        }

        // get the trophy counts for each friend
        for (FacebookFriendInfo info : result) {
            info.trophyCount = _trophyRepo.countTrophies(info.lastGame.id, info.memberId);
        }

        sortByLevel(result);

        return result;
    }

    @Override // from FacebookService
    public List<FacebookFriendInfo> getGameFriendsInfo (int gameId)
        throws ServiceException
    {
        // set up return map
        IntMap<FacebookFriendInfo> friendsInfo = getInitialFriendInfo();

        // find the ratings (single player only)
        IntMap<RatingRecord> ratings = IntMaps.newHashIntMap();
        for (RatingRecord rating :  _ratingRepo.getTopRatings(
            -gameId, friendsInfo.size(), 0L, friendsInfo.intKeySet())) {
            ratings.put(rating.playerId, rating);
        }

        // assign ratings and trophies and build final list
        Map<String, FacebookFriendInfo.Thumbnail> trophies = Maps.newHashMap();
        List<FacebookFriendInfo> result = Lists.newArrayList();
        for (FacebookFriendInfo friend : friendsInfo.values()) {
            RatingRecord rating = ratings.get(friend.memberId);

            // no rating - skip
            if (rating == null) {
                continue;
            }

            result.add(friend);
            friend.level = rating.rating;
            friend.trophyCount = _trophyRepo.countTrophies(gameId, friend.memberId);

            List<TrophyRecord> earned = _trophyRepo.loadRecentTrophies(friend.memberId, 1);
            if (earned.size() > 0) {
                String ident = earned.get(0).ident;
                FacebookFriendInfo.Thumbnail icon = trophies.get(ident);
                if (icon == null) {
                    icon = new FacebookFriendInfo.Thumbnail();
                    Trophy trophy = earned.get(0).toTrophy();
                    icon.name = trophy.name;
                    icon.media = trophy.trophyMedia;
                    trophies.put(ident, icon);
                }
                friend.lastGame = icon;
            }
        }

        sortByLevel(result);

        return result;
    }

    @Override // from FacebookService
    public InviteInfo getInviteInfo (FacebookGame game)
        throws ServiceException
    {
        InviteInfo info = new InviteInfo();
        if (game == null) {
            // application invite
            info.excludeIds = Lists.newArrayList();
            for (ExternalMapRecord exRec :
                _fbLogic.loadMappedFriends(requireAuthedUser(), false, 0)) {
                info.excludeIds.add(Long.valueOf(exRec.externalId));
            }

        } else {
            info.gameName = loadGameStoryFields(new StoryFields(), game).name;
        }

        SessionInfo session = requireSession();

        // TODO: we don't need this at all! Bite Me successfully used <fb:name> in request text,
        // using the facebook user id. We just need to expose said id to GWT at large.

        // use the facebook name for consistency and the facebook gender in case privacy settings
        // have changed. users will expect this
        FacebookJaxbRestClient client = _fbLogic.getFacebookClient(session.mapRec.sessionKey);
        Long userId = session.fbid;
        List<ProfileField> fields = Lists.newArrayList();
        fields.add(ProfileField.FIRST_NAME);
        fields.add(ProfileField.SEX);
        try {
            User user = ((UsersGetInfoResponse)client.users_getInfo(
                Collections.singletonList(userId), fields)).getUser().get(0);
            info.username = user.getFirstName();
            if ("male".equalsIgnoreCase(user.getSex())) {
                info.gender = FacebookService.Gender.MALE;
            } else if ("female".equalsIgnoreCase(user.getSex())) {
                info.gender = FacebookService.Gender.FEMALE;
            } else {
                info.gender = FacebookService.Gender.HIDDEN;
            }

        } catch (FacebookException fe) {
            log.warning("Could not get first name and sex, go figure!", "user", userId, fe);
        }

        // generate the tracking id for the invite
        TrackingId trackingId = game == null ?
            new TrackingId(LinkType.INVITE, userId) :
            new TrackingId(LinkType.INVITE, game.getStringId(), userId);
        info.trackingId = trackingId.flatten();

        return info;
    }

    @Override // from FacebookService
    public StoryFields sendChallengeNotification (FacebookGame game, boolean appOnly)
        throws ServiceException
    {
        SessionInfo session = requireSession();
        StoryFields result = loadGameStoryFields(loadBasicStoryFields(
            new StoryFields(), session, "challenge"), game);
        Map<String, String> replacements = Maps.newHashMap();
        replacements.put("game", result.name);
        replacements.put("game_url", SharedNaviUtil.buildRequest(
            FacebookLogic.WHIRLED_APP_CANVAS, game.getCanvasArgs()));
        _fbLogic.scheduleFriendNotification(session, "challenge", replacements, appOnly);

        return result.template != null ? result : null;
    }

    @Override // from FacebookService
    public StoryFields getChallengeStoryFields (FacebookGame game)
        throws ServiceException
    {
        StoryFields result = loadGameStoryFields(loadBasicStoryFields(
            new StoryFields(), requireSession(), "challenge"), game);
        if (result.template == null) {
            throw new ServiceException(MsoyCodes.E_INTERNAL_ERROR);
        }
        return result;
    }

    @Override
    public void challengePublished (FacebookGame game, String trackingId)
        throws ServiceException
    {
        _tracker.trackFeedStoryPosted(requireSession().fbid, trackingId);
    }

    /**
     * Builds the mapping keyed by member containing all facebook friends of the authed user who
     * also have Whirled accounts, filling in only the facebook uid and member id fields.
     */
    protected IntMap<FacebookFriendInfo> getInitialFriendInfo ()
        throws ServiceException
    {
        IntMap<FacebookFriendInfo> friendsInfo = IntMaps.newHashIntMap();
        for (ExternalMapRecord exRec : _fbLogic.loadMappedFriends(requireAuthedUser(), true, 0)) {
            FacebookFriendInfo info = new FacebookFriendInfo();
            info.facebookUid = Long.valueOf(exRec.externalId);
            info.memberId = exRec.memberId;
            friendsInfo.put(exRec.memberId, info);
        }

        return friendsInfo;
    }

    protected void sortByLevel (List<FacebookFriendInfo> result)
    {
        // sort by level
        Collections.sort(result, new Comparator<FacebookFriendInfo>() {
            @Override public int compare (FacebookFriendInfo o1, FacebookFriendInfo o2) {
                // first by descending level
                int cmp = Comparators.compare(o2.level, o1.level);
                if (cmp == 0) {
                    // then by increasing member id
                    cmp = Comparators.compare(o1.memberId, o2.memberId);
                }
                return cmp;
            }
        });
    }

    protected StoryFields loadBasicStoryFields (
        StoryFields fields, SessionInfo session, String template)
    {
        List<FacebookTemplateRecord> templates = _facebookRepo.loadVariants(template);
        if (templates.size() == 0) {
            log.warning("No Facebook templates found for request", "code", template);
            return fields;
        }

        fields.template = RandomUtil.pickRandom(templates).toTemplateCard();
        TrackingId trackingId = new KontagentLogic.TrackingId(LinkType.FEED_LONG,
            template + fields.template.variant, session.fbid);
        fields.trackingId = trackingId.flatten();
        return fields;
    }

    protected StoryFields loadGameStoryFields (StoryFields fields, FacebookGame game)
        throws ServiceException
    {
        switch (game.type) {
        case WHIRLED:
            // whirled game invite
            GameInfoRecord info = _mgameRepo.loadGame(game.getIntId());
            if (info == null) {
                throw new ServiceException();
            }
            fields.name = info.name;
            fields.description = info.description;
            fields.thumbnailURL = info.getThumbMedia().getMediaPath();
            return fields;

        case MOCHI:
            // mochi game invite
            MochiGameInfo minfo = _mgameRepo.loadMochiGame(game.getStringId());
            if (minfo == null) {
                throw new ServiceException();
            }
            fields.name = minfo.name;
            fields.description = minfo.desc;
            fields.thumbnailURL = minfo.thumbURL;
            return fields;
        }
        throw new ServiceException();
    }

    protected SessionInfo requireSession ()
        throws ServiceException
    {
        return _fbLogic.loadSessionInfo(requireAuthedUser());
    }

    @Inject protected FacebookLogic _fbLogic;
    @Inject protected FacebookRepository _facebookRepo;
    @Inject protected KontagentLogic _tracker;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MoneyRepository _moneyRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected TrophyRepository _trophyRepo;
}
