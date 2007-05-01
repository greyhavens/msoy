//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.Invitation;

/**
 * Defines general user services available to the GWT/AJAX web client.
 */
public interface WebUserService extends RemoteService
{
    /**
     * Requests that the client be logged in as the specified user with the supplied (MD5-encoded)
     * password.
     *
     * @return a set of credentials including a session cookie that should be provided to
     * subsequent remote service calls that require authentication.
     */
    public WebCreds login (long clientVersion, String email, String password, int expireDays)
        throws ServiceException;

    /**
     * Requests that an account be created for the specified user. The user will be logged in after
     * the account is created.
     *
     * @return a set of credentials including a session cookie that should be provided to
     * subsequent remote service calls that require authentication.
     */
    public WebCreds register (long clientVersion, String email, String password, String displayName,
                              int expireDays, Invitation invite)
        throws ServiceException;

    /**
     * Validates that the supplied session token is still active and refreshes its expiration time
     * if so.
     */
    public WebCreds validateSession (long clientVersion, String authtok, int expireDays)
        throws ServiceException;

    /**
     * Updates the email address on file for this account.
     */
    public void updateEmail (WebCreds creds, String newEmail)
        throws ServiceException;

    /**
     * Updates the password on file for this account.
     */
    public void updatePassword (WebCreds creds, String newPassword)
        throws ServiceException;

    /**
     * Configures the permaname for this account.
     */
    public void configurePermaName (WebCreds creds, String permaName)
        throws ServiceException;
}
