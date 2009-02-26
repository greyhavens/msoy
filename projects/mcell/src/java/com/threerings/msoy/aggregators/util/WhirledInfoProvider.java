// $Id: WhirledInfoProvider.java 1226 2008-12-22 23:30:21Z robert $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Retrieves public information from Whirled servers, caching locally between requests.
 *
 * <p>Note: this class uses a REST API for fetching data from the Whirled server,
 * which needs to match the definition in com.threerings.msoy.web.server.PublicInfoServlet,
 * in the Whirled source tree.
 *
 * @author Robert Zubek <robert@threerings.net>
 */
@ThreadSafe
public class WhirledInfoProvider
{
    /** Describes the various types of public information retrievable from Whirled. */
    public enum DataType {
        GAME    ("game"),
        WHIRLED ("whirled");

        public String getType () {
            return type;
        }

        public Map<Integer, Info> getCache () {
            return cache;
        }

        private DataType (String type) {
            @SuppressWarnings("unchecked")
            final Map<Integer, Info> lrumap = new LRUMap();

            this.cache = Collections.synchronizedMap(lrumap);
            this.type = type;
        }

        private final String type;
        private final Map<Integer, Info> cache;
    }

    /**
     * Interface for accessing cached info data.
     * Accessor functions return the requested fields, or null if content wasn't found.
     */
    public interface Info {
        public String getString (String name);
        public Integer getInteger (String name);
        public Boolean getBoolean (String name);
    }

    public WhirledInfoProvider (String host)
    {
        this.host = host;
        this.client = new HttpClient();
    }

    /**
     * Looks up the specified JSON object, memoizing via the type's LRU cache.
     */
    public Info get (DataType type, int id)
    {
        final Map<Integer, Info> cache = type.getCache();

        if (! cache.containsKey(id)) {
            // the map is synchronized, but multiple writers won't be prevented from
            // clobbering the map with several copies of the same new data.
            cache.put(id, fetch(type, id));
        }

        Info info = cache.get(id);
        return (info != unknownInfo) ? info : null;
    }

    /**
     * Contacts the Whirled server and fetches a single public info datum
     * for an object of given type and ID.
     */
    private JSONInfo fetch (DataType type, int id)
    {
        String response = fetchPage(type, id);

        // if the server fails to respond, make something up
        if (response == null) {
            response = String.format(emptyInfo, id, id);
        }

        // if the server doesn't know anything about this id, don't produce any output
        if (response.startsWith("[]")) {
            return unknownInfo;
        }

        // now convert to a proper array of info objects
        try {
            JSONArray results = new JSONArray(response);
            if (results.length() != 1) {
                throw new JSONException("Results array expected 1 element, got " + results.length());
            }
            return new JSONInfo(results.getJSONObject(0));

        } catch (JSONException je) {
            log.error("Failed to parse Whirled info string: " + response, je);
            return null;

        }
    }

    /**
     * Helper function to do the actual HTTP retrieval.
     */
    private String fetchPage (DataType type, int id)
    {
        final String path = String.format(queryPattern, type.getType(), id);

        // create the url
        URL url;
        try {
            url = new URL("http", host, path);
        } catch (MalformedURLException me) {
            log.error("Failed to create URL to get Whirled info from " + host, me);
            return null; // we can't recover from this one
        }

        // try to get the data
        HttpMethod method = new GetMethod(url.toString());
        try{
            client.executeMethod(method);
            return method.getResponseBodyAsString();

        } catch (IOException ioe) {
            log.error("Failed to fetch data from " + url, ioe);
            return null; // we're done for

        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Private implementation of {@link Info}, wrapping around a JSON object,
     * and doing string decoding to match what goes on in the Whirled server.
     */
    private static class JSONInfo implements Info
    {
        public JSONInfo (JSONObject source) {
            this.source = source;
        }

        public Boolean getBoolean (String name) {
            try {
                return source.getBoolean(name);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }

        public Integer getInteger (String name) {
            try {
                return source.getInt(name);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }

        public String getString (String name) {
            try {
                return URLDecoder.decode(source.getString(name), "UTF-8");
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }

        private final JSONObject source;
    }

    /** Pattern used to construct Whirled query URLs. */
    private final static String queryPattern = "/info?type=%s&id=%d";

    /** Pattern used to construct an empty, unnamed Info element. */
    private final static String emptyInfo = "[{\"name\":\"Unnamed (# %d)\",\"id\":%d}]";

    /** Constant used to denote that info of this type was not found on the server. */
    private final static JSONInfo unknownInfo = new JSONInfo(null);

    /** Our logger. */
    private final static Logger log = Logger.getLogger(WhirledInfoProvider.class);

    private final String host;
    private final HttpClient client;
}
