//
// $Id$

package com.threerings.msoy.underwire.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous version of {@link SupportService}.
 */
public interface SupportServiceAsync
{
    /** The asynchronous version of {@link SupportService#setGreeter()}. */
    void setGreeter(
        String authtok, String accountName, boolean greeter, AsyncCallback<Void> callback);

    /** The asynchronous version of {@link SupportService#setTroublemaker()}. */
    void setTroublemaker(
        String authtok, String accountName, boolean troublemaker, AsyncCallback<Void> callback);
}
