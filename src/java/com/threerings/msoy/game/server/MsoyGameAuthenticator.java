//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.game.data.MsoyGameCredentials;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Handles authentication on an MSOY Game server.
 */
@Singleton
public class MsoyGameAuthenticator extends Authenticator
{
    // from abstract Authenticator
    protected void processAuthentication (AuthingConnection conn, AuthResponse rsp)
        throws PersistenceException
    {
        AuthRequest req = conn.getAuthRequest();
        AuthResponseData rdata = rsp.getData();
        MsoyGameCredentials creds = null;

        try {
            // make sure they've got the correct version
            String cvers = req.getVersion(), svers = DeploymentConfig.version;
            if (!svers.equals(cvers)) {
                log.info("Refusing wrong version [creds=" + req.getCredentials() +
                         ", cvers=" + cvers + ", svers=" + svers + "].");
                throw new ServiceException(
                    (cvers.compareTo(svers) > 0) ? MsoyAuthCodes.NEWER_VERSION :
                    MessageBundle.tcompose(MsoyAuthCodes.VERSION_MISMATCH, svers));
            }

            // make sure they've sent valid credentials
            try {
                creds = (MsoyGameCredentials) req.getCredentials();
            } catch (ClassCastException cce) {
                log.warning("Invalid creds " + req.getCredentials() + ".", cce);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            if (StringUtil.isBlank(creds.sessionToken)) {
                log.warning("Receieved session-tokenless auth request " + req + ".");
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            if (MsoyCredentials.isGuestSessionToken(creds.sessionToken)) {
                // extract their assigned member id from their token
                int memberId = MsoyCredentials.getGuestMemberId(creds.sessionToken);
                creds.setUsername(new MemberName(String.valueOf(creds.getUsername()), memberId));

            } else {
                MemberRecord member = _memberRepo.loadMemberForSession(creds.sessionToken);
                if (member == null) {
                    throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
                }

                // set their starting username to their auth username
                creds.setUsername(new Name(member.accountName));

                // replace the tokens provided by the Domain with tokens derived from their member
                // record (a newly created record will have its bits set from the Domain values)
                int tokens = 0;
                if (member.isSet(MemberRecord.Flag.ADMIN)) {
                    tokens |= MsoyTokenRing.ADMIN;
                    tokens |= MsoyTokenRing.SUPPORT;
                } else if (member.isSet(MemberRecord.Flag.SUPPORT)) {
                    tokens |= MsoyTokenRing.SUPPORT;
                }
                rsp.authdata = new MsoyTokenRing(tokens);
            }

            // log.info("User logged on [user=" + user.username + "].");
            rdata.code = AuthResponseData.SUCCESS;

        } catch (ServiceException se) {
            rdata.code = se.getMessage();
            log.info("Rejecting authentication [creds=" + creds + ", code=" + rdata.code + "].");
        }
    }

    @Inject protected MemberRepository _memberRepo;

    /** Used to assign unique usernames to guests that authenticate with the server. */
    protected static int _guestCount;
}
