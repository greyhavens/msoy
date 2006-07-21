//
// $Id$

package com.threerings.msoy.server;

import com.threerings.presents.server.Authenticator;

import com.threerings.msoy.web.client.LogonException;
import com.threerings.msoy.web.client.WebCreds;

/**
 * Extends the standard {@link Authenticator} and defines some additional
 * MetaSOY authentication requirements.
 */
public abstract class MsoyAuthenticator extends Authenticator
{
    /**
     * Authenticates a web sesssion, verifying the supplied username and
     * password and creating (or reusing) a record in a session repository that
     * can be used to authenticate for the duration of that session.
     *
     * @param username the account to be authenticated.
     * @param password the MD5 encrypted password for this account.
     * @param persist if true the session should persist for some long but not
     * infinite duration (a month), if false the session should be scheduled to
     * expire in a day or two (the client will be instructed to expire the
     * session token when it next terminates).
     *
     * @return the session credentials that should be supplied to web-based
     * service requests for authentication.
     *
     * @exception LogonException thrown if the password is incorrect, the user
     * does not exist or some other problem occurs with logon.
     */
    public abstract WebCreds authenticateSession (
            String username, String password, boolean persist)
        throws LogonException;
}
