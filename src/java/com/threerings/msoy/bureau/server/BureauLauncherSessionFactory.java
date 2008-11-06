//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.bureau.data.BureauLauncherClientObject;
import com.threerings.msoy.bureau.data.BureauLauncherCredentials;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.SessionFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsSession;
import com.threerings.util.Name;

/**
 * Creates very basic clients for bureau launcher connections, otherwise delegates.
 */
public class BureauLauncherSessionFactory implements SessionFactory
{
    /**
     * Creates a new factory.
     * @param delegate factory to use when a non-bureau launcher connection is encountered
     */
    public BureauLauncherSessionFactory (SessionFactory delegate)
    {
        _delegate = delegate;
    }

    // from interface SessionFactory
    public Class<? extends PresentsSession> getSessionClass (AuthRequest areq)
    {
        // Just give bureau launchers a vanilla PresentsSession client.
        if (areq.getCredentials() instanceof BureauLauncherCredentials) {
            return BureauLauncherSession.class;
        } else {
            return _delegate.getSessionClass(areq);
        }
    }

    // from interface SessionFactory
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

    protected SessionFactory _delegate;
}
