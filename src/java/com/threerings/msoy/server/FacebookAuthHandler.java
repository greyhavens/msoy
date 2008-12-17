//
// $Id$

package com.threerings.msoy.server;

import java.net.URL;
import java.sql.Date;
import java.text.SimpleDateFormat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.samskivert.util.StringUtil;

import com.google.common.collect.Lists;

import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.ProfileField;
import com.google.code.facebookapi.schema.FriendsGetResponse;
import com.google.code.facebookapi.schema.User;
import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.web.gwt.ExternalCreds;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.profile.gwt.Profile;

import static com.threerings.msoy.Log.log;

/**
 * Handles Facebook authentication.
 */
public class FacebookAuthHandler extends ExternalAuthHandler
{
    public static ExternalAuthHandler getInstance ()
    {
        return new FacebookAuthHandler();
    }

    @Override // from ExternalAuthHandler
    public void validateCredentials (ExternalCreds creds)
        throws ServiceException
    {
        FacebookCreds fbcreds = (FacebookCreds)creds;
        // TODO: validate creds
    }

    @Override // from ExternalAuthHandler
    public Info getInfo (ExternalCreds creds)
        throws ServiceException
    {
        FacebookCreds fbcreds = (FacebookCreds)creds;
        FacebookJaxbRestClient fbclient = getFacebookClient(fbcreds);

        Info info = new Info();
        try {
            // look up information from this user's facebook profile
            Set<Long> ids = Collections.singleton(Long.parseLong(fbcreds.uid));
            EnumSet<ProfileField> fields = EnumSet.of(
                ProfileField.FIRST_NAME, ProfileField.LAST_NAME, ProfileField.SEX,
                ProfileField.BIRTHDAY, ProfileField.CURRENT_LOCATION);
            UsersGetInfoResponse uinfo = (UsersGetInfoResponse)fbclient.users_getInfo(ids, fields);
            if (uinfo.getUser().size() > 0) {
                User user = uinfo.getUser().get(0);
                info.displayName = user.getFirstName();
                info.profile.realName = user.getFirstName() + " " + user.getLastName();
                String city = user.getCurrentLocation().getValue().getCity();
                info.profile.location = (city == null) ? "" : city;
                if ("male".equalsIgnoreCase(user.getSex().getValue())) {
                    info.profile.sex = Profile.SEX_MALE;
                } else if ("female".equalsIgnoreCase(user.getSex().getValue())) {
                    info.profile.sex = Profile.SEX_FEMALE;
                }
                String bdstr = user.getBirthday().getValue();
                try {
                    info.profile.birthday = new Date(_bfmt.parse(String.valueOf(bdstr)).getTime());
                } catch (Exception e) {
                    log.info("Cannot parse Facebook birthday", "uid", fbcreds.uid, "bday", bdstr);
                }
            }

            // look up their friends' facebook ids
            info.friendIds = Lists.newArrayList();
            FriendsGetResponse finfo = fbclient.friends_get();
            for (Long uid : finfo.getUid()) {
                info.friendIds.add(uid.toString());
            }

        } catch (Exception e) {
            log.warning("Facebook getInfo() failed", "creds", creds, e);
        }

        return info;
    }

    protected FacebookJaxbRestClient getFacebookClient (FacebookCreds creds)
    {
        String apiKey = ServerConfig.config.getValue("facebook.api_key", (String)null);
        if (StringUtil.isBlank(apiKey)) {
            throw new IllegalStateException("Missing facebook.api_key server configuration.");
        }
        String secret = ServerConfig.config.getValue("facebook.secret", (String)null);
        if (StringUtil.isBlank(secret)) {
            throw new IllegalStateException("Missing facebook.secret server configuration.");
        }
        return new FacebookJaxbRestClient(
            SERVER_URL, apiKey, secret, creds.sessionKey, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    protected static final int CONNECT_TIMEOUT = 15*1000; // in millis
    protected static final int READ_TIMEOUT = 15*1000; // in millis

    protected static final URL SERVER_URL;
    static {
        try {
            SERVER_URL = new URL("http://api.facebook.com/restserver.php");
        } catch (Exception e) {
            throw new RuntimeException(e); // MalformedURLException should be unchecked, sigh
        }
    }

    /** Used to parse Facebook profile birthdays. */
    protected static SimpleDateFormat _bfmt = new SimpleDateFormat("MMMM dd, yyyy");
}
