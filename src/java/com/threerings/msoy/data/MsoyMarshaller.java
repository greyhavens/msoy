//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.client.MsoyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link MsoyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MsoyMarshaller extends InvocationMarshaller
    implements MsoyService
{
    /** The method id used to dispatch {@link #dispatchDeferredNotifications} requests. */
    public static final int DISPATCH_DEFERRED_NOTIFICATIONS = 1;

    // from interface MsoyService
    public void dispatchDeferredNotifications (Client arg1)
    {
        sendRequest(arg1, DISPATCH_DEFERRED_NOTIFICATIONS, new Object[] {});
    }

    /** The method id used to dispatch {@link #emailShare} requests. */
    public static final int EMAIL_SHARE = 2;

    // from interface MsoyService
    public void emailShare (Client arg1, boolean arg2, String arg3, int arg4, String[] arg5, String arg6, InvocationService.ConfirmListener arg7)
    {
        InvocationMarshaller.ConfirmMarshaller listener7 = new InvocationMarshaller.ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, EMAIL_SHARE, new Object[] {
            Boolean.valueOf(arg2), arg3, Integer.valueOf(arg4), arg5, arg6, listener7
        });
    }

    /** The method id used to dispatch {@link #getABTestGroup} requests. */
    public static final int GET_ABTEST_GROUP = 3;

    // from interface MsoyService
    public void getABTestGroup (Client arg1, String arg2, boolean arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, GET_ABTEST_GROUP, new Object[] {
            arg2, Boolean.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #purchaseAndSendBroadcast} requests. */
    public static final int PURCHASE_AND_SEND_BROADCAST = 4;

    // from interface MsoyService
    public void purchaseAndSendBroadcast (Client arg1, int arg2, String arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, PURCHASE_AND_SEND_BROADCAST, new Object[] {
            Integer.valueOf(arg2), arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #secureBroadcastQuote} requests. */
    public static final int SECURE_BROADCAST_QUOTE = 5;

    // from interface MsoyService
    public void secureBroadcastQuote (Client arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, SECURE_BROADCAST_QUOTE, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #setHearingGroupChat} requests. */
    public static final int SET_HEARING_GROUP_CHAT = 6;

    // from interface MsoyService
    public void setHearingGroupChat (Client arg1, int arg2, boolean arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_HEARING_GROUP_CHAT, new Object[] {
            Integer.valueOf(arg2), Boolean.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #trackClientAction} requests. */
    public static final int TRACK_CLIENT_ACTION = 7;

    // from interface MsoyService
    public void trackClientAction (Client arg1, String arg2, String arg3)
    {
        sendRequest(arg1, TRACK_CLIENT_ACTION, new Object[] {
            arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #trackTestAction} requests. */
    public static final int TRACK_TEST_ACTION = 8;

    // from interface MsoyService
    public void trackTestAction (Client arg1, String arg2, String arg3)
    {
        sendRequest(arg1, TRACK_TEST_ACTION, new Object[] {
            arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #trackVectorAssociation} requests. */
    public static final int TRACK_VECTOR_ASSOCIATION = 9;

    // from interface MsoyService
    public void trackVectorAssociation (Client arg1, String arg2)
    {
        sendRequest(arg1, TRACK_VECTOR_ASSOCIATION, new Object[] {
            arg2
        });
    }
}
