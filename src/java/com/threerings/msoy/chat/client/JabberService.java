//
// $Id$

package com.threerings.msoy.chat.client;

import com.threerings.msoy.data.all.JabberName;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.presents.client.InvocationService.ResultListener;

/**
 * Defines Jabber related invocation services.
 */
public interface JabberService extends InvocationService
{
    /**
     * Registers a client with an IM gateway and logs them in.
     */
    public void registerIM (Client client, String gateway, String username, String password,
            InvocationListener listener);

    /**
     * Unregisters a client with an IM gateway.
     */
    public void unregisterIM (Client client, String gateway, InvocationListener listener);

    /**
     * Sends a message to a Jabber contact.
     */
    public void sendMessage (Client client, JabberName name, String message, ResultListener listener);
}
