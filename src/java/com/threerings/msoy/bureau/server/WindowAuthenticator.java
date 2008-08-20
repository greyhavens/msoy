//
// $Id$

package com.threerings.msoy.bureau.server;

import com.samskivert.util.StringUtil;
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
    public WindowAuthenticator (String sharedSecret)
    {
        _token = StringUtil.md5hex(sharedSecret);
    }

    @Override // from abstract ChainedAuthenticator
    protected boolean shouldHandleConnection (AuthingConnection conn)
    {
        return (conn.getAuthRequest().getCredentials() instanceof WindowCredentials);
    }

    @Override // from Authenticator
    protected void processAuthentication (AuthingConnection conn, AuthResponse rsp)
    {
        AuthRequest req = conn.getAuthRequest();
        WindowCredentials creds = (WindowCredentials)req.getCredentials();
        if (WindowCredentials.isWindow(creds.getUsername())) {

            if (creds.getToken().equals(_token)) {
                rsp.getData().code = AuthResponseData.SUCCESS;

            } else {
                log.warning("Wrong token on bureau window auth request", "creds", creds);
                rsp.getData().code = AuthCodes.INVALID_PASSWORD;
            }

        } else {
            log.warning("Invalid bureau window auth request", "creds", creds);
            rsp.getData().code = AuthCodes.SERVER_ERROR;
        }
    }

    protected String _token;
}
