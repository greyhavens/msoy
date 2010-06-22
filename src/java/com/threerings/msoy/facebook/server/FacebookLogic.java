//
// $Id$

package com.threerings.msoy.facebook.server;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Calendars;
import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.ProfileField;
import com.google.code.facebookapi.schema.FriendsGetResponse;
import com.google.code.facebookapi.schema.Location;
import com.google.code.facebookapi.schema.User;
import com.google.code.facebookapi.schema.UsersGetStandardInfoResponse;

import com.threerings.cron.server.CronLogic;
import com.threerings.facebook.FQL;
import com.threerings.facebook.FQLQuery;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.apps.server.persist.AppInfoRecord;
import com.threerings.msoy.apps.server.persist.AppRepository;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.facebook.data.FacebookCodes;
import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.server.KontagentLogic.TrackingId;
import com.threerings.msoy.facebook.server.persist.FacebookActionRecord;
import com.threerings.msoy.facebook.server.persist.FacebookInfoRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.facebook.server.persist.ListRepository;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.LevelFinder;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.server.persist.ExternalMapRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ArgNames.FBParam;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.ExternalSiteId;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import static com.threerings.msoy.Log.log;

/**
 * Centralizes some Facebook API bits.
 * TODO: write a batch process that does some bookkeeping once a day or something:
 *   1 - Get each user's info up to once per week and update their profile (if it hasn't been
 *       updated manually on Whirled)
 *   2 - Update each user's friend list up to once per week
 *   3 - Check if the user has removed the app and if so delete the ExternalMapRecord and queue up
 *       the account for deletion.
 */
@Singleton
public class FacebookLogic
{
    /**
     * Some useful things to be able to pass around when doing things with a user's session.
     */
    public static class SessionInfo
    {
        /** Facebook user id. */
        public Long fbid;

        /** The external mapping. */
        public ExternalMapRecord mapRec;

        /** The member record. */
        public MemberRecord memRec;

        /** The site we are serving up. */
        public ExternalSiteId siteId;

        /** The client, only if requested. */
        public FacebookJaxbRestClient client;
    }

    /**
     * Prepends the Facebook application root to get the canvas url of the given name.
     */
    public static String getCanvasUrl (String canvasName)
    {
        return "http://apps.facebook.com/" + canvasName + "/";
    }

    /**
     * Parses a Facebook birthday, which may or may not include a year, and returns the year and
     * date in a tuple. If the year is null, the year in the date is not valid and will just be
     * the default given by the date format parser. If the year is not null, it will match the year
     * in the date (using a default local calendar). The date and year may both be null if the
     * given string could not be parsed as a date.
     */
    public static Tuple<Integer, Date> parseBirthday (String bday)
    {
        Date date = null;
        Integer year = null;
        if (bday != null) {
            try {
                date = BDAY_FMT.parse(bday);
                year = Calendars.at(date).get(Calendar.YEAR);

            } catch (Exception e) {
                try {
                    // this will end up with year set to 1970, but at least we'll get the
                    // month and day
                    date = BDAY_FMT_NO_YEAR.parse(bday);

                } catch (Exception e2) {
                    log.info("Could not parse Facebook birthday", "bday", bday);
                }
            }
        }

        return Tuple.newTuple(year, date);
    }

    @Inject public FacebookLogic (CronLogic cronLogic, Lifecycle lifecycle)
    {
        // run the demographics update between 4 and 5 am
        cronLogic.scheduleAt(4, "FacebookLogic demographics", new Runnable() {
            public void run () {
                updateDemographics();
            }
        });

        // run the featured game update between 11am and 12pm
        cronLogic.scheduleAt(11, "FacebookLogic roll games", new Runnable() {
            public void run () {
                updateFeaturedGames(false);
            }
        });

        lifecycle.addComponent(new Lifecycle.ShutdownComponent() {
            public void shutdown () {
                _shutdown = true;
            }
        });
    }

    /**
     * Returns a Facebook client not bound to any particular user's session with the deafault read
     * timeout.
     */
    public FacebookJaxbRestClient getFacebookClient (ExternalSiteId siteId)
    {
        return getFacebookClient(siteId, READ_TIMEOUT);
    }

    /**
     * Returns a Facebook client not bound to any particular user's session with the given read
     * timeout.
     */
    public FacebookJaxbRestClient getFacebookClient (ExternalSiteId siteId, int timeout)
    {
        return getFacebookClient(siteId, (String)null, timeout);
    }

    /**
     * Returns a Facebook client bound to the supplied user's session.
     */
    public FacebookJaxbRestClient getFacebookClient (FacebookCreds creds)
    {
        return getFacebookClient(creds.getSite(), creds.sessionKey);
    }

    /**
     * Returns a Facebook client bound to the supplied user's session.
     */
    public FacebookJaxbRestClient getFacebookClient (ExternalSiteId siteId, String sessionKey)
    {
        return getFacebookClient(siteId, sessionKey, 0);
    }

    /**
     * Returns a Facebook client bound to the supplied user's session with the supplied read
     * timeout.
     */
    public FacebookJaxbRestClient getFacebookClient (
        ExternalSiteId siteId, String sessionKey, int timeout)
    {
        timeout = timeout == 0 ? READ_TIMEOUT : timeout;
        FacebookInfoRecord fbinfo = loadSiteInfo(siteId);
        if (StringUtil.isBlank(fbinfo.apiKey)) {
            throw new IllegalStateException("Missing api_key for site " + siteId);
        }
        if (StringUtil.isBlank(fbinfo.appSecret)) {
            throw new IllegalStateException("Missing secrect for site " + siteId);
        }
        return getFacebookClient(fbinfo.apiKey, fbinfo.appSecret, sessionKey, timeout);
    }

    /**
     * Returns a Facebook client for the app represented by the supplied creds.
     */
    public FacebookJaxbRestClient getFacebookClient (FacebookAppCreds creds)
    {
        return getFacebookClient(creds.apiKey, creds.appSecret, creds.sessionKey, READ_TIMEOUT);
    }

    /**
     * Returns the site that refers to the default games portal site. This is needed for things
     * like posting game plays from Whirled to FB connect accounts and sending global notifications
     * when the featured games are updated.
     */
    public ExternalSiteId getDefaultGamesSite ()
    {
        return _defaultSite;
    }

    /**
     * Gets the url of the canvas of the given external site, or null if the site is not a
     * Facebook site or does not have a canvas name set.
     */
    public String getCanvasUrl (ExternalSiteId siteId)
    {
        if (siteId.auther != ExternalSiteId.Auther.FACEBOOK) {
            return null;
        }

        FacebookInfoRecord fbinfo = loadSiteInfo(siteId);
        if (fbinfo == null || StringUtil.isBlank(fbinfo.canvasName)) {
            return null;
        }
        return getCanvasUrl(fbinfo.canvasName);
    }

    /**
     * Loads up the session info for the given member, without initializing the jaxb client.
     */
    public SessionInfo loadSessionInfo (ExternalSiteId siteId, MemberRecord mrec)
        throws ServiceException
    {
        return loadSessionInfo(siteId, mrec, 0);
    }

    /**
     * Loads the facebook application info for the given external site.
     * @throws IllegalArgumentException if the site is not a facebook site
     * @throws RuntimeException if the info could not be loaded
     */
    public FacebookInfoRecord loadSiteInfo (ExternalSiteId siteId)
    {
        Preconditions.checkArgument(siteId.auther == ExternalSiteId.Auther.FACEBOOK);
        Integer appId = siteId.getFacebookAppId(), gameId = siteId.getFacebookGameId();
        Preconditions.checkArgument(gameId == null ^ appId == null);

        FacebookInfoRecord fbinfo;
        if (appId != null) {
            fbinfo = _facebookRepo.loadAppFacebookInfo(appId);
        } else {
            fbinfo = _facebookRepo.loadGameFacebookInfo(gameId);
        }
        if (fbinfo == null) {
            throw new RuntimeException("Facebook info not found [siteId=" + siteId + "]");
        }
        return fbinfo;
    }

    /**
     * Using standard parameter values, returns the whirled or mochi game associated with the given
     * request, or null if there is not one.
     */
    public FacebookGame parseGame (HttpServletRequest req)
    {
        if (req.getParameter(FBParam.GAME.name) != null) {
            return new FacebookGame(Integer.parseInt(req.getParameter(FBParam.GAME.name)));

        } else if (req.getParameter(FBParam.MOCHI_GAME.name) != null) {
            return new FacebookGame(req.getParameter(FBParam.MOCHI_GAME.name));
        }
        return null;
    }

    /**
     * Loads the map records for all the given member's facebook friends. Throws an exception if
     * the session isn't valid or if the facebook friends could not be loaded.
     * @param limit maximum number of friends to retrieve, or 0 to retrieve all
     */
    public List<ExternalMapRecord> loadMappedFriends (
        ExternalSiteId siteId, MemberRecord mrec, boolean includeSelf, int limit)
        throws ServiceException
    {
        SessionInfo sinf = loadSessionInfo(siteId, mrec, CONNECT_TIMEOUT);

        // get facebook friends to seed (more accurate)
        Set<String> facebookFriendIds = Sets.newHashSet();
        try {
            for (Long uid : sinf.client.friends_get().getUid()) {
                facebookFriendIds.add(String.valueOf(uid));
            }

        } catch (FacebookException fe) {
            log.warning("Unable to get facebook friends", "memberId", mrec.memberId, fe);
            // pass along the translated text for now
            throw new ServiceException(fe.getMessage());
        }

        // filter by those hooked up to the site and tack on self if appropriate
        List<ExternalMapRecord> exRecs =
            _memberRepo.loadExternalAccounts(siteId, facebookFriendIds);
        if (includeSelf) {
            exRecs = Lists.newArrayList(exRecs);
            exRecs.add(sinf.mapRec);
        }

        if (limit == 0) {
            return exRecs;
        }

        // TODO: remove shuffle when we actually optimize target selection
        Collections.shuffle(exRecs);
        return limit >= exRecs.size() ? exRecs : exRecs.subList(0, limit);
    }

    /**
     * Sets up the {@link SessionData.Extra} using the given partially filled-in data, member and
     * money. May also award coins, record this as daily visit, and update relevant fields in money
     * and data.
     */
    public void initSessionData (
        ExternalSiteId siteId, MemberRecord mrec, MemberMoney money, SessionData data)
    {
        if (siteId.getFacebookAppId() == null) {
            return;
        }

        data.extra = new SessionData.Extra();

        int memberId = mrec.memberId;

        // determine if an award is due, find lastLevel
        long now = System.currentTimeMillis();
        int lastAward = -1; // suppress award by default
        int lastLevel = 0;
        if (mrec.created.getTime() < now - NEW_ACCOUNT_TIME) {
            FacebookActionRecord lastVisit = _facebookRepo.getLastAction(
                siteId.getFacebookAppId(), memberId, FacebookActionRecord.Type.DAILY_VISIT);
            if (lastVisit == null) {
                lastAward = 0; // trigger a 1st visit award
                lastLevel = data.level; // trigger level-up if subsequent gain occurs

            } else if (lastVisit.timestamp.getTime() < now - MIN_AWARD_PERIOD) {
                Tuple<Integer, Integer> lastStuff = lastVisit.extractCoinAwardAndLevel();
                lastAward = lastStuff.left;
                lastLevel = lastStuff.right;
            }
        }

        // calculate award index, based on last one
        int awardIdx = -1;
        if (lastAward == 0) {
            // 1st visit, grant full award
            awardIdx = VISIT_AWARDS.length - 1;

        } else if (lastAward > 0) {
            // bump down 1 level
            awardIdx = Arrays.binarySearch(VISIT_AWARDS, lastAward);
            awardIdx = (awardIdx < 0) ? -(awardIdx + 1) : awardIdx;
            awardIdx = Math.max(awardIdx - 1, 0);
        }

        int award = awardIdx >= 0 ? VISIT_AWARDS[awardIdx] : 0;
        if (award > 0) {
            AppInfoRecord appInfo = _appRepo.loadAppInfo(siteId.getFacebookAppId());

            // award the coins; note this eventually calls synchMemberLevel
            _moneyLogic.awardCoins(memberId, award, true, UserAction.visitedFBApp(
                memberId, appInfo != null ? appInfo.name : ""));

            // shortcut, just update changed fields directly rather than reload frmo DB
            money.coins += award;
            money.accCoins += award;
            data.level = _memberLogic.getLevelFinder().findLevel((int)money.accCoins);

            // let the client know
            data.extra.flowAwarded = award;

            // record the daily visit
            _facebookRepo.recordAction(FacebookActionRecord.dailyVisit(
                siteId.getFacebookAppId(), memberId, award, data.level));

        } else {
            // update the level (it may have changed due to game play or other stuff)
            data.level = _memberLogic.synchMemberLevel(memberId, mrec.level, money.accCoins);
        }

        // set up facebook status fields
        data.extra.accumFlow = (int)Math.min(money.accCoins, Integer.MAX_VALUE);
        LevelFinder levelFinder = _memberLogic.getLevelFinder();
        // level 1 users should see "1000/1800" instead of "-500/300"
        data.extra.levelFlow = data.level <= 1 ? 0 : levelFinder.getCoinsForLevel(data.level);
        data.extra.nextLevelFlow = levelFinder.getCoinsForLevel(data.level + 1);
        data.extra.trophyCount = _trophyRepo.countTrophies(memberId);

        // level-uppance - note we only show this for people who have had a daily visit since the
        // award system was rolled out *or* gained a level as a result of the award, so older users
        // don't see something like "you have just reached level 5" when they haven't
        if (lastLevel != 0 && lastLevel != data.level) {
            data.extra.levelsGained = data.level - lastLevel;
        }
    }

    /**
     * Sets the sequence of mochi game tags to cycle through for the given bucket number.
     */
    public void setMochiGames (int bucket, List<String> tags)
    {
        String listId = MOCHI_BUCKETS[bucket - 1];
        _listRepo.setList(listId, tags);
        _listRepo.advanceCursor(listId, MOCHI_CURSOR);
    }

    /**
     * Gets the sequence of mochi game tags being cycled for the given bucket number.
     */
    public List<String> getMochiGames (int bucket)
    {
        return _listRepo.getList(MOCHI_BUCKETS[bucket -1], false);
    }

    /**
     * Gets the tag of the currently featured mochi game for the given bucket.
     */
    public int getCurrentGameIndex (int bucket)
    {
        Integer index = _listRepo.getCursorIndex(MOCHI_BUCKETS[bucket -1], MOCHI_CURSOR);
        return index != null ? index : 0;
    }

    /**
     * Gets the 5 games to be featured on the Facebook games portal.
     */
    public List<String> getFeaturedGames ()
    {
        return _listRepo.getCursorItems(Arrays.asList(MOCHI_BUCKETS), MOCHI_CURSOR);
    }

    public void testUpdateFeaturedGames ()
    {
        updateFeaturedGames(true);
    }

    protected void updateURL (Map<String, String> replacements, String varName,
        MemberRecord memRec, TrackingId trackingId)
    {
        String url = replacements.get(varName);
        if (url != null) {
            if (memRec != null) {
                url = SharedNaviUtil.buildRequest(url,
                    CookieNames.AFFILIATE, String.valueOf(memRec.memberId));
            }
            replacements.put(varName, url = SharedNaviUtil.buildRequest(url,
                FBParam.TRACKING.name, trackingId.flatten()));
        }
    }

    protected FacebookJaxbRestClient getFacebookClient (
        String apiKey, String appSecret, String sessionKey, int timeout)
    {
        FacebookJaxbRestClient client = new FacebookJaxbRestClient(apiKey, appSecret, sessionKey);
        client.setServerUrl(SERVER_URL);
        client.setConnectTimeout(CONNECT_TIMEOUT);
        client.setReadTimeout(timeout);
        return client;
    }

    /**
     * Loads up the session info for the given member, initializing the jaxb client with the given
     * timeout if it is not zero, otherwise leaving it set to null.
     */
    protected SessionInfo loadSessionInfo (ExternalSiteId siteId, MemberRecord mrec, int timeout)
        throws ServiceException
    {
        SessionInfo sinf = new SessionInfo();
        sinf.memRec = mrec;
        sinf.mapRec = _memberRepo.loadExternalMapEntry(siteId, mrec.memberId);
        sinf.siteId = siteId;
        if (sinf.mapRec == null || sinf.mapRec.sessionKey == null) {
            throw new ServiceException(FacebookCodes.NO_SESSION);
        }
        sinf.fbid = Long.valueOf(sinf.mapRec.externalId);
        if (timeout > 0) {
            sinf.client = getFacebookClient(siteId, sinf.mapRec.sessionKey, timeout);
        }
        return sinf;
    }

    protected List<Long> loadFriends (ExternalSiteId siteId, MemberRecord mrec)
        throws ServiceException
    {
        SessionInfo sinf = loadSessionInfo(siteId, mrec, CONNECT_TIMEOUT);
        try {
            return sinf.client.friends_get().getUid();

        } catch (FacebookException fe) {
            log.warning("Unable to get facebook friends", "memberId", mrec.memberId, fe);
            // pass along the translated text for now
            throw new ServiceException(fe.getMessage());
        }
    }

    protected void updateDemographics ()
    {
        for (AppInfoRecord appInfo : _appRepo.loadApps()) {
            FacebookInfoRecord fbinfo = _facebookRepo.loadAppFacebookInfo(appInfo.appId);
            if (fbinfo == null) {
                continue;
            }
            final ExternalSiteId siteId = ExternalSiteId.facebookApp(appInfo.appId);
            List<ExternalMapRecord> users = _memberRepo.loadExternalMappings(siteId);
            log.info("Starting demographics update", "siteId", siteId, "users", users.size());

            final FacebookJaxbRestClient client = getFacebookClient(siteId);
            final int BATCH_SIZE = 200;
            List<Integer> results = segment(users,
                new Function<List<ExternalMapRecord>, Integer> () {
                public Integer apply (List<ExternalMapRecord> batch) {
                    if (_shutdown) {
                        return 0;
                    }
                    return updateDemographics(siteId, client, batch);
                }
            }, BATCH_SIZE);

            int count = 0;
            for (int result : results) {
                count += result;
            }

            log.info("Finished demographics update", "siteId", siteId, "count", count);
        }
    }

    /**
     * Gets demographic data from Facebook and sends it to Kongagent for a given segment of users.
     * Users whose demographic data has been previously uploaded are culled.
     */
    protected int updateDemographics (
        ExternalSiteId siteId, FacebookJaxbRestClient client, List<ExternalMapRecord> users)
    {
        int appId = siteId.getFacebookAppId();

        // create mapping of member id to external record
        Map<Integer, ExternalMapRecord> fbusers = Maps.newHashMap();
        for (ExternalMapRecord exrec : users) {
            fbusers.put(exrec.memberId, exrec);
        }

        // remove ones that we've done before
        // TODO: should probably limit to records less than a certain age so that data gets
        // refreshed from time to time... or just prune old records
        for (FacebookActionRecord action : _facebookRepo.loadActions(appId, Lists.transform(
            users, MAPREC_TO_MEMBER_ID), FacebookActionRecord.Type.GATHERED_DATA)) {
            fbusers.remove(action.memberId);
        }

        if (fbusers.size() == 0) {
            return 0;
        }

        // create mapping of facebook user id to member id
        Map<Long, Integer> memberIds = Maps.newHashMap();
        for (ExternalMapRecord rec : fbusers.values()) {
            memberIds.put(MAPREC_TO_UID.apply(rec), rec.memberId);
        }

        // retrieve the data
        Set<ProfileField> fields = EnumSet.of(
            ProfileField.SEX, ProfileField.BIRTHDAY, ProfileField.CURRENT_LOCATION);
        Collection<Long> uids = Collections2.transform(fbusers.values(), MAPREC_TO_UID);
        UsersGetStandardInfoResponse uinfo;
        try {
            uinfo = client.users_getStandardInfo(uids, fields);
        } catch (FacebookException ex) {
            log.warning("Failed to getStandardInfo", "uids", uids,
                "rawResponse", client.getRawResponse(), ex);
            return 0;
        }

        // extract and track the data for each user
        for (User user : uinfo.getUser()) {
            Long uid = user.getUid();

            // gender
            String gender = user.getSex();

            // location
            String city = null, state = null, zip = null, country = null;
            Location location = user.getCurrentLocation();
            if (location != null) {
                city = location.getCity();
                state = location.getState();
                zip = location.getZip();
                country = location.getCountry();
            }

            // birthday
            Integer birthYear = parseBirthday(user.getBirthday()).left;

            // friend count
            Integer friendCount = null;
            try {
                FriendsGetResponse finfo = client.friends_get(uid);
                friendCount = finfo.getUid().size();

            } catch (FacebookException e) {
                log.warning("Could not retrieve friend count", "uid", uid,
                    "rawResponse", client.getRawResponse(), e);
            }

            // send them to the tracker and note that we've done so
            _tracker.trackUserInfo(appId, uid, birthYear, gender, city, state, zip, country,
                friendCount);
            _facebookRepo.recordAction(
                FacebookActionRecord.dataGathered(appId, memberIds.get(uid)));
        }

        return uinfo.getUser().size();
    }

    protected void updateFeaturedGames (boolean test)
    {
        for (String bucket : MOCHI_BUCKETS) {
            _listRepo.advanceCursor(bucket, MOCHI_CURSOR);
        }
    }

    /**
     * Divides up a list into segements and calls the given function on each one. Returns a list
     * of return values of the segments.
     */
    protected static <F, T> List<T> segment (
        List<F> list, Function<List<F>, T> fn, int maxLength)
    {
        int size = list.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        List<T> results = Lists.newArrayListWithCapacity((size - 1) / maxLength + 1);
        for (int ii = 0; ii < size; ii += maxLength) {
            results.add(fn.apply(list.subList(ii, Math.min(ii + maxLength, size))));
        }
        return results;
    }

    // assume the default Whirled Games app is 1. This is far easier than anything else I tried.
    protected ExternalSiteId _defaultSite = ExternalSiteId.facebookApp(1);
    protected boolean _shutdown;

    protected static final Function<ExternalMapRecord, FQL.Exp> MAPREC_TO_UID_EXP =
        new Function<ExternalMapRecord, FQL.Exp>() {
        public FQL.Exp apply (ExternalMapRecord exRec) {
            return FQL.unquoted(exRec.externalId);
        }
    };

    protected static final Function<ExternalMapRecord, Long> MAPREC_TO_UID =
        new Function<ExternalMapRecord, Long>() {
        public Long apply (ExternalMapRecord exRec) {
            return Long.valueOf(exRec.externalId);
        }
    };

    public static final Function<ExternalMapRecord, Integer> MAPREC_TO_MEMBER_ID =
        new Function<ExternalMapRecord, Integer>() {
        @Override public Integer apply (ExternalMapRecord exrec) {
            return exrec.memberId;
        }
    };

    protected static final Predicate<FQLQuery.Record> APP_USER_FILTER =
        new Predicate<FQLQuery.Record>() {
        public boolean apply (FQLQuery.Record result) {
            return result.getField(IS_APP_USER).equals("1");
        }
    };

    protected static final FQL.Field UID = new FQL.Field("uid");
    protected static final FQL.Field IS_APP_USER = new FQL.Field("is_app_user");
    protected static final String USERS_TABLE = "user";

    protected static final int CONNECT_TIMEOUT = 15*1000; // in millis
    protected static final int READ_TIMEOUT = 15*1000; // in millis
    protected static final int BATCH_READ_TIMEOUT = 5*60*1000; // 5 minutes
    protected static final SimpleDateFormat BDAY_FMT = new SimpleDateFormat("MMMM dd, yyyy");
    protected static final SimpleDateFormat BDAY_FMT_NO_YEAR = new SimpleDateFormat("MMMM dd");
    protected static final SimpleDateFormat BATCH_FMT = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss");

    /** Amount of time after the app is installed that we will start granting visit rewards. This
     *  is because new users automatically get 1000 coins for joining. */
    protected static final long NEW_ACCOUNT_TIME = 20 * 60 * 60 * 1000; // 20 hours

    /** Minimum amount of time between visit rewards. Use 20 hours so that there is some leeway for
     *  people to visit at the same approximate time each day but not generally get more than one
     *  reward per day. */
    protected static final long MIN_AWARD_PERIOD = 20 * 60 * 60 * 1000; // 20 hours

    /** Coin awards for visiting the app, in reverse order. I.e. 1st visit rewards is in last
     *  slot. */
    protected static final int VISIT_AWARDS[] = {200, 400, 600, 800};

    protected static final URL SERVER_URL;
    static {
        try {
            SERVER_URL = new URL("http://api.facebook.com/restserver.php");
        } catch (Exception e) {
            throw new RuntimeException(e); // MalformedURLException should be unchecked, sigh
        }
    }

    /** Bucket ids for defining the featured mochi game rotation. */
    protected static final String[] MOCHI_BUCKETS = new String[5];
    static {
        for (int ii = 1; ii <= MOCHI_BUCKETS.length; ++ii) {
            MOCHI_BUCKETS[ii-1] = "MochiBucket" + ii;
        }
    }

    protected static final String MOCHI_CURSOR = "";
    protected static final String DAILY_GAMES_CURSOR = "";

    // dependencies
    @Inject protected @BatchInvoker Invoker _batchInvoker;
    @Inject protected AppRepository _appRepo;
    @Inject protected FacebookRepository _facebookRepo;
    @Inject protected KontagentLogic _tracker;
    @Inject protected ListRepository _listRepo;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyPeerManager _peerMgr;
    @Inject protected RuntimeConfig _runtime; // temp
    @Inject protected TrophyRepository _trophyRepo;
}
