//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.bureau.data.WindowCredentials;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsClient;
import com.threerings.util.Name;

import static com.threerings.msoy.Log.log;

/**
 * Creates very basic clients for bureau window connections, otherwise delegates.
 */
public class WindowClientFactory implements ClientFactory
{
    /** 
     * Creates a new factory.
     * @param delegate factory to use when a non-window launcher connection is encountered
     */
    public WindowClientFactory (ClientFactory delegate)
    {
        _delegate = delegate;
    }

    // from interface ClientFactory
    public Class<? extends PresentsClient> getClientClass (AuthRequest areq)
    {
        if (areq.getCredentials() instanceof WindowCredentials) {
            return WindowServerClient.class;

        } else {
            return _delegate.getClientClass(areq);
        }
    }

    // from interface ClientFactory
    public Class<? extends ClientResolver> getClientResolverClass (Name username)
    {
        String prefix = WindowCredentials.PREFIX;

        // Just give bureau windows a vanilla ClientResolver.
        if (WindowCredentials.isWindow(username)) {
            return ClientResolver.class;
        } else {
            return _delegate.getClientResolverClass(username);
        }
    }

    protected ClientFactory _delegate;
}
