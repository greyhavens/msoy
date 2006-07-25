//
// $Id$

package com.threerings.msoy.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.threerings.util.IdentUtil;
import com.threerings.util.Name;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.crowd.data.TokenRing;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.server.persist.Member;
import com.threerings.msoy.web.client.LogonException;
import com.threerings.msoy.web.client.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Handles authentication for the MetaSOY server. We rely on underlying
 * authentication domain implementations to actually validate a username and
 * password. From there, we maintain all relevant information within MetaSOY,
 * indexed initially on the domain-specific account name (an email address).
 */
public class MsoyAuthenticator extends Authenticator
{
    /** Used to coordinate with authentication domains. */
    public static class Account
    {
        /** The account name in question. */
        public String accountName;

        /** The access privileges conferred to this account. */
        public TokenRing tokens;
    }

    /** Provides authentication information for a particular partner. */
    public static interface Domain
    {
        /**
         * Initializes this authentication domain and gives it a chance to
         * connect to its underlying authentication data source. If the domain
         * throws an exception during init it will not be used later, if it
         * does not it is assumed tht it is ready to process authentications.
         */
        public void init ()
            throws PersistenceException;

        /**
         * Loads up account information for the specified account and checks
         * the supplied password.
         *
         * @exception LogonException thrown with {@link
         * MsoyAuthCodes#NO_SUCH_USER} if the account does not exist or with
         * {@link MsoyAuthCodes#INVALID_PASSWORD} if the provided password is
         * invalid.
         */
        public Account authenticateAccount (String accountName, String password)
            throws LogonException, PersistenceException;

        /**
         * Called with an account loaded from {@link #authenticateAccount} to
         * check whether the specified account is banned or if the supplied
         * machine identifier should be prevented from creating a new account.
         *
         * @param machIdent a unique identifier assigned to the machine from
         * which this account is logging in.
         * @param firstLogon if true, this is the first time the specified
         * account has logged on to the interactive service.
         *
         * @exception LogonException thrown with {@link MsoyAuthCodes#BANNED}
         * if the account is banned or {@link MsoyAuthCodes#MACHINE_TAINTED} if
         * the machine identifier provided is associated with a banned account
         * and this is the account's first logon.
         */
        public void validateAccount (Account account, String machIdent,
                                     boolean firstLogon)
            throws LogonException, PersistenceException;

        /**
         * Called with an account loaded from {@link #authenticateAccount} to
         * check whether the specified account is banned. This is used when the
         * account logs in from a non-machine-ident supporting client (like the
         * web browser).
         *
         * @exception LogonException thrown with {@link MsoyAuthCodes#BANNED}
         * if the account is banned.
         */
        public void validateAccount (Account account)
            throws LogonException, PersistenceException;
    }

    @Override
    protected AuthResponseData createResponseData ()
    {
        return new MsoyAuthResponseData();
    }

    // from abstract Authenticator
    protected void processAuthentication (
            AuthingConnection conn, AuthResponse rsp)
        throws PersistenceException
    {
        AuthRequest req = conn.getAuthRequest();
        MsoyAuthResponseData rdata = (MsoyAuthResponseData) rsp.getData();

        try {
//             // make sure they've got the correct version
//             long cvers = 0L;
//             long svers = DeploymentConfig.getVersion();
//             try {
//                 cvers = Long.parseLong(req.getVersion());
//             } catch (Exception e) {
//                 // ignore it and fail below
//             }
//             if (svers != cvers) {
//                 if (cvers > svers) {
//                     throw new LogonException(NEWER_VERSION);
//                 } else {
//                     // TEMP: force the use of the old auth response data to
//                     // avoid freaking out older clients
//                     rsp = new AuthResponse(new AuthResponseData());
//                     rsp.getData().code = MessageBundle.tcompose(
//                         VERSION_MISMATCH, "" + svers);
//                 }
//                 log.info("Refusing wrong version " +
//                          "[creds=" + req.getCredentials() +
//                          ", cvers=" + cvers + ", svers=" + svers + "].");
//                 return;
//             }

            // make sure they've sent valid credentials
            MsoyCredentials creds;
            try {
                creds = (MsoyCredentials) req.getCredentials();

            } catch (ClassCastException cce) {
                log.warning("Invalid creds " + req.getCredentials() + ".");
                throw new LogonException(MsoyAuthCodes.SERVER_ERROR);
            }

            // TODO: if they provide no client identifier, determine whether
            // one has been assigned to the account in question and provide
            // that to them if so, otherwise assign them a new one

            // TODO: remove temporary guest-access code
            if (creds.ident == null) {
                rdata.code = MsoyAuthResponseData.SUCCESS;
                return;
            }
            // END: temp

            String username = creds.getUsername().toString();
            if (StringUtil.isBlank(creds.ident)) {
                log.warning("Received blank ident [creds=" +
                            req.getCredentials() + "].");
                MsoyServer.generalLog(
                    "refusing_spoofed_ident " + username +
                    " ip:" + conn.getInetAddress());
                throw new LogonException(MsoyAuthCodes.SERVER_ERROR);
            }

            // TODO: allow guest login

            // if they supplied a known non-unique machine identifier, create
            // one for them
            if (IdentUtil.isBogusIdent(creds.ident.substring(1))) {
                String sident = StringUtil.md5hex(
                    "" + Math.random() + System.currentTimeMillis());
                creds.ident = "S" + IdentUtil.encodeIdent(sident);
                MsoyServer.generalLog("creating_ident " + username +
                                      " ip:" + conn.getInetAddress() +
                                      " id:" + creds.ident);
                rdata.ident = creds.ident;
            }

            // convert the encrypted ident to the original MD5 hash
            try {
                String prefix = creds.ident.substring(0, 1);
                creds.ident = prefix +
                    IdentUtil.decodeIdent(creds.ident.substring(1));
            } catch (Exception e) {
                log.warning("Received spoofed ident [who=" + username +
                            ", err=" + e.getMessage() + "].");
                MsoyServer.generalLog("refusing_spoofed_ident " + username +
                                      " ip:" + conn.getInetAddress() +
                                      " id:" + creds.ident);
                throw new LogonException(MsoyAuthCodes.SERVER_ERROR);
            }

            // obtain the authentication domain appropriate to their account
            // name (which is their email address)
            Domain domain = getDomain(username);

            // load up and authenticate their domain account record
            Account account = domain.authenticateAccount(
                username, creds.getPassword());

            // we need to find out if this account has ever logged in so that
            // we can decide how to handle tainted idents; so we load up the
            // member record for this account; if this user makes it through
            // the gauntlet, we'll stash this away in a place that the client
            // resolver can get its hands on it so that we can avoid loading
            // the record twice during authentication
            Member mrec = MsoyServer.memberRepo.loadMember(account.accountName);

            // check to see whether this account has been banned or if this is
            // a first time user logging in from a tainted machine
            domain.validateAccount(account, creds.ident, mrec == null);

//             // check whether we're restricting non-insider login
//             if (!RuntimeConfig.server.openToPublic &&
//                 !user.holdsToken(OOOUser.INSIDER) &&
//                 !user.holdsToken(OOOUser.TESTER) &&
//                 !user.isSupportPlus()) {
//                 throw new LogonException(NON_PUBLIC_SERVER);
//             }

//             // check whether we're restricting non-admin login
//             if (!RuntimeConfig.server.nonAdminsAllowed &&
//                 !user.isSupportPlus()) {
//                 throw new LogonException(UNDER_MAINTENANCE);
//             }

//             rsp.authdata = new MsoyTokenRing(tokens);

            // replace the username in their credentials with the canonical
            // name in their user record as that username will later be stuffed
            // into their user object
            creds.setUsername(new Name(account.accountName));

            // log.info("User logged on [user=" + user.username + "].");
            rdata.code = MsoyAuthResponseData.SUCCESS;

//             // pass their user record to the client resolver for retrieval
//             // later in the logging on process
//             if (mrec != null) {
//                 MsoyClientResolver.stashMember(mrec);
//             }

        } catch (LogonException le) {
            rdata.code = le.getMessage();
        }
    }

    /**
     * Authenticates a web sesssion, verifying the supplied username and
     * password and creating (or reusing) a record in a session repository that
     * can be used to authenticate for the duration of that session.
     *
     * @param username the account to be authenticated.
     * @param password the MD5 encrypted password for this account.
     * @param persist if true the session should persist for some long but not
     * infinite duration (a month), if false the session should be scheduled to
     * expire in a day or two (the client will be instructed to expire the
     * session token when it next terminates).
     *
     * @return the session credentials that should be supplied to web-based
     * service requests for authentication.
     *
     * @exception LogonException thrown if the password is incorrect, the user
     * does not exist or some other problem occurs with logon.
     */
    public WebCreds authenticateSession (String username, String password,
                                         boolean persist)
        throws LogonException
    {
        try {
            // validate their account credentials; make sure they're not banned
            Domain domain = getDomain(username);
            Account account = domain.authenticateAccount(username, password);
            domain.validateAccount(account);

            // load up their member information to get their member id
            Member mrec = MsoyServer.memberRepo.loadMember(account.accountName);

            // if this is their first logon, insert a skeleton member record
            if (mrec == null) {
                mrec = new Member();
                mrec.accountName = account.accountName;
                MsoyServer.memberRepo.insertMember(mrec);
            }

            // if they made it through that gauntlet, create or update their
            // session token and let 'em on in
            WebCreds creds = new WebCreds();
            creds.memberId = mrec.memberId;
            creds.token = MsoyServer.memberRepo.startOrJoinSession(
                mrec.memberId, persist);
            return creds;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Error authenticating user " +
                    "[who=" + username + "].", pe);
            throw new LogonException(MsoyAuthCodes.SERVER_ERROR);
        }
    }

    /**
     * Returns the authentication domain to use for the supplied account name
     * (which is an email address). We support federation of authentication
     * domains based on the domain of the address. For example, we could route
     * all @yahoo.com addresses to a custom authenticator that talked to Yahoo!
     * to authenticate user accounts.
     */
    protected Domain getDomain (String accountName)
        throws PersistenceException
    {
        // if we have not yet created our default authentication domain, do so
        if (_defaultDomain == null) {
            Domain domain = new OOOAuthenticationDomain();
            domain.init();
            // don't assign the reference until we've successfully init()ed
            _defaultDomain = domain;
        }

        // TODO: fancy things based on the user's email domain for our various
        // exciting partners

        return _defaultDomain;
    }

    protected Domain _defaultDomain;
}
