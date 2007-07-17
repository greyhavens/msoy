//
// $Id$

package com.threerings.msoy.web.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.Invitation;

/**
 * The asynchronous (client-side) version of {@link WebUserService}.
 */
public interface WebUserServiceAsync
{
    /**
     * The asynchronous version of {@link WebUserService#login}.
     */
    public void login (long clientVersion, String username, String password, int expireDays,
                       AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#register}.
     */
    public void register (long clientVersion, String username, String password, String displayName,
                          Date birthday, AccountInfo info, int expireDays, Invitation invite, 
                          AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#validateSession}.
     */
    public void validateSession (long clientVersion, String authtok, int expireDays,
                                 AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#getConnectConfig}.
     */
    public void getConnectConfig (AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#sendForgotPasswordEmail}.
     */
    public void sendForgotPasswordEmail (String email, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#updateEmail}.
     */
    public void updateEmail (WebIdent ident, String newEmail, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#updatePassword}.
     */
    public void updatePassword (WebIdent ident, String newPassword, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#resetPassword}.
     */
    public void resetPassword (int memberId, String code, String newPassword,
                               AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#configurePermaName}.
     */
    public void configurePermaName (WebIdent ident, String permaName, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#getAccountInfo}.
     */
    public void getAccountInfo (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link WebUserService#updateAccountInfo}.
     */
    public void updateAccountInfo (WebIdent ident, AccountInfo info, AsyncCallback callback);
}
