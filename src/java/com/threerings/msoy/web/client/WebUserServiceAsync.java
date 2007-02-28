//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

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
}
