//
// $Id$

package com.threerings.msoy.facebook.server;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;

import com.threerings.msoy.facebook.server.persist.FacebookNotificationRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.server.persist.ExternalMapRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.ServiceException;

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
    public FacebookJaxbRestClient getFacebookClient (FacebookAppCreds creds)
    {
        return getFacebookClient(creds.apiKey, creds.appSecret, creds.sessionKey);
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

        // convert to millis
        delay *= 60 * 1000L;

        // insert into map and schedule
        synchronized (_notifications) {
            NotificationBatch notification = _notifications.get(id);
            if (notification != null) {
                notification.cancel();
            }
            _notifications.put(id, notification = new NotificationBatch(notifRec, delay));
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
     * A notification batch.
     */
    protected class NotificationBatch
    {
        public NotificationBatch (FacebookNotificationRecord notifRec, long delay)
        {
            _notifRec = notifRec;
            _interval.schedule(delay, false);
        }

        public void cancel ()
            throws ServiceException
        {
            _interval.cancel();
        }

        protected void send ()
        {
            _facebookRepo.noteNotificationStarted(_notifRec.id);

            final FacebookJaxbRestClient client = getFacebookBatchClient();
            final int BATCH_SIZE = 100;
            segment(_memberRepo.loadExternalMappings(ExternalAuther.FACEBOOK),
                new Function<List<ExternalMapRecord>, Void>() {
                    public Void apply (List<ExternalMapRecord> exRecs) {
                        sendBatch(client, exRecs);
                        return null;
                    }
                }, BATCH_SIZE);

            _facebookRepo.noteNotificationFinished(_notifRec.id);
            log.info("Successfully sent notifications", "id", _notifRec.id);
        }

        protected void sendBatch (FacebookJaxbRestClient client, List<ExternalMapRecord> batch)
        {
            try {
                trySendBatch(client, batch);
            } catch (Exception e) {
                log.warning("Failed to send facebook notifications", e);
            }
        }

        protected void trySendBatch (FacebookJaxbRestClient client, List<ExternalMapRecord> batch)
            throws FacebookException
        {
            _facebookRepo.noteNotificationProgress(_notifRec.id, "Loading recipients", 0, 0);

            List<FQL.Exp> uids = Lists.transform(batch,
                new Function<ExternalMapRecord, FQL.Exp>() {
                public FQL.Exp apply (ExternalMapRecord exRec) {
                    return FQL.unquoted(exRec.externalId);
                }
            });

            FQLQuery getUsers = new FQLQuery(USERS_TABLE, new FQL.Field[] {UID, IS_APP_USER},
                new FQL.Where(UID.in(uids)));

            List<Long> targetIds = Lists.newArrayListWithCapacity(uids.size());
            for (FQLQuery.Record result : getUsers.run(client)) {
                if (!result.getField(IS_APP_USER).equals("1")) {
                    continue;
                }
                targetIds.add(Long.valueOf(result.getField(UID)));
            }

            if (targetIds.size() == 0) {
                return;
            }

            _facebookRepo.noteNotificationProgress(_notifRec.id, "Sending", targetIds.size(), 0);
            client.notifications_send(targetIds, _notifRec.text, true);
            _facebookRepo.noteNotificationProgress(_notifRec.id, "Sent", 0, targetIds.size());
        }

        protected FacebookNotificationRecord _notifRec;
        protected Interval _interval = new Interval(_batchInvoker) {
            public void expired () {
                send();
            }
        };
    }

    protected Map<String, NotificationBatch> _notifications = Maps.newHashMap();

    protected static final FQL.Field UID = new FQL.Field("uid");
    protected static final FQL.Field IS_APP_USER = new FQL.Field("is_app_user");
    protected static final String USERS_TABLE = "user";

    @Inject protected @BatchInvoker Invoker _batchInvoker;
    @Inject protected FacebookRepository _facebookRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyPeerManager _peerMgr;

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
