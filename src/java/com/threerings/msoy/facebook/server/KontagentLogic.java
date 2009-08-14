//
// $Id$

package com.threerings.msoy.facebook.server;

import com.google.inject.Singleton;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import static com.threerings.msoy.Log.log;

@Singleton
public class KontagentLogic
{
    protected void sendMessage (String type, String... nameValuePairs)
    {
        if (StringUtil.isBlank(API_KEY)) {
            // this is a dev deployment or kontagent is disabled
            log.info("Skipping message (disabled)", "type", type, "data", nameValuePairs);
            return;
        }

        String url = SharedNaviUtil.buildRequest(MSG_URL, nameValuePairs);
        log.info("Sending message", "url", url);

        // TODO: sign params
        // TODO: query url and wait for OK
    }

    protected static final String API_URL = "http://api.geo.kontagent.net/api/";
    protected static final String VERSION = "v1";
    protected static final String API_KEY = ServerConfig.config.getValue("kontagent.api_key", "");
    protected static final String SECRET = ServerConfig.config.getValue("kontagent.secret", "");
    protected static final String MSG_URL = API_URL + VERSION + "/" + API_KEY + "/";
}
