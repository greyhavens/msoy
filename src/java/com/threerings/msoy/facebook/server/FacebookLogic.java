//
// $Id$

package com.threerings.msoy.facebook.server;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.msoy.facebook.gwt.NotificationStatus;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.server.persist.ExternalMapRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.server.FacebookServlet;

import static com.threerings.msoy.Log.log;

/**
 * Centralizes some Facebook API bits.
 */
@Singleton
public class FacebookLogic
{
    /**
     * Returns a Facebook client not bound to any particular user's session.
     */
    public FacebookJaxbRestClient getFacebookClient ()
    {
        return getFacebookClient((String)null);
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
        return getFacebookClient(requireAPIKey(), requireSecret(), sessionKey);
    }

    /**
     * Returns a Facebook client for the app represented by the supplied creds.
     */
    public FacebookJaxbRestClient getFacebookClient (FacebookServlet.FacebookAppCreds creds)
    {
        return getFacebookClient(creds.apiKey, creds.appSecret, creds.sessionKey);
    }

    /**
     * Schedules or reschedules a notification to be posted to all Facebook users. If a
     * notification already exists with the given id, it is rescheduled and the content
     * overwritten.
     * @param delay number of minutes to wait before sending the notification
     */
    public void scheduleNotification (String id, String content, long delay)
    {
        // convert to millis
        delay *= 60 * 1000L;

        synchronized (_notifications) {
            NotificationBatch notification = _notifications.get(id);
            if (notification == null) {
                _notifications.put(id, notification = new NotificationBatch(id));
            }
            notification.schedule(delay, content);
        }
    }

    /**
     * Schedules a notification with text loaded from the database.
     */
    public void scheduleNotification (String id, long delay)
    {
        String text = _facebookRepo.getNotification(id);
        if (StringUtil.isBlank(text)) {
            log.warning("Missing notification", "id", id);
            return;
        }
        scheduleNotification(id, text, delay);
    }

    /**
     * Gets the statuses of all scheduled, active or completed notification batches.
     */
    public List<NotificationStatus> getNotificationStatuses ()
    {
        synchronized (_notifications) {
            List<NotificationStatus> results =
                Lists.newArrayListWithCapacity(_notifications.size());
            for (NotificationBatch notification : _notifications.values()) {
                results.add(notification.getStatus().clone());
            }
            return results;
        }
    }

    protected FacebookJaxbRestClient getFacebookClient (
        String apiKey, String appSecret, String sessionKey)
    {
        return new FacebookJaxbRestClient(
            SERVER_URL, apiKey, appSecret, sessionKey, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    protected FacebookJaxbRestClient getFacebookBatchClient ()
    {
        return new FacebookJaxbRestClient(SERVER_URL, requireAPIKey(), requireSecret(), null,
            CONNECT_TIMEOUT, BATCH_READ_TIMEOUT);
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

    /**
     * A notification batch.
     */
    protected class NotificationBatch
    {
        public NotificationBatch (String id)
        {
            _id = id;
            _status = new NotificationStatus(id);
        }

        public void schedule (long delay, String content)
        {
            if (_interval == null) {
                _interval = new Interval(_batchInvoker) {
                    public void expired () {
                        trySend();
                    }
                };
            }
            _interval.schedule(delay, false);
            _content = content;
            _status.start = new Date(System.currentTimeMillis() + delay);
        }

        public NotificationStatus getStatus ()
        {
            return _status;
        }

        protected void trySend ()
        {
            try {
                send();
                log.info("Successfully sent notification", "id", _id);

            } catch (Exception e) {
                log.warning("Unable to send notification", "id", _id, e);
                _status.status = "Failed: " + e;
            }
        }

        protected void send ()
            throws FacebookException
        {
            _status = new NotificationStatus(_id);
            _status.status = "Loading recipients";
            _status.start = new Date();

            // TODO: hmm, going over each individual user and getting a jax response for each one
            // seems rather inefficient
            FacebookJaxbRestClient client = getFacebookBatchClient();
            List<Long> userIds = Lists.newArrayList();
            for (ExternalMapRecord extRec :
                _memberRepo.loadExternalMappings(ExternalAuther.FACEBOOK)) {
                Long userId = Long.valueOf(extRec.externalId);
                if (client.users_isAppUser(userId)) {
                    userIds.add(userId);
                    _status.userCount++;
                }
            }

            _status.status = "Sending";
            client.notifications_send(userIds, _content, true);

            _status.status = "Finished";
            _status.finished = new Date();
        }

        protected String _id;
        protected String _content;
        protected Interval _interval;
        protected NotificationStatus _status;
    }

    protected Map<String, NotificationBatch> _notifications = Maps.newHashMap();

    @Inject protected @BatchInvoker Invoker _batchInvoker;
    @Inject protected FacebookRepository _facebookRepo;
    @Inject protected MemberRepository _memberRepo;

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
