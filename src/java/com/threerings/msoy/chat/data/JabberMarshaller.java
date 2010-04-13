//
// $Id$

package com.threerings.msoy.chat.data;

import javax.annotation.Generated;

import com.threerings.msoy.chat.client.JabberService;
import com.threerings.msoy.data.all.JabberName;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link JabberService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from JabberService.java.")
public class JabberMarshaller extends InvocationMarshaller
    implements JabberService
{
    /** The method id used to dispatch {@link #registerIM} requests. */
    public static final int REGISTER_IM = 1;

    // from interface JabberService
    public void registerIM (Client arg1, String arg2, String arg3, String arg4, InvocationService.InvocationListener arg5)
    {
        ListenerMarshaller listener5 = new ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, REGISTER_IM, new Object[] {
            arg2, arg3, arg4, listener5
        });
    }

    /** The method id used to dispatch {@link #sendMessage} requests. */
    public static final int SEND_MESSAGE = 2;

    // from interface JabberService
    public void sendMessage (Client arg1, JabberName arg2, String arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SEND_MESSAGE, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #unregisterIM} requests. */
    public static final int UNREGISTER_IM = 3;

    // from interface JabberService
    public void unregisterIM (Client arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UNREGISTER_IM, new Object[] {
            arg2, listener3
        });
    }
}
