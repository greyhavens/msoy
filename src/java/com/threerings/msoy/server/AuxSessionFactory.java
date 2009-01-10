//
// $Id$

package com.threerings.msoy.server;

import com.threerings.util.Name;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.Credentials;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.SessionFactory;

/**
 * A delegating client factory for auxiliary services.
 */
public class AuxSessionFactory implements SessionFactory
{
    public AuxSessionFactory (SessionFactory delegate,
                              Class<? extends Credentials> credsClass,
                              Class<? extends Name> nameClass,
                              Class<? extends PresentsSession> sessionClass,
                              Class<? extends ClientResolver> resolverClass)
    {
        _delegate = delegate;
        _credsClass = credsClass;
        _nameClass = nameClass;
        _sessionClass = sessionClass;
        _resolverClass = resolverClass;
    }

    // from interface SessionFactory
    public Class<? extends PresentsSession> getSessionClass (AuthRequest areq)
    {
        return _credsClass.isInstance(areq.getCredentials()) ?
            _sessionClass : _delegate.getSessionClass(areq);
    }

    // from interface SessionFactory
    public Class<? extends ClientResolver> getClientResolverClass (Name username)
    {
        return _nameClass.isInstance(username) ?
            _resolverClass : _delegate.getClientResolverClass(username);
    }

    protected SessionFactory _delegate;
    protected Class<? extends Credentials> _credsClass;
    protected Class<? extends Name> _nameClass;
    protected Class<? extends PresentsSession> _sessionClass;
    protected Class<? extends ClientResolver> _resolverClass;
}
