//
// $Id$

package com.threerings.msoy.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserManager;
import com.threerings.user.OOOUserRepository;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.web.data.LogonException;

import static com.threerings.msoy.Log.log;

/**
 * Implements account authentication against the OOO global user database.
 */
public class OOOAuthenticationDomain
    implements MsoyAuthenticator.Domain
{
    // from interface MsoyAuthenticator.Domain
    public void init ()
        throws PersistenceException
    {
        // we get our user manager configuration from the ocean config
        _usermgr = new OOOUserManager(
            ServerConfig.config.getSubProperties("oooauth"),
            MsoyServer.conProv);
        _authrep = (OOOUserRepository)_usermgr.getRepository();
    }

    // from interface MsoyAuthenticator.Domain
    public MsoyAuthenticator.Account authenticateAccount (
        String accountName, String password)
        throws LogonException, PersistenceException
    {
        // load up their user account record
        OOOUser user = _authrep.loadUser(accountName, true);
        if (user == null) {
            throw new LogonException(MsoyAuthCodes.NO_SUCH_USER);
        }

        // now check their password
        if (PASSWORD_BYPASS != password && !user.password.equals(password)) {
            throw new LogonException(MsoyAuthCodes.INVALID_PASSWORD);
        }

        // configure their access tokens
        int tokens = 0;
        if (user.holdsToken(OOOUser.ADMIN) ||
            user.holdsToken(OOOUser.MAINTAINER)) {
            tokens |= MsoyTokenRing.ADMIN;
            tokens |= MsoyTokenRing.SUPPORT;
        }
        if (user.holdsToken(OOOUser.SUPPORT)) {
            tokens |= MsoyTokenRing.SUPPORT;
        }

        // create and return an account record
        OOOAccount account = new OOOAccount();
        account.accountName = user.username;
        account.tokens = new MsoyTokenRing(tokens);
        account.record = user;
        return account;
    }

    // from interface MsoyAuthenticator.Domain
    public void validateAccount (MsoyAuthenticator.Account account,
                                 String machIdent, boolean firstLogon)
        throws LogonException, PersistenceException
    {
        OOOAccount oooacc = (OOOAccount)account;
        switch (_authrep.validateUser(oooacc.record, machIdent, firstLogon)) {
        case OOOUserRepository.ACCOUNT_BANNED:
            throw new LogonException(MsoyAuthCodes.BANNED);
        case OOOUserRepository.NEW_ACCOUNT_TAINTED:
            throw new LogonException(MsoyAuthCodes.MACHINE_TAINTED);
        }
        // TODO: do we care about other badness like DEADBEAT?
    }

    // from interface MsoyAuthenticator.Domain
    public void validateAccount (MsoyAuthenticator.Account account)
        throws LogonException, PersistenceException
    {
        OOOAccount oooacc = (OOOAccount)account;
        if (oooacc.record.isBanned()) {
            throw new LogonException(MsoyAuthCodes.BANNED);
        }
        // TODO: do we care about other badness like DEADBEAT?
    }

    protected static class OOOAccount extends MsoyAuthenticator.Account
    {
        public OOOUser record;
    }

    protected OOOUserRepository _authrep;
    protected OOOUserManager _usermgr;
}
