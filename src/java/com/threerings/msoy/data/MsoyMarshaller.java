//
// $Id$

package com.threerings.msoy.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.client.MsoyService;

/**
 * Provides the implementation of the {@link MsoyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from MsoyService.java.")
public class MsoyMarshaller extends InvocationMarshaller<ClientObject>
    implements MsoyService
{
    /** The method id used to dispatch {@link #dispatchDeferredNotifications} requests. */
    public static final int DISPATCH_DEFERRED_NOTIFICATIONS = 1;

    // from interface MsoyService
    public void dispatchDeferredNotifications ()
    {
        sendRequest(DISPATCH_DEFERRED_NOTIFICATIONS, new Object[] {
        });
    }

    /** The method id used to dispatch {@link #emailShare} requests. */
    public static final int EMAIL_SHARE = 2;

    // from interface MsoyService
    public void emailShare (boolean arg1, String arg2, int arg3, String[] arg4, String arg5, boolean arg6, InvocationService.ConfirmListener arg7)
    {
        InvocationMarshaller.ConfirmMarshaller listener7 = new InvocationMarshaller.ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(EMAIL_SHARE, new Object[] {
            Boolean.valueOf(arg1), arg2, Integer.valueOf(arg3), arg4, arg5, Boolean.valueOf(arg6), listener7
        });
    }

    /** The method id used to dispatch {@link #getABTestGroup} requests. */
    public static final int GET_ABTEST_GROUP = 3;

    // from interface MsoyService
    public void getABTestGroup (String arg1, boolean arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(GET_ABTEST_GROUP, new Object[] {
            arg1, Boolean.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #purchaseAndSendBroadcast} requests. */
    public static final int PURCHASE_AND_SEND_BROADCAST = 4;

    // from interface MsoyService
    public void purchaseAndSendBroadcast (int arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(PURCHASE_AND_SEND_BROADCAST, new Object[] {
            Integer.valueOf(arg1), arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #secureBroadcastQuote} requests. */
    public static final int SECURE_BROADCAST_QUOTE = 5;

    // from interface MsoyService
    public void secureBroadcastQuote (InvocationService.ResultListener arg1)
    {
        InvocationMarshaller.ResultMarshaller listener1 = new InvocationMarshaller.ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(SECURE_BROADCAST_QUOTE, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #setHearingGroupChat} requests. */
    public static final int SET_HEARING_GROUP_CHAT = 6;

    // from interface MsoyService
    public void setHearingGroupChat (int arg1, boolean arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(SET_HEARING_GROUP_CHAT, new Object[] {
            Integer.valueOf(arg1), Boolean.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #trackTestAction} requests. */
    public static final int TRACK_TEST_ACTION = 7;

    // from interface MsoyService
    public void trackTestAction (String arg1, String arg2)
    {
        sendRequest(TRACK_TEST_ACTION, new Object[] {
            arg1, arg2
        });
    }
}
