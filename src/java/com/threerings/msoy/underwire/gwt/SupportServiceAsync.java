//
// $Id$

package com.threerings.msoy.underwire.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Provides the asynchronous version of {@link SupportService}.
 */
public interface SupportServiceAsync
{
    /**
     * The async version of {@link SupportService#setSocialStatus}.
     */
    void setSocialStatus (int memberId, MsoyAccount.SocialStatus status, AsyncCallback<Void> callback);
}
