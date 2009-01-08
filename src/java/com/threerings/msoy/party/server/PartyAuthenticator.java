//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;
import com.samskivert.util.StringUtil;

import com.threerings.util.Name;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.MsoyAuxAuthenticator;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.party.data.PartyCredentials;

import static com.threerings.msoy.Log.log;

/**
 * Handles partier authentication.
 */
public class PartyAuthenticator extends MsoyAuxAuthenticator<PartyCredentials>
{
    protected PartyAuthenticator ()
    {
        super(PartyCredentials.class);
    }

    @Override // from MsoyAuxAuthenticator
    protected void processAuthentication (AuthRequest req, PartyCredentials creds, AuthResponse rsp)
        throws ServiceException
    {
        if (StringUtil.isBlank(creds.sessionToken)) {
            log.warning("Receieved session-tokenless auth request " + req + ".");
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        MemberRecord member = _memberRepo.loadMemberForSession(creds.sessionToken);
        if (member == null) {
            throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
        }

        // set their starting username to their auth username
        creds.setUsername(new Name(member.accountName));

        // fill in our access control tokens
        rsp.authdata = member.toTokenRing();
    }

    @Inject protected MemberRepository _memberRepo;
}
