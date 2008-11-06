//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.bureau.data.WindowClientObject;
import com.threerings.msoy.bureau.data.WindowCredentials;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsSession;
import com.threerings.util.Name;

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
    public Class<? extends PresentsSession> getClientClass (AuthRequest areq)
    {
        if (areq.getCredentials() instanceof WindowCredentials) {
            return WindowSession.class;

        } else {
            return _delegate.getClientClass(areq);
        }
    }

    // from interface ClientFactory
    public Class<? extends ClientResolver> getClientResolverClass (Name username)
    {
        // Just give bureau windows a vanilla ClientResolver.
        if (WindowCredentials.isWindow(username)) {
            return MyClientResolver.class;
        } else {
            return _delegate.getClientResolverClass(username);
        }
    }

    protected static class MyClientResolver extends ClientResolver
    {
        @Override
        public ClientObject createClientObject ()
        {
            return new WindowClientObject();
        }

        @Override
        protected void resolveClientData (ClientObject clobj)
            throws Exception
        {
            super.resolveClientData(clobj);
            
            String bureauId = WindowCredentials.extractBureauId(_username);
            ((WindowClientObject)clobj).bureauId = bureauId;
        }
    }
    
    protected ClientFactory _delegate;
}
