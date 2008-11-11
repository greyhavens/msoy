//
// $Id$

package com.threerings.msoy.underwire.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.underwire.gwt.MsoyAccount.SocialStatus;

/**
 * The asynchronous version of {@link SupportService}.
 */
public interface SupportServiceAsync
{
    /** The asynchronous version of {@link SupportService#setSocialStatus()}. */
    void setSocialStatus (
        String authtok, int memberId, SocialStatus status, AsyncCallback<Void> callback);
}
