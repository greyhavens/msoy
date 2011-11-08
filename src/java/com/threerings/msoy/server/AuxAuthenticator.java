//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;

import com.threerings.util.MessageBundle;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.server.ChainedAuthenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.orth.data.AuthName;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.server.AffiliateCookie;

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
    public boolean shouldHandleConnection (AuthingConnection conn)
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
            String accountName, name, token;

            // if the client supplied no token, create a new permaguest account
            if (creds.sessionToken == null) {
                MemberRecord guest = null;

                // maybe a returning guest?
                if (creds.getUsername() != null) {
                    try {
                        accountName = creds.getUsername().toString().toLowerCase();
                        accountName = _accountLogic.getDomain(accountName).authenticateAccount(
                            accountName, AccountLogic.PERMAGUEST_PASSWORD).accountName;
                        guest = _memberRepo.loadMember(accountName);
                    } catch (ServiceException se) {
                        // purged permaguests fall through and get a new permaguest account
                        if (!AuthLogic.fixPurgedPermaguest(se, creds)) {
                            throw se;
                        }
                    }
                }

                // a brand new guest (or one returning after a long sojourn)
                if (guest == null || guest.isDeleted()) {
                    guest = _accountLogic.createGuestAccount(
                        conn.getInetAddress().toString(), creds.visitorId, creds.themeId,
                        AffiliateCookie.fromCreds(creds.affiliateId));
                }

                memberId = guest.memberId;
                accountName = guest.accountName;
                name = guest.name;
                token = _memberRepo.startOrJoinSession(guest.memberId);

            } else {
                MemberRecord member = _memberRepo.loadMemberForSession(creds.sessionToken);
                if (member == null || member.isDeleted()) {
                    throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
                }

                memberId = member.memberId;
                accountName = member.accountName;
                name = member.name;
                rsp.authdata = member.toTokenRing(); // export our access control tokens
                token = creds.sessionToken;
            }

            // fill in the appropriate AuthName instance into their connection
            conn.setAuthName(createName(accountName, memberId));

            // always return the session token (even if they gave it to us in the first place)
            ((MsoyAuthResponseData)rdata).sessionToken = token;

            // TODO: other MsoyAuthResponseData fields?

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

    @Override
    protected AuthResponseData createResponseData ()
    {
        return new MsoyAuthResponseData();
    }

    protected Class<T> _credsClass;

    @Inject protected AccountLogic _accountLogic;
    @Inject protected AuthLogic _authLogic;
    @Inject protected MemberRepository _memberRepo;
}
