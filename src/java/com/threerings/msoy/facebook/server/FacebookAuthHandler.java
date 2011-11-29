//
// $Id$

package com.threerings.msoy.facebook.server;

import java.sql.Date;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.restfb.DefaultFacebookClient;
import com.restfb.types.User;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.server.ExternalAuthHandler;
import com.threerings.msoy.web.gwt.ExternalCreds;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.ServiceCodes;

import static com.threerings.msoy.Log.log;

/**
 * Handles Facebook authentication.
 */
@Singleton
public class FacebookAuthHandler extends ExternalAuthHandler
{
    // trophy bundle: 48939637184

    public static ExternalAuthHandler getInstance ()
    {
        return new FacebookAuthHandler();
    }

    @Override // from ExternalAuthHandler
    public void validateCredentials (ExternalCreds creds)
        throws ServiceException
    {
        // FacebookCreds fbcreds = (FacebookCreds)creds;
        // TODO: validate creds
    }

    @Override // from ExternalAuthHandler
    public Info getInfo (ExternalCreds creds)
        throws ServiceException
    {
        DefaultFacebookClient client;
        if (creds instanceof FacebookCreds) {
            client = new DefaultFacebookClient(((FacebookCreds)creds).accessToken);
        } else if (creds instanceof FacebookAppCreds) {
            throw new IllegalArgumentException("Canvas apps are unimplemented!");
        } else {
            throw new IllegalArgumentException("Invalid creds: " + creds);
        }

        Info info = new Info();
        try {
            // Lookup information from this user's facebook profile
            User user = client.fetchObject("me", User.class);
            info.displayName = user.getFirstName();
            if ("male".equalsIgnoreCase(user.getGender())) {
                info.profile.sex = Profile.SEX_MALE;
            } else if ("female".equalsIgnoreCase(user.getGender())) {
                info.profile.sex = Profile.SEX_FEMALE;
            }
            info.profile.realName = user.getName();
            info.profile.location = (user.getLocation() != null) ?
                user.getLocation().getName() : "";

            java.util.Date bday = user.getBirthdayAsDate();
            info.profile.birthday = bday != null ? new Date(bday.getTime()) : null;
            info.friendIds = _faceLogic.fetchFriends(client);

            // TODO(bruno): Download their FB photo and make it their ProfileRecord photo

            return info;

        } catch (Exception e) {
            log.warning("Facebook getInfo() failed", "creds", creds, e);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    @Inject protected FacebookLogic _faceLogic;
}
