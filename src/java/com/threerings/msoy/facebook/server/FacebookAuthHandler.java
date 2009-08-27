//
// $Id$

package com.threerings.msoy.facebook.server;

import java.sql.Date;
import java.text.SimpleDateFormat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.ProfileField;
import com.google.code.facebookapi.schema.FriendsGetResponse;
import com.google.code.facebookapi.schema.User;
import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.web.gwt.ExternalCreds;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.server.ExternalAuthHandler;

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
        FacebookJaxbRestClient fbclient;
        if (creds instanceof FacebookCreds) {
            fbclient = _faceLogic.getFacebookClient((FacebookCreds)creds);
        } else if (creds instanceof FacebookAppCreds) {
            fbclient = _faceLogic.getFacebookClient((FacebookAppCreds)creds);
        } else {
            throw new IllegalArgumentException("Invalid creds: " + creds);
        }

        Info info = new Info();
        try {
            // look up information from this user's facebook profile
            Set<Long> ids = Collections.singleton(Long.parseLong(creds.getUserId()));
            EnumSet<ProfileField> fields = EnumSet.of(
                ProfileField.FIRST_NAME, ProfileField.LAST_NAME, ProfileField.SEX,
                ProfileField.BIRTHDAY, ProfileField.CURRENT_LOCATION);
            UsersGetInfoResponse uinfo = (UsersGetInfoResponse)fbclient.users_getInfo(ids, fields);
            if (uinfo.getUser().size() > 0) {
                User user = uinfo.getUser().get(0);
                info.displayName = user.getFirstName();
                info.profile.realName = user.getFirstName() + " " + user.getLastName();
                if (user.getCurrentLocation() != null) {
                    info.profile.location = StringUtil.deNull(user.getCurrentLocation().getCity());
                }
                if ("male".equalsIgnoreCase(user.getSex())) {
                    info.profile.sex = Profile.SEX_MALE;
                } else if ("female".equalsIgnoreCase(user.getSex())) {
                    info.profile.sex = Profile.SEX_FEMALE;
                }
                java.util.Date bday = FacebookLogic.parseBirthday(user.getBirthday()).right;
                info.profile.birthday = bday != null ? new Date(bday.getTime()) : null;
            }

            // TODO: we need to somehow fix this: Session key invalid or no longer valid

            // look up their friends' facebook ids
            try {
                info.friendIds = Lists.newArrayList();
                FriendsGetResponse finfo = fbclient.friends_get();
                for (Long uid : finfo.getUid()) {
                    info.friendIds.add(uid.toString());
                }
            } catch (Exception e) {
                log.info("Failed to look up Facebook friends", "who", creds.getUserId(),
                         "error", e.getMessage());
            }
            return info;

        } catch (Exception e) {
            log.warning("Facebook getInfo() failed", "creds", creds, e);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    @Inject protected FacebookLogic _faceLogic;
}
