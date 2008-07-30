//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.msoy.chat.data.JabberMarshaller;
import com.threerings.msoy.data.all.JabberName;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link JabberProvider}.
 */
public class JabberDispatcher extends InvocationDispatcher<JabberMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public JabberDispatcher (JabberProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public JabberMarshaller createMarshaller ()
    {
        return new JabberMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case JabberMarshaller.REGISTER_IM:
            ((JabberProvider)provider).registerIM(
                source, (String)args[0], (String)args[1], (String)args[2], (InvocationService.InvocationListener)args[3]
            );
            return;

        case JabberMarshaller.SEND_MESSAGE:
            ((JabberProvider)provider).sendMessage(
                source, (JabberName)args[0], (String)args[1], (InvocationService.ResultListener)args[2]
            );
            return;

        case JabberMarshaller.UNREGISTER_IM:
            ((JabberProvider)provider).unregisterIM(
                source, (String)args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
