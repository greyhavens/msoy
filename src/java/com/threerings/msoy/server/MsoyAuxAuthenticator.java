//
// $Id$

package com.threerings.msoy.server;

import com.threerings.util.MessageBundle;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.net.Credentials;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Handles the standard parts of the MSOY authentication process.
 */
public abstract class MsoyAuxAuthenticator<T extends Credentials> extends Authenticator
{
    protected MsoyAuxAuthenticator (Class<T> credsClass)
    {
        _credsClass = credsClass;
    }

    @Override // from Authenticator
    protected void processAuthentication (AuthingConnection conn, AuthResponse rsp)
        throws Exception
    {
        AuthRequest req = conn.getAuthRequest();
        AuthResponseData rdata = rsp.getData();
        T creds = null;

        try {
            // make sure they've got the correct version
            String cvers = req.getVersion(), svers = DeploymentConfig.version;
            if (!svers.equals(cvers)) {
                log.info("Refusing wrong version", "creds", req.getCredentials(), "cvers", cvers,
                         "svers", svers);
                throw new ServiceException(
                    (cvers.compareTo(svers) > 0) ? MsoyAuthCodes.NEWER_VERSION :
                    MessageBundle.tcompose(MsoyAuthCodes.VERSION_MISMATCH, svers));
            }

            // make sure they've sent valid credentials
            try {
                creds = _credsClass.cast(req.getCredentials());
            } catch (ClassCastException cce) {
                log.warning("Invalid creds " + req.getCredentials() + ".", cce);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            // do our domain specific authentication
            processAuthentication(req, creds, rsp);

            // if we make it here, we're all clear
            rdata.code = AuthResponseData.SUCCESS;

        } catch (ServiceException se) {
            rdata.code = se.getMessage();
            log.info("Rejecting authentication", "creds", creds, "cause", rdata.code);
        }
    }

    /**
     * Handles the domain specific parts of client authentication. When this method is called, the
     * client version will have been checked and the creds are known to be of the correct type. If
     * this method returns normally, the session will be assumed to have been successfully
     * authenticated. Otherwise the method should throw a ServiceException configured with the
     * cause of authentication failure.
     */
    protected abstract void processAuthentication (AuthRequest req, T creds, AuthResponse rsp)
        throws ServiceException;

    protected Class<T> _credsClass;
}
