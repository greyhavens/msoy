//
// $Id$

package com.threerings.msoy.server;

import java.util.Arrays;

import com.google.inject.Inject;

import com.samskivert.net.MailUtil;
import com.samskivert.util.StringUtil;

import com.samskivert.servlet.user.InvalidUsernameException;
import com.samskivert.servlet.user.UserExistsException;
import com.samskivert.servlet.user.Username;

import com.threerings.user.OOOUser;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.underwire.server.SupportLogic;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.server.persist.MsoyOOOUserRepository;

import static com.threerings.msoy.Log.log;

/**
 * Implements account authentication against the OOO global user database.
 */
public class OOOAuthenticationDomain
    implements WorldAuthenticator.Domain
{
    // from interface WorldAuthenticator.Domain
    public WorldAuthenticator.Account createAccount (String accountName, String password)
        throws ServiceException
    {
        // make sure this account is not already in use
        if (_authrep.loadUserByEmail(accountName, false) != null) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        }

        // create a new account record
        int userId;
        try {
            userId = _authrep.createUser(new MsoyUsername(accountName), password, accountName);
        } catch (InvalidUsernameException iue) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        } catch (UserExistsException uee) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        }

        // load up our newly created record
        OOOUser user = _authrep.loadUser(userId);
        if (user == null) {
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        // create and return an account metadata record
        OOOAccount account = new OOOAccount();
        account.accountName = user.email;
        account.user = user;
        return account;
    }

    // from interface WorldAuthenticator.Domain
    public void uncreateAccount (String accountName)
    {
        OOOUser user = _authrep.loadUserByEmail(accountName, false);
        if (user != null) {
            _authrep.uncreateUser(user.userId);
        }
    }

    // from interface WorldAuthenticator.Domain
    public void updateAccount (String accountName, String newAccountName, String newPermaName,
                               String newPassword)
        throws ServiceException
    {
        // load up their user account record
        OOOUser user = _authrep.loadUserByEmail(accountName, false);
        if (user == null) {
            throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
        }

        // if they are changing their account name, do that first so that we can fail on dups
        if (newPermaName != null) {
            try {
                if (!_authrep.changeUsername(user.userId, newPermaName)) {
                    log.warning("Failed to set permaname, no such user? [account=" + accountName +
                                ", pname=" + newPermaName + "].");
                }
            } catch (UserExistsException uee) {
                throw new ServiceException(MsoyAuthCodes.DUPLICATE_PERMANAME);
            }
        }

        // update their bits and store the record back to the database
        if (newAccountName != null) {
            // if they have not yet set their permaname, change their account name to their new
            // email address to keep things in sync
            if (newPermaName == null && user.username.equals(user.email)) {
                try {
                    _authrep.changeUsername(user.userId, newAccountName);
                } catch (UserExistsException uee) {
                    log.warning("Requested to change email address to one in use.");
                    throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
                }
            }
            _authrep.changeEmail(user.userId, newAccountName);
        }
        if (newPassword != null) {
            _authrep.changePassword(user.userId, newPassword);
        }
    }

    // from interface WorldAuthenticator.Domain
    public WorldAuthenticator.Account authenticateAccount (String accountName, String password)
        throws ServiceException
    {
        // load up their user account record
        OOOUser user = _authrep.loadUserByEmail(accountName, false);
        if (user == null) {
            throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
        }

        // now check their password
        if (PASSWORD_BYPASS != password && !user.password.equals(password)) {
            throw new ServiceException(MsoyAuthCodes.INVALID_PASSWORD);
        }

        // create and return an account record
        OOOAccount account = new OOOAccount();
        account.accountName = user.email;
        account.user = user;
        return account;
    }

    // from interface WorldAuthenticator.Domain
    public void validateAccount (
            WorldAuthenticator.Account account, String machIdent, boolean newIdent)
        throws ServiceException
    {
        OOOAccount oooacc = (OOOAccount)account;
        OOOUser user = oooacc.user;

        // if they gave us an invalid machIdent, ban them
        if (!newIdent && !StringUtil.isBlank(machIdent) &&
                !WorldAuthenticator.isValidIdent(machIdent)) {
            if (_authrep.ban(OOOUser.METASOY_SITE_ID, user.username)) {
                _supportLogic.reportAutoBan(user, "AUTO-BAN: supplied invalid machIdent");
            }

        // otherwire add the ident if necessary
        } else {
            String[] machIdents = _authrep.loadMachineIdents(user.userId);

            // if we have never seen them before...
            if (machIdents == null) {
                _authrep.addUserIdent(user.userId, machIdent);

            } else if (Arrays.binarySearch(machIdents, machIdent) < 0) {
                // and slap it in the db
                _authrep.addUserIdent(user.userId, machIdent);
            }
        }

        // if this is a banned user, mark that ident
        if (user.holdsToken(OOOUser.MSOY_BANNED) ||
                _authrep.isBannedIdent(machIdent, OOOUser.METASOY_SITE_ID)) {
            _authrep.addTaintedIdent(machIdent);
            throw new ServiceException(MsoyAuthCodes.BANNED);
        }

        // don't let those bastards grief us.
        if (account.firstLogon && (_authrep.isTaintedIdent(machIdent)) ) {
            throw new ServiceException(MsoyAuthCodes.MACHINE_TAINTED);
        }

        // if the user has bounced a check or reversed payment, let them know
        if (user.holdsToken(OOOUser.MSOY_DEADBEAT)) {
            throw new ServiceException(MsoyAuthCodes.DEADBEAT);
        }

        // you're all clear kid...
    }

    // from interface WorldAuthenticator.Domain
    public void validateAccount (WorldAuthenticator.Account account)
        throws ServiceException
    {
        OOOAccount oooacc = (OOOAccount)account;
        if (oooacc.user.holdsToken(OOOUser.MSOY_BANNED)) {
            throw new ServiceException(MsoyAuthCodes.BANNED);
        }
        // TODO: do we care about other badness like DEADBEAT?
    }

    // from interface WorldAuthenticator.Domain
    public String generatePasswordResetCode (String accountName)
        throws ServiceException
    {
        OOOUser user = _authrep.loadUserByEmail(accountName, false);
        return (user == null) ? null : StringUtil.md5hex(user.username + user.password);
    }

    // from interface WorldAuthenticator.Domain
    public boolean validatePasswordResetCode (String accountName, String code)
        throws ServiceException
    {
        return code.equals(generatePasswordResetCode(accountName));
    }

    // from interface WorldAuthenticator.Domain
    public boolean isUniqueIdent (String machIdent)
    {
        return _authrep.getMachineIdentCount(machIdent) == 0;
    }

    protected static class OOOAccount extends WorldAuthenticator.Account
    {
        public OOOUser user;
    }

    protected static class MsoyUsername extends Username
    {
        public MsoyUsername (String username) throws InvalidUsernameException {
            super(username);
        }
        @Override
        protected void validateName (String username) throws InvalidUsernameException {
            if (!MailUtil.isValidAddress(username)) {
                throw new InvalidUsernameException("");
            }
        }
    }

    @Inject protected MsoyOOOUserRepository _authrep;
    @Inject protected SupportLogic _supportLogic;
}
