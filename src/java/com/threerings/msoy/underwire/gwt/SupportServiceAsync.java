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
    void setSocialStatus (int memberId, SocialStatus status, AsyncCallback<Void> callback);
}
