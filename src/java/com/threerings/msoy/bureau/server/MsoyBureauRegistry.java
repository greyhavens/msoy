//
// $Id$

package com.threerings.msoy.bureau.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.ServiceAuthenticator;
import com.threerings.presents.server.SessionFactory;
import com.threerings.presents.server.net.PresentsConnectionManager;

import com.threerings.bureau.server.BureauRegistry;
import com.threerings.bureau.server.BureauSession;

import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.bureau.data.BureauLauncherAuthName;
import com.threerings.msoy.bureau.data.BureauLauncherClientObject;
import com.threerings.msoy.bureau.data.BureauLauncherCreds;
import com.threerings.msoy.bureau.data.WindowAuthName;
import com.threerings.msoy.bureau.data.WindowClientObject;
import com.threerings.msoy.bureau.data.WindowCredentials;

/**
 * Customizes the stock BureauRegistry with some MSOY bits.
 */
@Singleton
public class MsoyBureauRegistry extends BureauRegistry
{
    @Inject public MsoyBureauRegistry (
        InvocationManager invmgr, PresentsConnectionManager conmgr, ClientManager clmgr)
    {
        super(invmgr, conmgr, clmgr);

        // wire up authenticators for our window and bureau launcher clients
        conmgr.addChainedAuthenticator(new ServiceAuthenticator<WindowCredentials>(
                WindowCredentials.class, WindowAuthName.class) {
            protected boolean areValid (WindowCredentials creds) {
                return creds.areValid(_sharedSecret);
            }
            // we double-MD5 because the secret is passed on the command line to the client
            protected String _sharedSecret = StringUtil.md5hex(ServerConfig.windowSharedSecret);
        });
        conmgr.addChainedAuthenticator(new ServiceAuthenticator<BureauLauncherCreds>(
                BureauLauncherCreds.class, BureauLauncherAuthName.class) {
            protected boolean areValid (BureauLauncherCreds creds) {
                return creds.areValid(ServerConfig.bureauSharedSecret);
            }
        });

        // wire session factories for our window and bureau launcher clients
        clmgr.addSessionFactory(SessionFactory.newSessionFactory(
                                    WindowCredentials.class, WindowSession.class,
                                    WindowAuthName.class, WindowClientResolver.class));
        clmgr.addSessionFactory(SessionFactory.newSessionFactory(
                                    BureauLauncherCreds.class, BureauLauncherSession.class,
                                    BureauLauncherAuthName.class, BureauLauncherResolver.class));
    }

    @Override // from BureauRegistry
    protected Class<? extends BureauSession> getSessionClass ()
    {
        return MsoyBureauClient.class;
    }

    protected static class WindowClientResolver extends ClientResolver
    {
        @Override public ClientObject createClientObject () {
            return new WindowClientObject();
        }
        @Override protected void resolveClientData (ClientObject clobj) throws Exception {
            super.resolveClientData(clobj);
            ((WindowClientObject)clobj).bureauId = _username.toString();
        }
    }

    protected static class BureauLauncherResolver extends ClientResolver
    {
        @Override public ClientObject createClientObject () {
            return new BureauLauncherClientObject();
        }
    }

    protected static class BureauLauncherSession extends PresentsSession {
        @Override protected void sessionWillStart () {
            super.sessionWillStart();
            BureauLauncherClientObject clobj = (BureauLauncherClientObject)getClientObject();
            clobj.hostname = getInetAddress().getHostName();
        }
    }
}
