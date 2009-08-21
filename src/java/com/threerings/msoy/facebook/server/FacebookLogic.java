//
// $Id$

package com.threerings.msoy.facebook.server;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.facebook.data.FacebookCodes;
import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.server.persist.FacebookNotificationRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.server.persist.ExternalMapRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import static com.threerings.msoy.Log.log;

/**
 * Centralizes some Facebook API bits.
 */
@Singleton
public class FacebookLogic
{
    /** URL of the main application entry point. */
    public static final String WHIRLED_APP_CANVAS =
        getCanvasUrl(DeploymentConfig.facebookCanvasName);

    /** URL of the main application's profile page. */
    public static final String WHIRLED_APP_PROFILE = SharedNaviUtil.buildRequest(
        "http://www.facebook.com/apps/application.php",
        "id", DeploymentConfig.facebookApplicationId);

    /** Tracking prefix for a game invite (challenge). */
    public static final String TRACKING_GAME_INVITE = "invg";

    /** Tracking prefix for an app invite. */
    public static final String TRACKING_APP_INVITE = "inva";

    /**
     * Prepends the Facebook application root to get the canvas url of the given name.
     */
    public static String getCanvasUrl (String canvasName)
    {
        return "http://apps.facebook.com/" + canvasName + "/";
    }

    /**
     * Generates a tracking id value and assembles it in the usual format that can be parsed later
     * when a user requests a page with the {@link ArgNames#FB_PARAM_TRACKING} parameter assigned.
     */
    public static String generateInviteTrackingId (FacebookGame game, long uid)
    {
        return (game == null ? TRACKING_APP_INVITE : TRACKING_GAME_INVITE + "-" +
            game.getStringId()) + "-" + KontagentLogic.genUUID(uid);
    }

    /**
     * Returns a Facebook client not bound to any particular user's session with the deafault read
     * timeout.
     */
    public FacebookJaxbRestClient getFacebookClient ()
    {
        return getFacebookClient(READ_TIMEOUT);
    }

    /**
     * Returns a Facebook client not bound to any particular user's session with the given read
     * timeout.
     */
    public FacebookJaxbRestClient getFacebookClient (int timeout)
    {
        return getFacebookClient((String)null, timeout);
    }

    /**
     * Returns a Facebook client bound to the supplied user's session.
     */
    public FacebookJaxbRestClient getFacebookClient (FacebookCreds creds)
    {
        return getFacebookClient(creds.sessionKey);
    }

    /**
     * Returns a Facebook client bound to the supplied user's session.
     */
    public FacebookJaxbRestClient getFacebookClient (String sessionKey)
    {
        return getFacebookClient(requireAPIKey(), requireSecret(), sessionKey, READ_TIMEOUT);
    }

    /**
     * Returns a Facebook client bound to the supplied user's session with the supplied read
     * timeout.
     */
    public FacebookJaxbRestClient getFacebookClient (String sessionKey, int timeout)
    {
        return getFacebookClient(requireAPIKey(), requireSecret(), sessionKey, timeout);
    }

    /**
     * Returns a Facebook client for the app represented by the supplied creds.
     */
    public FacebookJaxbRestClient getFacebookClient (FacebookAppCreds creds)
    {
        return getFacebookClient(creds.apiKey, creds.appSecret, creds.sessionKey, READ_TIMEOUT);
    }

    /**
     * Using standard parameter values, returns the whirled or mochi game associated with the given
     * request, or null if there is not one.
     */
    public FacebookGame parseGame (HttpServletRequest req)
    {
        if (req.getParameter(ArgNames.FB_PARAM_GAME) != null) {
            return new FacebookGame(Integer.parseInt(req.getParameter(ArgNames.FB_PARAM_GAME)));

        } else if (req.getParameter(ArgNames.FB_PARAM_MOCHI_GAME) != null) {
            return new FacebookGame(req.getParameter(ArgNames.FB_PARAM_MOCHI_GAME));
        }
        return null;
    }

    /**
     * Schedules or reschedules a notification to be posted to all Facebook users. If a
     * notification already exists with the given id, it is rescheduled and the content
     * overwritten.
     * @param delay number of minutes to wait before sending the notification
     */
    public void scheduleNotification (String id, int delay)
        throws ServiceException
    {
        // check preconditions
        FacebookNotificationRecord notifRec = _facebookRepo.loadNotification(id);
        if (notifRec.node != null && !notifRec.node.equals(_peerMgr.getNodeObject().nodeName)) {
            throw new ServiceException("e.notification_scheduled_on_other_node");
        }
        if (StringUtil.isBlank(notifRec.text)) {
            throw new ServiceException("e.notification_blank_text");
        }

        schedule(id, new NotificationBatch(notifRec), delay);
    }

    /**
     * Schedules a notification to be sent immediately to a subset of the Facebook friends of a
     * given user. If a notification of the given id is already active for that user, an exception
     * is thrown.
     */
    public void scheduleFriendNotification (MemberRecord mrec, String notificationId,
        Map<String, String> replacements, boolean appOnly)
        throws ServiceException
    {
        // throw if there is no session
        SessionInfo sinf = getSessionInfo(mrec, 0);

        // load up & verify the template
        FacebookNotificationRecord notifRec = _facebookRepo.loadNotification(notificationId);
        if (notifRec == null) {
            log.warning("Unable to load notification", "id", notificationId);
            return;
        }
        if (StringUtil.isBlank(notifRec.text)) {
            log.warning("Blank notification", "id", notificationId);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        // check we are not already sending a batch from this user
        String batchId = notificationId + "." + mrec.memberId;
        FacebookNotificationRecord instance = _facebookRepo.loadNotification(batchId);
        if (instance != null && instance.node != null) {
            log.warning("Requested notification still sending", "id", instance.id);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        // add some standard replacements
        replacements.put("uid", sinf.fbid.toString());
        String gameUrl = replacements.get("game_url");
        if (gameUrl != null) {
            replacements.put("game_url", gameUrl = SharedNaviUtil.buildRequest(gameUrl,
                CookieNames.AFFILIATE, String.valueOf(sinf.memRec.memberId)));
        }

        // do the replacements and create the instance
        String text = notifRec.text;
        for (Map.Entry<String, String> pair : replacements.entrySet()) {
            String key = "{*" + pair.getKey() + "*}";
            text = text.replace(key, pair.getValue());
        }
        instance = _facebookRepo.storeNotification(batchId, text);

        // kick it off now
        schedule(batchId, new NotificationBatch(instance, mrec, appOnly), 0);
    }

    /**
     * Loads the map records for all the given member's facebook friends. Throws an exception if
     * the session isn't valid or if the facebook friends could not be loaded.
     * @param limit maximum number of friends to retrieve, or 0 to retrieve all
     */
    public List<ExternalMapRecord> loadMappedFriends (
        MemberRecord mrec, boolean includeSelf, int limit)
        throws ServiceException
    {
        SessionInfo sinf = getSessionInfo(mrec);

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

        // filter by those hooked up to Whirled and
        List<ExternalMapRecord> exRecs = _memberRepo.loadExternalAccounts(
            ExternalAuther.FACEBOOK, facebookFriendIds);
        if (includeSelf) {
            exRecs.add(sinf.mapRec);
        }

        return (limit == 0 || limit >= exRecs.size()) ? exRecs : exRecs.subList(0, limit);
    }

    protected void schedule (String id, NotificationBatch notif, int delay)
    {
        // convert to millis
        long millis = delay * 60 * 1000L;

        // insert into map and schedule
        synchronized (_notifications) {
            NotificationBatch prior = _notifications.get(id);
            if (prior != null) {
                prior.cancel();
            }
            _notifications.put(id, notif);
            notif.schedule(millis);
        }
    }

    protected void removeNotification (String id)
    {
        synchronized (_notifications) {
            _notifications.remove(id);
        }
    }

    protected FacebookJaxbRestClient getFacebookClient (
        String apiKey, String appSecret, String sessionKey, int timeout)
    {
        return new FacebookJaxbRestClient(
            SERVER_URL, apiKey, appSecret, sessionKey, CONNECT_TIMEOUT, timeout);
    }

    protected String requireAPIKey ()
    {
        String apiKey = ServerConfig.config.getValue("facebook.api_key", "");
        if (StringUtil.isBlank(apiKey)) {
            throw new IllegalStateException("Missing facebook.api_key server configuration.");
        }
        return apiKey;
    }

    protected String requireSecret ()
    {
        String secret = ServerConfig.config.getValue("facebook.secret", "");
        if (StringUtil.isBlank(secret)) {
            throw new IllegalStateException("Missing facebook.secret server configuration.");
        }
        return secret;
    }

    protected SessionInfo getSessionInfo (MemberRecord mrec)
        throws ServiceException
    {
        return getSessionInfo(mrec, CONNECT_TIMEOUT);
    }

    protected SessionInfo getSessionInfo (MemberRecord mrec, int timeout)
        throws ServiceException
    {
        SessionInfo sinf = new SessionInfo();
        sinf.memRec = mrec;
        sinf.mapRec = _memberRepo.loadExternalMapEntry(ExternalAuther.FACEBOOK, mrec.memberId);
        if (sinf.mapRec == null || sinf.mapRec.sessionKey == null) {
            throw new ServiceException(FacebookCodes.NO_SESSION);
        }
        sinf.fbid = Long.valueOf(sinf.mapRec.externalId);
        if (timeout > 0) {
            sinf.client = getFacebookClient(sinf.mapRec.sessionKey, timeout);
        }
        return sinf;
    }

    protected List<Long> loadFriends (MemberRecord mrec)
        throws ServiceException
    {
        SessionInfo sinf = getSessionInfo(mrec);
        try {
            return sinf.client.friends_get().getUid();

        } catch (FacebookException fe) {
            log.warning("Unable to get facebook friends", "memberId", mrec.memberId, fe);
            // pass along the translated text for now
            throw new ServiceException(fe.getMessage());
        }
    }

    protected <T> void sendNotifications (FacebookJaxbRestClient client, String id, String text,
        List<T> batch, Function<T, FQL.Exp> toUserId, Predicate<FQLQuery.Record> filter)
    {
        try {
            _facebookRepo.noteNotificationProgress(id, "Verifying", 0, 0);

            List<FQL.Exp> uids = Lists.transform(batch, toUserId);

            FQLQuery getUsers = new FQLQuery(USERS_TABLE, new FQL.Field[] {UID, IS_APP_USER},
                new FQL.Where(UID.in(uids)));

            List<Long> targetIds = Lists.newArrayListWithCapacity(uids.size());
            for (FQLQuery.Record result : getUsers.run(client)) {
                if (!filter.apply(result)) {
                    continue;
                }
                targetIds.add(Long.valueOf(result.getField(UID)));
            }

            if (targetIds.size() == 0) {
                return;
            }

            _facebookRepo.noteNotificationProgress(id, "Sending", targetIds.size(), 0);
            client.notifications_send(targetIds, text, true);
            _facebookRepo.noteNotificationProgress(id, "Sent", 0, targetIds.size());

        } catch (Exception e) {
            log.warning("Failed to send notifications", "id", id, "targetCount", batch.size(),
                "rawResponse", client.getRawResponse(), e);
            _facebookRepo.noteNotificationProgress(id, "Failed", 0, 0);
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

    /**
     * Some useful things to be able to pass around when doing things with a user's session.
     */
    protected static class SessionInfo
    {
        public Long fbid;
        public ExternalMapRecord mapRec;
        public MemberRecord memRec;
        public FacebookJaxbRestClient client;
    }

    /**
     * A notification batch.
     */
    protected class NotificationBatch
    {
        /**
         * Creates a global notification batch.
         */
        public NotificationBatch (FacebookNotificationRecord notifRec)
        {
            this(notifRec, null, false);
        }

        /**
         * Creates a notification batch targeting the designated subset of a specific memeber's
         * friends, or global if the member is null.
         */
        public NotificationBatch (FacebookNotificationRecord notifRec, MemberRecord mrec,
            boolean appFriendsOnly)
        {
            _notifRec = notifRec;
            _mrec = mrec;
            _appFriendsOnly = appFriendsOnly;
        }

        /**
         * Schedules the batch to run after the given number of milliseconds.
         */
        public void schedule (long delay)
        {
            _interval.schedule(delay, false);
        }

        /**
         * If the notification is scheduled, removes it and stops it from running.
         */
        public void cancel ()
        {
            _interval.cancel();
        }

        protected void send ()
            throws ServiceException
        {
            _facebookRepo.noteNotificationStarted(_notifRec.id);
            int alloc = _runtime.server.fbNotificationsAlloc;

            if (_mrec != null && _appFriendsOnly) {
                // this will only send to the first N users where N is the "notifications_per_day"
                // allocation
                // TODO: fetch the N value on a timer and don't bother sending > N targets
                // TODO: optimize the first N targets?
                // NOTE: wtf? the facebook api is throwing an exception, perhaps because FB
                // have changed the documented behavior of this method... try to limit how many we
                // send, see if exception stops
                List<ExternalMapRecord> targets = loadMappedFriends(_mrec, false, alloc);
                sendNotifications(getSessionInfo(_mrec, BATCH_READ_TIMEOUT).client,
                    _notifRec.id, _notifRec.text, targets, MAPREC_TO_UID_EXP, APP_USER_FILTER);

            } else if (_mrec != null) {
                // see above
                List<Long> targets = loadFriends(_mrec).subList(0, alloc);
                sendNotifications(getSessionInfo(_mrec, BATCH_READ_TIMEOUT).client,
                    _notifRec.id, _notifRec.text, targets, new Function<Long, FQL.Exp> () {
                    public FQL.Exp apply (Long uid) {
                        return FQL.unquoted(uid);
                    }
                }, Predicates.not(APP_USER_FILTER));

            } else {
                final FacebookJaxbRestClient client = getFacebookClient(BATCH_READ_TIMEOUT);
                final int BATCH_SIZE = 100;
                segment(_memberRepo.loadExternalMappings(ExternalAuther.FACEBOOK),
                    new Function<List<ExternalMapRecord>, Void>() {
                        public Void apply (List<ExternalMapRecord> batch) {
                            sendNotifications(client, _notifRec.id, _notifRec.text,
                                batch, MAPREC_TO_UID_EXP, APP_USER_FILTER);
                            return null;
                        }
                    }, BATCH_SIZE);
            }

            _facebookRepo.noteNotificationFinished(_notifRec.id);
            removeNotification(_notifRec.id);
            log.info("Successfully sent notifications", "id", _notifRec.id);
        }

        protected FacebookNotificationRecord _notifRec;
        protected MemberRecord _mrec;
        protected boolean _appFriendsOnly;
        protected Interval _interval = new Interval(_batchInvoker) {
            public void expired () {
                try {
                    send();
                } catch (ServiceException se) {
                    throw new RuntimeException(se);
                }
            }
        };
    }

    protected Map<String, NotificationBatch> _notifications = Maps.newHashMap();

    protected static final Function<ExternalMapRecord, FQL.Exp> MAPREC_TO_UID_EXP =
        new Function<ExternalMapRecord, FQL.Exp>() {
        public FQL.Exp apply (ExternalMapRecord exRec) {
            return FQL.unquoted(exRec.externalId);
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

    @Inject protected @BatchInvoker Invoker _batchInvoker;
    @Inject protected FacebookRepository _facebookRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyPeerManager _peerMgr;
    @Inject protected RuntimeConfig _runtime; // temp

    protected static final int CONNECT_TIMEOUT = 15*1000; // in millis
    protected static final int READ_TIMEOUT = 15*1000; // in millis
    protected static final int BATCH_READ_TIMEOUT = 5*60*1000; // 5 minutes

    protected static final URL SERVER_URL;
    static {
        try {
            SERVER_URL = new URL("http://api.facebook.com/restserver.php");
        } catch (Exception e) {
            throw new RuntimeException(e); // MalformedURLException should be unchecked, sigh
        }
    }
}
