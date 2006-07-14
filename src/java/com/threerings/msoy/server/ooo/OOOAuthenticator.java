//
// $Id$

package com.threerings.msoy.server.ooo;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.util.IdentUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserManager;
import com.threerings.user.OOOUserRepository;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;

import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.msoy.client.LogonException;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.MsoyClientResolver;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.Member;

import static com.threerings.msoy.Log.log;
import static com.threerings.msoy.data.MsoyAuthCodes.*;

/**
 * Delegates authentication to the OOO user manager.
 */
public class OOOAuthenticator extends MsoyAuthenticator
{
    public OOOAuthenticator ()
    {
        try {
            // we get our user manager configuration from the ocean config
            _usermgr = new OOOUserManager(
                ServerConfig.config.getSubProperties("oooauth"),
                MsoyServer.conProv);
            _authrep = (OOOUserRepository)_usermgr.getRepository();
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to initialize OOO authenticator. " +
                    "Users will be unable to log in.", pe);
        }
    }

    // from Authenticator
    public void authenticateConnection (final AuthingConnection conn)
    {
        // fire up an invoker unit that will load the user object just to
        // make sure they exist
        String name = "auth:" + conn.getAuthRequest().getCredentials();
        MsoyServer.invoker.postUnit(new Invoker.Unit(name) {
            public boolean invoke () {
                processAuthentication(conn);
                return false;
            }
        });
    }

    // from MsoyAuthenticator
    public String authenticateSession (String username, String password,
                                       boolean persist)
        throws LogonException
    {
        try {
            // load up the user record and verify various bits
            OOOUser user = _authrep.loadUser(username, false);
            if (user == null) {
                throw new LogonException(NO_SUCH_USER);
            }
            if (!user.password.equals(password)) {
                throw new LogonException(INVALID_PASSWORD);
            }
            if (user.isBanned()) {
                throw new LogonException(BANNED);
            }

            // if they made it through that gauntlet, create or update their
            // session token and let 'em on in
            return _authrep.registerSession(user, persist);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Error authenticating user " +
                    "[who=" + username + "].", pe);
            throw new LogonException(SERVER_ERROR);
        }
    }

    /**
     * Here we do the actual authentication processing while running
     * happily on the invoker thread.
     */
    protected void processAuthentication (AuthingConnection conn)
    {
        AuthRequest req = conn.getAuthRequest();
        MsoyAuthResponseData rdata = new MsoyAuthResponseData();
        AuthResponse rsp = new AuthResponse(rdata);

        try {
            // make sure we were properly initialized
            if (_authrep == null) {
                rdata.code = SERVER_ERROR;
                return;
            }

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
//                     rdata.code = NEWER_VERSION;
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
            if (!(req.getCredentials() instanceof MsoyCredentials)) {
                log.warning("Invalid creds " + req.getCredentials() + ".");
                rdata.code = SERVER_ERROR;
                return;
            }

            // TODO: if they provide no client identifier, determine whether
            // one has been assigned to the account in question and provide
            // that to them if so, otherwise assign them a new one

            // check their provided machine identifier
            MsoyCredentials creds = (MsoyCredentials)req.getCredentials();
            String username = creds.getUsername().toString();
            if (StringUtil.isBlank(creds.ident)) {
                log.warning("Received blank ident [creds=" +
                            req.getCredentials() + "].");
                MsoyServer.generalLog(
                    "refusing_spoofed_ident " + username +
                    " ip:" + conn.getInetAddress());
                rdata.code = SERVER_ERROR;
                return;
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
                rdata.code = SERVER_ERROR;
                return;
            }

            // load up their user account record
            OOOUser user = _authrep.loadUser(username, true);
            if (user == null) {
                rdata.code = NO_SUCH_USER;
                return;
            }

            // we need to find out if this account has ever logged in so that
            // we can decide how to handle tainted idents; so we load up the
            // user record for this account; if this user makes it through the
            // gauntlet, we'll stash this away in a place that the client
            // resolver can get its hands on it so that we can avoid loading
            // the record twice during authentication
            Member mrec = MsoyServer.memberRepo.loadMember(username);

            // check to see whether this account has been banned or if this is
            // a first time user logging in from a tainted machine
            int vc = _authrep.validateUser(user, creds.ident, mrec == null);
            switch (vc) {
                // various error conditions
                case OOOUserRepository.ACCOUNT_BANNED:
                   rdata.code = BANNED;
                   return;
                case OOOUserRepository.NEW_ACCOUNT_TAINTED:
                   rdata.code = MACHINE_TAINTED;
                   return;
            }

//             // check whether we're restricting non-insider login
//             if (!RuntimeConfig.server.openToPublic &&
//                 !user.holdsToken(OOOUser.INSIDER) &&
//                 !user.holdsToken(OOOUser.TESTER) &&
//                 !user.isSupportPlus()) {
//                 rdata.code = NON_PUBLIC_SERVER;
//                 return;
//             }

//             // check whether we're restricting non-admin login
//             if (!RuntimeConfig.server.nonAdminsAllowed &&
//                 !user.isSupportPlus()) {
//                 rdata.code = UNDER_MAINTENANCE;
//                 return;
//             }

            // now check their password
            if (!user.password.equals(creds.getPassword())) {
                rdata.code = INVALID_PASSWORD;
                return;
            }

            // configure a token ring for this user
            int tokens = 0;
            if (user.holdsToken(OOOUser.ADMIN) ||
                user.holdsToken(OOOUser.MAINTAINER)) {
                tokens |= MsoyTokenRing.ADMIN;
                tokens |= MsoyTokenRing.SUPPORT;
            }
            if (user.holdsToken(OOOUser.SUPPORT)) {
                tokens |= MsoyTokenRing.SUPPORT;
            }
            rsp.authdata = new MsoyTokenRing(tokens);

            // replace the username in their credentials with the
            // canonical name in their user record as that username will
            // later be stuffed into their user object
            creds.setUsername(new Name(user.username));

            // log.info("User logged on [user=" + user.username + "].");
            rdata.code = MsoyAuthResponseData.SUCCESS;

//             // pass their user record to the client resolver for retrieval
//             // later in the logging on process
//             if (mrec != null) {
//                 MsoyClientResolver.stashMember(mrec);
//             }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Error authenticating user " +
                    "[areq=" + req + "].", pe);
            rdata.code = SERVER_ERROR;

        } finally {
            // let the powers that be know that we're done authenticating
            connectionWasAuthenticated(conn, rsp);
        }
    }

    protected OOOUserRepository _authrep;
    protected OOOUserManager _usermgr;
}
