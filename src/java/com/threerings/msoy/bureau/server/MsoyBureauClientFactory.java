//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.bureau.data.BureauCredentials;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsSession;
import com.threerings.util.Name;

/**
 * Overrides the usual client stuff with classes specific to msoy bureaus.
 */
public class MsoyBureauClientFactory
    implements ClientFactory
{
    /**
     * Creats a new bureau client factory.
     */
    public MsoyBureauClientFactory (ClientFactory delegate)
    {
        _delegate = delegate;
    }

    // from ClientFactory
    public Class<? extends PresentsSession> getClientClass (AuthRequest areq)
    {
        // Just give bureau launchers a vanilla PresentsSession client.
        if (areq.getCredentials() instanceof BureauCredentials) {
            return MsoyBureauClient.class;
        } else {
            return _delegate.getClientClass(areq);
        }
    }

    // from ClientFactory
    public Class<? extends ClientResolver> getClientResolverClass (Name username)
    {
        if (BureauCredentials.isBureau(username)) {
            return ClientResolver.class;
        } else {
            return _delegate.getClientResolverClass(username);
        }
    }

    ClientFactory _delegate;
}
