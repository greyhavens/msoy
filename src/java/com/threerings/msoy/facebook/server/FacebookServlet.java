//
// $Id$

package com.threerings.msoy.facebook.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.inject.Inject;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.ProfileField;
import com.google.code.facebookapi.schema.User;
import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.samskivert.util.CollectionUtil;
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

import com.threerings.msoy.apps.server.persist.AppInfoRecord;
import com.threerings.msoy.apps.server.persist.AppRepository;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.facebook.gwt.FacebookFriendInfo;
import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.server.FacebookLogic.SessionInfo;
import com.threerings.msoy.facebook.server.KontagentLogic.TrackingId;
import com.threerings.msoy.facebook.server.KontagentLogic.LinkType;
import com.threerings.msoy.facebook.server.persist.FacebookActionRecord;
import com.threerings.msoy.facebook.server.persist.FacebookInfoRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.facebook.server.persist.FacebookTemplateRecord;
import com.threerings.msoy.facebook.server.persist.FeedThumbnailRecord;

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

import com.threerings.msoy.web.gwt.ExternalSiteId;
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
    /** Hard-wired feed story and thumbnail code for trophy posting. */
    public static final String TROPHY = "trophy";

    /** Hard-wired feed story and thumbnail code for challenge posts. */
    public static final String CHALLENGE = "challenge";

    /** Hard-wired feed story and thumbnail code for levl up posts. */
    public static final String LEVELUP = "levelup";

    @Override // from FacebookService
    public StoryFields getTrophyStoryFields (int appId, int gameId)
        throws ServiceException
    {
        StoryFields fields = loadGameStoryFields(loadBasicStoryFields(
            new StoryFields(), requireSession(appId), TROPHY), appId, new FacebookGame(gameId),
            TROPHY);

        if (fields.template == null) {
            throw new ServiceException(MsoyCodes.E_INTERNAL_ERROR);
        }
        return fields;
    }

    @Override // from FacebookService
    public void trophyPublished (int appId, int gameId, String ident, String trackingId)
        throws ServiceException
    {
        SessionInfo session = requireSession(appId);
        _facebookRepo.recordAction(FacebookActionRecord.trophyPublished(
            appId, session.memRec.memberId, gameId, ident));
        _tracker.trackFeedStoryPosted(appId, session.fbid, trackingId);
    }

    @Override // from FacebookService
    public List<FacebookFriendInfo> getAppFriendsInfo (int appId)
        throws ServiceException
    {
        // set up return map
        IntMap<FacebookFriendInfo> friendsInfo = getInitialFriendInfo(appId);

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
    public List<FacebookFriendInfo> getGameFriendsInfo (int appId, int gameId)
        throws ServiceException
    {
        // set up return map
        IntMap<FacebookFriendInfo> friendsInfo = getInitialFriendInfo(appId);

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
    public InviteInfo getInviteInfo (int appId, FacebookGame game)
        throws ServiceException
    {
        SessionInfo session = requireSession(appId);

        InviteInfo info = new InviteInfo();

        AppInfoRecord appInfo = _appRepo.loadAppInfo(appId);
        info.appName = appInfo != null ? appInfo.name : "";

        FacebookInfoRecord fbinfo = _facebookRepo.loadAppFacebookInfo(appId);
        info.canvasName = fbinfo != null ? fbinfo.canvasName : "";

        if (game == null) {
            // application invite
            info.excludeIds = Lists.newArrayList();
            for (ExternalMapRecord exRec : _fbLogic.loadMappedFriends(
                session.siteId, requireAuthedUser(), false, 0)) {
                info.excludeIds.add(Long.valueOf(exRec.externalId));
            }

        } else {
            info.gameName = loadGameStoryFields(new StoryFields(), appId, game, null).name;
        }

        // TODO: we don't need this at all! Bite Me successfully used <fb:name> in request text,
        // using the facebook user id. We just need to expose said id to GWT at large.

        // use the facebook name for consistency and the facebook gender in case privacy settings
        // have changed. users will expect this
        FacebookJaxbRestClient client = _fbLogic.getFacebookClient(
            session.siteId, session.mapRec.sessionKey);
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
    public StoryFields sendChallengeNotification (int appId, FacebookGame game, boolean appOnly)
        throws ServiceException
    {
        SessionInfo session = requireSession(appId);
        StoryFields result = loadGameStoryFields(loadBasicStoryFields(
            new StoryFields(), session, CHALLENGE), appId, game, CHALLENGE);
        Map<String, String> replacements = Maps.newHashMap();
        replacements.put("game", result.name);
        replacements.put("game_url", SharedNaviUtil.buildRequest(
            _fbLogic.getCanvasUrl(session.siteId), game.getCanvasArgs()));
        _fbLogic.scheduleFriendNotification(session, CHALLENGE, replacements, appOnly);

        return result.template != null ? result : null;
    }

    @Override // from FacebookService
    public StoryFields getChallengeStoryFields (int appId, FacebookGame game)
        throws ServiceException
    {
        StoryFields result = loadGameStoryFields(loadBasicStoryFields(new StoryFields(),
            requireSession(appId), CHALLENGE), appId, game, CHALLENGE);
        if (result.template == null) {
            throw new ServiceException(MsoyCodes.E_INTERNAL_ERROR);
        }
        return result;
    }

    @Override // from FacebookService
    public StoryFields getLevelUpStoryFields (int appId)
        throws ServiceException
    {
        StoryFields result = loadBasicStoryFields(
            new StoryFields(), requireSession(appId), "levelup");
        if (result.template == null) {
            throw new ServiceException(MsoyCodes.E_INTERNAL_ERROR);
        }
        result.thumbnails = assembleThumbnails(appId, LEVELUP, null, 0);
        return result;
    }

    @Override // from FacebookService
    public void challengePublished (int appId, FacebookGame game, String trackingId)
        throws ServiceException
    {
        _tracker.trackFeedStoryPosted(appId, requireSession(appId).fbid, trackingId);
    }

    @Override // from FacebookService
    public void levelUpPublished (int appId, String trackingId)
        throws ServiceException
    {
        _tracker.trackFeedStoryPosted(appId, requireSession(appId).fbid, trackingId);
    }

    @Override // from FacebookService
    public void trackPageRequest (int appId, String page)
        throws ServiceException
    {
        HttpServletRequest req = getThreadLocalRequest();
        SessionInfo session = requireSession(appId);
        _tracker.trackPageRequest(appId, session.fbid, req.getRemoteAddr(), page);
    }

    /**
     * Builds the mapping keyed by member containing all facebook friends of the authed user who
     * also have Whirled accounts linked to the given app, filling in only the facebook uid and
     * member id fields.
     */
    protected IntMap<FacebookFriendInfo> getInitialFriendInfo (int appId)
        throws ServiceException
    {
        IntMap<FacebookFriendInfo> friendsInfo = IntMaps.newHashIntMap();
        for (ExternalMapRecord exRec : _fbLogic.loadMappedFriends(
            ExternalSiteId.facebookApp(appId), requireAuthedUser(), true, 0)) {
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
        int appId = session.siteId.getFacebookAppId();
        List<FacebookTemplateRecord> templates = _facebookRepo.loadVariants(appId, template);
        if (templates.size() == 0) {
            log.warning("No Facebook templates found for request", "code", template);
            return fields;
        }
        FacebookInfoRecord fbinfo = _facebookRepo.loadAppFacebookInfo(appId);
        fields.canvasName = fbinfo != null ? fbinfo.canvasName : "";
        fields.template = RandomUtil.pickRandom(templates).toTemplate();
        TrackingId trackingId = new KontagentLogic.TrackingId(LinkType.FEED_LONG,
            template + fields.template.variant, session.fbid);
        fields.trackingId = trackingId.flatten();
        fields.fbuid = session.fbid;
        return fields;
    }

    protected StoryFields loadGameStoryFields (
        StoryFields fields, int appId, FacebookGame game, String code)
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
            if (code != null) {
                fields.thumbnails = assembleThumbnails(
                    appId, code, info.getShotMedia().getMediaPath(), game.getIntId());
            }
            return fields;

        case MOCHI:
            // mochi game invite
            MochiGameInfo minfo = _mgameRepo.loadMochiGame(game.getStringId());
            if (minfo == null) {
                throw new ServiceException();
            }
            fields.name = minfo.name;
            fields.description = minfo.desc;
            if (code != null) {
                fields.thumbnails = assembleThumbnails(appId, code, minfo.thumbURL, 0);
            }
            return fields;
        }
        throw new ServiceException();
    }

    protected List<String> assembleThumbnails (
        int appId, String code, String gameMain, int gameId)
    {
        List<String> result = loadThumbnails(code, gameId, appId);

        if (result.size() >= 3) {
            // we have all 3 thumbnails, ship it
            CollectionUtil.limit(result, 3);

        } else if (gameId != 0) {
            // the game didn't override, try the app-defined ones
            result = loadThumbnails(code, 0, appId);

            if (result.size() < 3) {
                // eek, not enough app ones, just use nothing or fallback to the game's main one!
                result.clear();
                if (gameMain != null) {
                    result.add(gameMain);
                }
            }
        } else {
            // not enough globals... not much we can do here
        }
        if (DeploymentConfig.devDeployment) {
            log.info("Assembled thumbnails", "code", code, "gameId", gameId, "result", result);
        }
        return result;
    }

    protected List<String> loadThumbnails (String code, int gameId, int appId)
    {
        List<FeedThumbnailRecord> thumbnails = gameId == 0 ?
            _facebookRepo.loadAppThumbnails(code, appId) :
            _facebookRepo.loadGameThumbnails(code, gameId);
        Set<String> variants = Sets.newHashSet();
        for (FeedThumbnailRecord thumb : thumbnails) {
            variants.add(thumb.variant);
        }
        if (variants.size() == 0) {
            return Lists.newArrayList();
        }
        Iterable<FeedThumbnailRecord> result = thumbnails;
        if (variants.size() > 1) {
            // TODO: pick the LRU variant based on user history
            final String variant = RandomUtil.pickRandom(variants);
            result = Iterables.filter(result, new Predicate<FeedThumbnailRecord>() {
                @Override public boolean apply (FeedThumbnailRecord thumb) {
                    return thumb.variant.equals(variant);
                }
            });
        }
        return Lists.newArrayList(Iterables.transform(result, FeedThumbnailRecord.TO_MEDIA_PATH));
    }

    protected SessionInfo requireSession (int appId)
        throws ServiceException
    {
        return _fbLogic.loadSessionInfo(ExternalSiteId.facebookApp(appId), requireAuthedUser());
    }

    @Inject protected AppRepository _appRepo;
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
