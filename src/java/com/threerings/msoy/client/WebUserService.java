//
// $Id$

package com.threerings.msoy.client;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * Defines general user services available to the GWT/AJAX web client.
 */
public interface WebUserService extends RemoteService
{
    /**
     * Requests that the client be logged in as the specified user with the
     * supplied (MD5-encoded) password.
     *
     * @return a session cookie 
     *
     * @throws LogonException containing a translatable error message if the
     * password was incorrect or the user does not exist or if some other
     * failure was encountered.
     */
    public String login (String username, String password)
        throws LogonException;
}
