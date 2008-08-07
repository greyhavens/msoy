//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.RegisterInfo;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.SessionData;

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
    SessionData login (String clientVersion, String email, String password, int expireDays)
        throws ServiceException;

    /**
     * Requests that an account be created for the specified user. The user will be logged in after
     * the account is created.
     *
     * @return a set of credentials including a session cookie that should be provided to
     * subsequent remote service calls that require authentication.
     */
    SessionData register (String clientVersion, RegisterInfo info)
        throws ServiceException;

    /**
     * Validates that the supplied session token is still active and refreshes its expiration time
     * if so.
     */
    SessionData validateSession (String clientVersion, String authtok, int expireDays)
        throws ServiceException;

    /**
     * Returns the connection information for this server's World services.
     */
    ConnectConfig getConnectConfig ()
        throws ServiceException;

    /**
     * Loads the configuration needed to play (launch) the specified game.
     */
    LaunchConfig loadLaunchConfig (int gameId, boolean assignGuestId)
        throws ServiceException;

    /**
     * Sends a "forgot my password" email to the account registered with the supplied address.
     */
    void sendForgotPasswordEmail (String email)
        throws ServiceException;

    /**
     * Updates the email address on file for this account.
     */
    void updateEmail (String newEmail)
        throws ServiceException;

    /**
     * Updates the email preferences for this account.
     */
    void updateEmailPrefs (boolean emailOnWhirledMail,
                                  boolean emailAnnouncements)
        throws ServiceException;

    /**
     * Updates the password on file for this account.
     */
    void updatePassword (String newPassword)
        throws ServiceException;

    /**
     * Resets the password on file for the specified account to the new value.
     */
    boolean resetPassword (int memberId, String code, String newPassword)
        throws ServiceException;

    /**
     * Configures the permaname for this account.
     */
    void configurePermaName (String permaName)
        throws ServiceException;

    /**
     * fetches the user's account info.
     */
    AccountInfo getAccountInfo ()
        throws ServiceException;

    /**
     * Updates the user's account info to match the AccountInfo object.
     */
    void updateAccountInfo (AccountInfo info)
        throws ServiceException;
}
