//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.ReferralInfo;

import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link WebUserService}.
 */
public interface WebUserServiceAsync
{
    /**
     * The asynchronous version of {@link WebUserService#login}.
     */
    public void login (String clientVersion, String username, String password, int expireDays,
                       AsyncCallback<SessionData> callback);

    /**
     * The asynchronous version of {@link WebUserService#register}.
     */
    public void register (
        String clientVersion, String username, String password, String displayName, int[] birthday,
        MediaDesc photo, AccountInfo info, int expireDays, String inviteId, int guestId,
        String captchaChallenge, String captchaResponse, ReferralInfo referral,
        AsyncCallback<SessionData> callback);

    /**
     * The asynchronous version of {@link WebUserService#validateSession}.
     */
    public void validateSession (String clientVersion, String authtok, int expireDays,
                                 AsyncCallback<SessionData> callback);

    /**
     * The asynchronous version of {@link WebUserService#getConnectConfig}.
     */
    void getConnectConfig (AsyncCallback<ConnectConfig> callback);

    /**
     * The asynchronous version of {@link WebUserService#sendForgotPasswordEmail}.
     */
    void sendForgotPasswordEmail (String email, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#updateEmail}.
     */
    void updateEmail (WebIdent ident, String newEmail, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#updateEmailPrefs}.
     */
    public void updateEmailPrefs (WebIdent ident, boolean emailOnWhirledMail,
                                  boolean emailAnnouncements, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#updatePassword}.
     */
    void updatePassword (WebIdent ident, String newPassword, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#resetPassword}.
     */
    public void resetPassword (int memberId, String code, String newPassword,
                               AsyncCallback<Boolean> callback);

    /**
     * The asynchronous version of {@link WebUserService#configurePermaName}.
     */
    void configurePermaName (WebIdent ident, String permaName, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#getAccountInfo}.
     */
    void getAccountInfo (WebIdent ident, AsyncCallback<AccountInfo> callback);

    /**
     * The asynchronous version of {@link WebUserService#updateAccountInfo}.
     */
    void updateAccountInfo (WebIdent ident, AccountInfo info, AsyncCallback<Void> callback);
}
