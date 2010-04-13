//
// $Id$

package com.threerings.msoy.chat.server;

import javax.annotation.Generated;

import com.threerings.msoy.chat.client.JabberService;
import com.threerings.msoy.data.all.JabberName;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link JabberService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from JabberService.java.")
public interface JabberProvider extends InvocationProvider
{
    /**
     * Handles a {@link JabberService#registerIM} request.
     */
    void registerIM (ClientObject caller, String arg1, String arg2, String arg3, InvocationService.InvocationListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link JabberService#sendMessage} request.
     */
    void sendMessage (ClientObject caller, JabberName arg1, String arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link JabberService#unregisterIM} request.
     */
    void unregisterIM (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
