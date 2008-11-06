//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.bureau.data.BureauLauncherClientObject;
import com.threerings.msoy.bureau.data.BureauLauncherCredentials;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsSession;
import com.threerings.util.Name;

/**
 * Creates very basic clients for bureau launcher connections, otherwise delegates.
 */
public class BureauLauncherClientFactory implements ClientFactory
{
    /**
     * Creates a new factory.
     * @param delegate factory to use when a non-bureau launcher connection is encountered
     */
    public BureauLauncherClientFactory (ClientFactory delegate)
    {
        _delegate = delegate;
    }

    // from interface ClientFactory
    public Class<? extends PresentsSession> getClientClass (AuthRequest areq)
    {
        // Just give bureau launchers a vanilla PresentsSession client.
        if (areq.getCredentials() instanceof BureauLauncherCredentials) {
            return BureauLauncherServerClient.class;
        } else {
            return _delegate.getClientClass(areq);
        }
    }

    // from interface ClientFactory
    public Class<? extends ClientResolver> getClientResolverClass (Name username)
    {
        String prefix = BureauLauncherCredentials.PREFIX;

        // Just give bureau launchers a vanilla ClientResolver.
        if (username.toString().startsWith(prefix)) {
            return Resolver.class;
        } else {
            return _delegate.getClientResolverClass(username);
        }
    }

    protected static class Resolver extends ClientResolver
    {
        public ClientObject createClientObject ()
        {
            return new BureauLauncherClientObject();
        }
    }

    protected ClientFactory _delegate;
}
