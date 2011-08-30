//
// $Id$

package com.threerings.msoy.chat.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.data.all.JabberName;

/**
 * Defines Jabber related invocation services.
 */
public interface JabberService extends InvocationService
{
    /**
     * Registers a client with an IM gateway and logs them in.
     */
    public void registerIM (String gateway, String username, String password,
            InvocationListener listener);

    /**
     * Unregisters a client with an IM gateway.
     */
    void unregisterIM (String gateway, InvocationListener listener);

    /**
     * Sends a message to a Jabber contact.
     */
    void sendMessage (JabberName name, String message, ResultListener listener);
}
