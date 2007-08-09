//
// $Id$

package com.threerings.msoy.web.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link AdminService}.
 */
public interface AdminServiceAsync
{
    /** 
     * The asynchronous version of {@link grantInvitations}.
     */
    public void grantInvitations (WebIdent ident, int numberInvitations, Date activeSince,
                                  AsyncCallback callback);

    /** 
     * The asynchronous version of {@link grantInvitations}.
     */
    public void grantInvitations (WebIdent ident, int numberInvitations, int memberId,
                                  AsyncCallback callback);

    /**
     * The asynchronous version of {@link getPlayerList}.
     */
    public void getPlayerList (WebIdent ident, int inviterId, AsyncCallback callback);
}
