package com.threerings.msoy.bureau.server;

import com.threerings.msoy.bureau.data.WindowCredentials;
import com.threerings.presents.data.AuthCodes;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.server.ChainedAuthenticator;
import com.threerings.presents.server.net.AuthingConnection;

import static com.threerings.msoy.Log.log;

/**
 * Authenticates bureau windows only.
 * TODO: use something besides username to authenticate
 */
public class WindowAuthenticator extends ChainedAuthenticator
{
    /**
     * Creates a new bureau window authenticator.
     */
    public WindowAuthenticator ()
    {
    }

    @Override // from abstract ChainedAuthenticator
    protected boolean shouldHandleConnection (AuthingConnection conn)
    {
        return (conn.getAuthRequest().getCredentials() instanceof WindowCredentials);
    }

    @Override // from Authenticator
    protected void processAuthentication (
        AuthingConnection conn,
        AuthResponse rsp)
    {
        AuthRequest req = conn.getAuthRequest();
        WindowCredentials creds = (WindowCredentials)req.getCredentials();
        if (WindowCredentials.isWindow(creds.getUsername())) {
            rsp.getData().code = AuthResponseData.SUCCESS;

        } else {
            log.warning("Invalid bureau window auth request", "creds", creds);
            rsp.getData().code = AuthCodes.SERVER_ERROR;
        }
    }
}
