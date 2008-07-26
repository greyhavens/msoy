//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines general user services available to the GWT/AJAX web client.
 */
public interface WebUserService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/usersvc";

    /**
     * Requests that the client be logged in as the specified user with the supplied (MD5-encoded)
     * password.
     *
     * @return a set of credentials including a session cookie that should be provided to
     * subsequent remote service calls that require authentication.
     */
    public SessionData login (String clientVersion, String email, String password, int expireDays)
        throws ServiceException;

    /**
     * Requests that an account be created for the specified user. The user will be logged in after
     * the account is created.
     *
     * @return a set of credentials including a session cookie that should be provided to
     * subsequent remote service calls that require authentication.
     */
    public SessionData register (
        String clientVersion, String email, String password, String displayName, int[] birthday,
        MediaDesc photo, AccountInfo info, int expireDays, String inviteId, int guestId,
        String captchaChallenge, String captchaResponse, ReferralInfo referral)
        throws ServiceException;

    /**
     * Validates that the supplied session token is still active and refreshes its expiration time
     * if so.
     */
    public SessionData validateSession (String clientVersion, String authtok, int expireDays)
        throws ServiceException;

    /**
     * Returns the connection information for this server's World services.
     */
    public ConnectConfig getConnectConfig ()
        throws ServiceException;

    /**
     * Sends a "forgot my password" email to the account registered with the supplied address.
     */
    public void sendForgotPasswordEmail (String email)
        throws ServiceException;

    /**
     * Updates the email address on file for this account.
     */
    public void updateEmail (WebIdent ident, String newEmail)
        throws ServiceException;

    /**
     * Updates the email preferences for this account.
     */
    public void updateEmailPrefs (WebIdent ident, boolean emailOnWhirledMail,
                                  boolean emailAnnouncements)
        throws ServiceException;

    /**
     * Updates the password on file for this account.
     */
    public void updatePassword (WebIdent ident, String newPassword)
        throws ServiceException;

    /**
     * Resets the password on file for the specified account to the new value.
     */
    public boolean resetPassword (int memberId, String code, String newPassword)
        throws ServiceException;

    /**
     * Configures the permaname for this account.
     */
    public void configurePermaName (WebIdent ident, String permaName)
        throws ServiceException;

    /**
     * fetches the user's account info.
     */
    public AccountInfo getAccountInfo (WebIdent ident)
        throws ServiceException;

    /**
     * Updates the user's account info to match the AccountInfo object.
     */
    public void updateAccountInfo (WebIdent ident, AccountInfo info)
        throws ServiceException;
}
