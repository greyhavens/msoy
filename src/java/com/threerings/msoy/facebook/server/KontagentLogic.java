//
// $Id$

package com.threerings.msoy.facebook.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import static com.threerings.msoy.Log.log;

@Singleton
public class KontagentLogic
{
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

    protected void sendMessage (String type, String... nameValuePairs)
    {
        String url = buildMessageUrl(MSG_URL + type + "/",
            String.valueOf(System.currentTimeMillis()), SECRET, nameValuePairs);

        if (StringUtil.isBlank(API_KEY)) {
            // this is a dev deployment or kontagent is disabled
            log.info("Kontagent disabled, skipping message", "url", url);
            return;
        }

        // TODO
        log.info("Sending message", "url", url);
    }

    protected static final String API_URL = "http://api.geo.kontagent.net/api/";
    protected static final String VERSION = "v1";
    protected static final String API_KEY = ServerConfig.config.getValue("kontagent.api_key", "");
    protected static final String SECRET = ServerConfig.config.getValue("kontagent.secret", "");
    protected static final String MSG_URL = API_URL + VERSION + "/" + API_KEY + "/";
}
