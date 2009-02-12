//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.server.ChainedAuthenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.msoy.data.AuthName;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Handles the standard parts of the MSOY authentication process.
 */
public abstract class AuxAuthenticator<T extends MsoyCredentials> extends ChainedAuthenticator
{
    protected AuxAuthenticator (Class<T> credsClass)
    {
        _credsClass = credsClass;
    }

    @Override // from abstract ChainedAuthenticator
    protected boolean shouldHandleConnection (AuthingConnection conn)
    {
        return _credsClass.isInstance(conn.getAuthRequest().getCredentials());
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

            // cast their credentials to the appropriate class (this won't fail because we already
            // checked the type in shouldHandleConnection)
            creds = _credsClass.cast(req.getCredentials());

            // we'll be figuring these things out shortly
            int memberId;
            String accountName, name;

            // if this is a guest, see if they supplied a guest id and if not, assign one
            if (MsoyCredentials.isGuestSessionToken(creds.sessionToken)) {
                memberId = MsoyCredentials.getGuestMemberId(creds.sessionToken);
                Name credsName = creds.getUsername();
                accountName = credsName != null ? credsName.toString() :
                    MsoyAuthenticator.generateGuestName(memberId);
                name = accountName;

            } else {
                MemberRecord member = _memberRepo.loadMemberForSession(creds.sessionToken);
                if (member == null) {
                    throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
                }
                memberId = member.memberId;
                accountName = member.accountName;
                name = member.name;
                rsp.authdata = member.toTokenRing(); // export our access control tokens
            }

            // fill in the appropriate AuthName instance into their creds
            creds.setUsername(createName(accountName, memberId));

            // do our domain specific authentication (if any)
            finishAuthentication(creds, new MemberName(name, memberId));

            // if we make it here, we're all clear
            rdata.code = AuthResponseData.SUCCESS;

        } catch (ServiceException se) {
            rdata.code = se.getMessage();
            log.info("Rejecting authentication", "creds", creds, "cause", rdata.code);
        }
    }

    /**
     * Creates the appropriate {@link AuthName} derivation for the supplied name and id.
     */
    protected abstract AuthName createName (String name, int memberId);

    /**
     * Handles the domain specific parts of client authentication. If this method returns normally,
     * the session will be assumed to have been successfully authenticated. Otherwise the method
     * should throw a ServiceException configured with the cause of authentication failure.
     */
    protected void finishAuthentication (T creds, MemberName name)
        throws ServiceException
    {
        // nothing to do by default
    }

    protected Class<T> _credsClass;

    @Inject protected MemberRepository _memberRepo;
}
