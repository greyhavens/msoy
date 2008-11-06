//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.bureau.data.BureauCredentials;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.SessionFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsSession;
import com.threerings.util.Name;

/**
 * Overrides the usual client stuff with classes specific to msoy bureaus.
 */
public class MsoyBureauSessionFactory
    implements SessionFactory
{
    /**
     * Creats a new bureau client factory.
     */
    public MsoyBureauSessionFactory (SessionFactory delegate)
    {
        _delegate = delegate;
    }

    // from SessionFactory
    public Class<? extends PresentsSession> getSessionClass (AuthRequest areq)
    {
        // Just give bureau launchers a vanilla PresentsSession client.
        if (areq.getCredentials() instanceof BureauCredentials) {
            return MsoyBureauClient.class;
        } else {
            return _delegate.getSessionClass(areq);
        }
    }

    // from SessionFactory
    public Class<? extends ClientResolver> getClientResolverClass (Name username)
    {
        if (BureauCredentials.isBureau(username)) {
            return ClientResolver.class;
        } else {
            return _delegate.getClientResolverClass(username);
        }
    }

    SessionFactory _delegate;
}
