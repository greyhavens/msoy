//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.bureau.data.BureauLauncherCredentials;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.presents.data.AuthCodes;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.server.ChainedAuthenticator;
import com.threerings.presents.server.net.AuthingConnection;

import static com.threerings.msoy.Log.log;

/**
 * Authenticator for incoming connections from a bureau launcher.
 * This is expected to be used by msoy game servers.
 */
public class BureauLauncherAuthenticator extends ChainedAuthenticator
{
    /** Creates a new bureau launcher authenticator. */
    public BureauLauncherAuthenticator ()
    {
    }

    @Override // from abstract ChainedAuthenticator
    protected boolean shouldHandleConnection (AuthingConnection conn)
    {
        return (conn.getAuthRequest().getCredentials()
                instanceof BureauLauncherCredentials);
    }

    @Override // from abstract Authenticator
    protected void processAuthentication (AuthingConnection conn, AuthResponse rsp)
    {
        // here, we are ONLY authenticating bureau launchers
        AuthRequest req = conn.getAuthRequest();
        BureauLauncherCredentials launcherCreds =
            (BureauLauncherCredentials) req.getCredentials();

        String password = BureauLauncherCredentials.createPassword(
            launcherCreds.getNodeName(), ServerConfig.bureauSharedSecret);

        if (password.equals(launcherCreds.getPassword())) {
            rsp.getData().code = AuthResponseData.SUCCESS;

        } else {
            log.warning("Received invalid bureau launcher auth request",
                        "creds", launcherCreds);
            rsp.getData().code = AuthCodes.SERVER_ERROR;
        }
    }
}
