//
// $Id$

package com.threerings.msoy.facebook.server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.facebook.server.persist.KontagentInfoRecord;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import static com.threerings.msoy.Log.log;

@Singleton
public class KontagentLogic
{
    /**
     * Kinds of links that we send, on behalf of a user or globally.
     */
    public enum LinkType
    {
        INVITE("inv", MessageType.INVITE_RESPONSE),
        NOTIFICATION("ntf", MessageType.NOTIFICATION_RESPONSE),
        FEED_SHORT("fds", MessageType.POST_RESPONSE),
        FEED_LONG("fdl", MessageType.POST_RESPONSE);

        /** The unique id for composing and parsing. */
        public String id;

        /** The type of message to send to Kontagent on click-through. */
        public MessageType responseType;

        LinkType (String id, MessageType responseType) {
            this.id = id;
            this.responseType = responseType;
        }
    }

    /**
     * A unique id for links that we generate. Can be flattened and embedded in a link and later
     * reconstituted and reported to the Kontagent server.
     */
    public static class TrackingId
    {
        /** The type of link. */
        public LinkType type;

        /** The subtype (e.g. a game id). */
        public String subtype;

        /** The Kontagent tracking tag. */
        public String uuid;

        /**
         * Creates a new tracking id of the given type, a null subtype and a new uuid seeded by the
         * given initiator.
         */
        public TrackingId (LinkType type, long initiator)
        {
            this(type, null, initiator);
        }

        /**
         * Creates a new tracking id of the given type and subtype and a new uuid seeded by the
         * given initiator.
         */
        public TrackingId (LinkType type, String subtype, long initiator)
        {
            this.type = type;
            this.subtype = subtype;
            this.uuid = genUUID(initiator);
        }

        /**
         * Creates a new tracking id from a previously flattened one. We assume the id is sent
         * directly from the web, therefore an exception is thrown if it is not in the expected
         * format.
         * @throws IllegalArgumentException if the tracking id or any component is invalid
         */
        public TrackingId (String trackingId)
        {
            String[] components = trackingId.split("-");
            if (components.length < 2 || components.length > 3) {
                throw new IllegalArgumentException();
            }
            for (LinkType type : LinkType.values()) {
                if (type.id.equals(components[0])) {
                    this.type = type;
                    break;
                }
            }
            if (type == null) {
                throw new IllegalArgumentException();
            }
            uuid = components[components.length - 1];
            if (!uuid.matches("[0-9a-f]{16}")) {
                throw new IllegalArgumentException();
            }
            if (components.length == 3) {
                subtype = components[1];
            }
        }

        /**
         * Flattens the tracking id to a string that can be embedded in a URL and later
         * reconstructed with {@link #TrackingId(String)}.
         */
        public String flatten ()
        {
            // type-subtype-uuid or type-uuid
            return type.id + (StringUtil.isBlank(subtype) ? "" : "-" + subtype) + "-" + uuid;
        }
    }

    /**
     * Builds the url for sending a kontagent message using the given name/value pairs, type,
     * timestamp and secrect. Blank values are removed. The time stamp and signature are appended
     * as required.
     */
    public static String buildMessageUrl (
        String baseUrl, String timeStamp, String secret, String... nameValuePairs)
    {
        // convert to map, eliminate blank parameters
        Map<String, String> params = Maps.newHashMap();
        for (int ii = 0; ii < nameValuePairs.length; ii += 2) {
            if (StringUtil.isBlank(nameValuePairs[ii+1])) {
                continue;
            }
            params.put(nameValuePairs[ii], nameValuePairs[ii+1]);
        }

        // add the time stamp
        params.put("ts", timeStamp);

        // sort by name
        List<String> names = Lists.newArrayListWithCapacity(params.size());
        names.addAll(params.keySet());
        Collections.sort(names);

        // build parameter list and signature buffer
        List<String> urlParams = Lists.newArrayListWithCapacity((names.size() + 1) * 2);
        StringBuilder sig = new StringBuilder();
        for (String name : names) {
            String value = params.get(name);
            sig.append(name).append("=").append(value);
            urlParams.add(name);
            urlParams.add(value);
        }
        sig.append(secret);

        // append signature
        urlParams.add("an_sig");
        urlParams.add(StringUtil.md5hex(sig.toString()));

        return SharedNaviUtil.buildRequest(baseUrl, urlParams);
    }

    /**
     * Generates a 16 hex digit Kontagent UUID for use in tracking invites etc.
     */
    public static String genUUID (long uid)
    {
        long salt = System.currentTimeMillis();
        for (int shift = 60; shift >= 0; shift -= 4, salt >>= 4) {
            uid ^= (salt & 0xF) << shift;
        }
        uid ^= _rand.nextLong();
        String hex = Long.toHexString(uid);
        if (hex.length() < 16) {
            hex = "0000000000000000".substring(hex.length()) + hex;
        }
        return hex;
    }

    @Inject public KontagentLogic (Lifecycle lifecycle)
    {
        lifecycle.addComponent(new Lifecycle.ShutdownComponent() {
            @Override public void shutdown () {
                _queue.shutdown();
            }
        });
        _queue.start();
    }

    /**
     * Tracks a visit to the application.
     * @param uid id of the user visiting
     * @param trackingId the tracking id given via the canvas url
     * @param newInstall if this is the user's first visit after the login redirect
     */
    public void trackUsage (int appId, long uid, String trackingId, boolean newInstall)
    {
        TrackingId id = parseTrackingId(trackingId, true);
        String uidStr = String.valueOf(uid);
        if (newInstall) {
            if (id != null) {
                _queue.post(appId, MessageType.APP_ADDED, "s", uidStr, "u", id.uuid);
            } else {
                _queue.post(appId, MessageType.APP_ADDED, "s", uidStr, "su", ShortTag.UNKNOWN.id);
            }
        }

        if (id == null) {
            // TODO: hmm, new short tag for different sources? (e.g. app bookmark vs. clicking the
            // app link in an invite)
            _queue.post(appId, MessageType.UNDIRECTED, "s", uidStr, "tu", ShortTag.UNKNOWN.id, "i", "1");

        } else {
            switch (id.type.responseType) {
            case INVITE_RESPONSE:
            case NOTIFICATION_RESPONSE:
                _queue.post(appId, id.type.responseType, "r", uidStr, "i", "1", "u", id.uuid,
                    "st1", id.subtype, "tu", id.type.responseType.id);
                break;
            case POST_RESPONSE:
                _queue.post(appId, id.type.responseType, "r", uidStr, "i", "1", "tu", FEED_CHANNEL,
                    "st1", id.subtype, "u", id.uuid);
                break;
            default:
                log.warning("Unhandled response type", "trackingId", trackingId);
                break;
            }
        }
    }

    /**
     * Tracks the removal of the application.
     */
    public void trackApplicationRemoved (int appId, long uid)
    {
        _queue.post(appId, MessageType.APP_REMOVED, "s", String.valueOf(uid));
    }

    /**
     * Tracks an invite sent by the user. The tracking id is normally obtained from a previously
     * constructed {@link TrackingId} and passed via the cgi invite submission parameters.
     */
    public void trackInviteSent (int appId, long senderId, String trackingId, String[] recipients)
    {
        TrackingId id = parseTrackingId(trackingId, false);
        if (id == null) {
            return;
        }

        _queue.post(appId, MessageType.INVITE_SENT, "s", String.valueOf(senderId),
            "r", StringUtil.join(recipients, StringUtil.encode(",")), "u", id.uuid,
            "st1", id.subtype);
    }

    /**
     * Track the sending of a notification. The tracking id is normally generated during the
     * scheduling of the notification and passed back after it is actually sent, possibly multiple
     * times for large batches.
     */
    public void trackNotificationSent (
        int appId, long senderId, TrackingId trackingId, Collection<String> recipients)
    {
        // no need to send if there are no recipients
        if (recipients.size() == 0) {
            return;
        }

        _queue.post(appId, MessageType.NOTIFICATION_SENT, "s", String.valueOf(senderId),
            "r", StringUtil.join(recipients.toArray(), StringUtil.encode(",")),
            "u", trackingId.uuid, "st1", trackingId.subtype);
    }

    /**
     * Track the posting of a feed story. The tracking id is normally generated when the template
     * is requested, then passed back when the user presses a button. Note the Facebook API does
     * not actually tell us whether the user pressed "Cancel" or "Publish", so I guess we just have
     * to pass cancels through to Kontagent and let them be 0% responses.
     * TODO: work around lack of cancel/publish distinction
     */
    public void trackFeedStoryPosted (int appId, long senderId, String trackingId)
    {
        TrackingId id = parseTrackingId(trackingId, false);
        if (id == null) {
            return;
        }

        _queue.post(appId, MessageType.POST, "s", String.valueOf(senderId), "tu", FEED_CHANNEL,
            "u", id.uuid, "st1", id.subtype);
    }

    /**
     * Track the user info for demographics. Any null values are considered undefined or
     * unspecified and will not be sent.
     */
    public void trackUserInfo (int appId, long uid, Integer birthYear, String gender, String city,
        String state, String zip, String country, Integer friendCount)
    {
        // convert to kontagent gender
        if ("male".equals(gender) || "female".equals(gender)) {
            gender = gender.substring(0, 1);
        } else if (gender != null) {
            gender = "u";
        }

        // convert to strings
        String birthYearStr = birthYear == null ? null : String.valueOf(birthYear);
        String friendCountStr = friendCount == null ? null : String.valueOf(friendCount);

        // encode
        city = StringUtil.encode(city);
        state = StringUtil.encode(state);
        zip = StringUtil.encode(zip);
        country = StringUtil.encode(country);

        // send
        _queue.post(appId, MessageType.USER_INFO, "s", String.valueOf(uid), "b", birthYearStr,
            "g", gender, "ly", city, "lc", country, "ls", state, "lp", zip, "f", friendCountStr);
    }

    public void trackPageRequest (int appId, long uid, String ipAddress, String page)
    {
        _queue.post(appId, MessageType.PAGE_REQUEST, "s", String.valueOf(uid), "ip", ipAddress,
            "u", StringUtil.encode(page));
    }

    protected TrackingId parseTrackingId (String trackingId, boolean allowBlank)
    {
        if (allowBlank && StringUtil.isBlank(trackingId)) {
            return null;
        }

        try {
            return new TrackingId(trackingId);

        } catch (IllegalArgumentException ex) {
            log.warning("Invalid tracking id", "value", trackingId);
            return null;
        }
    }

    protected void sendMessage (int appId, MessageType type, String... nameValuePairs)
        throws MalformedURLException, IOException
    {
        KontagentInfoRecord kinfo = _facebookRepo.loadKontagentInfo(appId);
        if (kinfo == null || StringUtil.isBlank(kinfo.apiSecret) ||
                StringUtil.isBlank(kinfo.apiKey)) {
            // this is a dev deployment or kontagent is disabled
            log.info("Disabled, skipping message", "appId", appId, "type", type,
                "pairs", nameValuePairs);
            return;
        }

        String url = buildMessageUrl(API_URL + VERSION + "/" + kinfo.apiKey + "/" + type.id + "/",
            String.valueOf(System.currentTimeMillis()), kinfo.apiSecret, nameValuePairs);

        if (DeploymentConfig.devDeployment) {
            log.info("Sending message", "url", url);
        }

        HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            log.warning("Response code not OK",
                "code", conn.getResponseCode(), "url", url);
        }
        conn.disconnect();
    }

    /**
     * Simple invoker thread to send our messages from somewhere besides the servlet threads.
     */
    protected class MessageSender extends Invoker
    {
        public MessageSender ()
        {
            super("Kontagent", null);
        }

        public void post (final int appId, final MessageType type, final String... nameValuePairs)
        {
            if (shutdownRequested()) {
                log.info("Server shutting down, skipping message", "appId", appId,
                    "type", type, "pairs", nameValuePairs);
                return;
            }

            postUnit(new Invoker.Unit("send") {
                @Override public boolean invoke () {
                    try {
                        sendMessage(appId, type, nameValuePairs);
                    } catch (Exception ex) {
                        log.warning("Failed to send", "appId", appId, "type", type,
                            "pairs", nameValuePairs);
                    }
                    return false;
                }
                @Override public long getLongThreshold () {
                    return 3000;
                }
            });
        }
    }

    /**
     * Kontagent message types.
     */
    protected enum MessageType
    {
        APP_ADDED("apa"),
        APP_REMOVED("apr"),
        UNDIRECTED("ucc"),
        INVITE_RESPONSE("inr"),
        INVITE_SENT("ins"),
        NOTIFICATION_SENT("nts"),
        NOTIFICATION_RESPONSE("ntr"),
        POST("pst"),
        POST_RESPONSE("psr"),
        USER_INFO("cpu"),
        PAGE_REQUEST("pgr");

        public String id;

        MessageType (String id) {
            this.id = id;
        }
    }

    /**
     * Kontagent short tag types.
     */
    protected enum ShortTag
    {
        UNKNOWN("unknown1");

        public String id;

        ShortTag (String id) {
            if (id.length() != 8) {
                throw new IllegalArgumentException();
            }
            this.id = id;
        }
    }

    /** A private thread for sending message. */
    protected MessageSender _queue = new MessageSender();

    // dependencies
    @Inject protected FacebookRepository _facebookRepo;

    protected static final Random _rand = new Random();

    protected static final String API_URL = DeploymentConfig.devDeployment ?
        "http://api.test.kontagent.net/api/" : "http://api.geo.kontagent.net/api/";
    protected static final String VERSION = "v1";
    protected static final String FEED_CHANNEL = "feedstory";
}
