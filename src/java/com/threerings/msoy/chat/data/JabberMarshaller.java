//
// $Id$

package com.threerings.msoy.chat.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.chat.client.JabberService;
import com.threerings.msoy.data.all.JabberName;

/**
 * Provides the implementation of the {@link JabberService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from JabberService.java.")
public class JabberMarshaller extends InvocationMarshaller<ClientObject>
    implements JabberService
{
    /** The method id used to dispatch {@link #registerIM} requests. */
    public static final int REGISTER_IM = 1;

    // from interface JabberService
    public void registerIM (String arg1, String arg2, String arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(REGISTER_IM, new Object[] {
            arg1, arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #sendMessage} requests. */
    public static final int SEND_MESSAGE = 2;

    // from interface JabberService
    public void sendMessage (JabberName arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(SEND_MESSAGE, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #unregisterIM} requests. */
    public static final int UNREGISTER_IM = 3;

    // from interface JabberService
    public void unregisterIM (String arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(UNREGISTER_IM, new Object[] {
            arg1, listener2
        });
    }
}
