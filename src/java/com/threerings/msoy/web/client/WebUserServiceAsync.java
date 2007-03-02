//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link WebUserService}.
 */
public interface WebUserServiceAsync
{
    /**
     * The asynchronous version of {@link WebUserService#login}.
     */
    public void login (String username, String password, int expireDays, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#register}.
     */
    public void register (String username, String password, String displayName, int expireDays,
                          AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#validateSession}.
     */
    public void validateSession (String authtok, int expireDays, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#updateEmail}.
     */
    public void updateEmail (WebCreds creds, String newEmail, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#updatePassword}.
     */
    public void updatePassword (WebCreds creds, String newPassword, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#configurePermaName}.
     */
    public void configurePermaName (WebCreds creds, String permaName, AsyncCallback callback);
}
