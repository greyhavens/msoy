//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.client.MsoyService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link MsoyService}.
 */
public interface MsoyProvider extends InvocationProvider
{
    /**
     * Handles a {@link MsoyService#dispatchDeferredNotifications} request.
     */
    void dispatchDeferredNotifications (ClientObject caller);

    /**
     * Handles a {@link MsoyService#emailShare} request.
     */
    void emailShare (ClientObject caller, boolean arg1, String arg2, int arg3, String[] arg4, String arg5, InvocationService.ConfirmListener arg6)
        throws InvocationException;

    /**
     * Handles a {@link MsoyService#getABTestGroup} request.
     */
    void getABTestGroup (ClientObject caller, String arg1, boolean arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MsoyService#purchaseAndSendBroadcast} request.
     */
    void purchaseAndSendBroadcast (ClientObject caller, int arg1, String arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MsoyService#secureBroadcastQuote} request.
     */
    void secureBroadcastQuote (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link MsoyService#setHearingGroupChat} request.
     */
    void setHearingGroupChat (ClientObject caller, int arg1, boolean arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MsoyService#trackClientAction} request.
     */
    void trackClientAction (ClientObject caller, String arg1, String arg2);

    /**
     * Handles a {@link MsoyService#trackTestAction} request.
     */
    void trackTestAction (ClientObject caller, String arg1, String arg2);

    /**
     * Handles a {@link MsoyService#trackVectorAssociation} request.
     */
    void trackVectorAssociation (ClientObject caller, String arg1);
}
