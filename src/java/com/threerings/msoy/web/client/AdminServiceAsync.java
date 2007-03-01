//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link AdminService}.
 */
public interface AdminServiceAsync
{
    /**
     * The asynchronous version of {@link AdminService#loadConnectConfig}.
     */
    public void loadConnectConfig (WebCreds creds, AsyncCallback callback);
}
