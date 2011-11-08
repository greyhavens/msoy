//
// $Id$

package com.threerings.msoy.server;

import java.util.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.TimeUtil;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.apps.server.persist.AppRepository;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.GwtAuthCodes;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyAuthName;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.WorldCredentials;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.server.AuthenticationDomain.Account;
import com.threerings.msoy.server.persist.ExternalMapRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MemberWarningRecord;
import com.threerings.msoy.web.gwt.BannedException;
import com.threerings.msoy.web.gwt.ExternalCreds;
import com.threerings.msoy.web.server.AffiliateCookie;

import static com.threerings.msoy.Log.log;

/**
 * Handles authentication for the MetaSOY server. We rely on underlying authentication domain
 * implementations to actually validate a username and password. From there, we maintain all
 * relevant information within MetaSOY, indexed initially on the domain-specific account name (an
 * email address).
 */
@Singleton
public class MsoyAuthenticator extends Authenticator
{
    /**
     * Authenticates a web sesssion, verifying the supplied email and password and loading,
     * creating (or reusing) a member record.
     *
     * @param email the email address identifying account to be authenticated.
     * @param password the MD5 encrypted password for this account.
     *
     * @return the user's member record.
     */
    public MemberRecord authenticateSession (String email, String password)
        throws ServiceException
    {
        try {
            // make sure we're dealing with a lower cased email
            email = email.toLowerCase();

            // validate their account credentials; make sure they're not banned
            AuthenticationDomain domain = _accountLogic.getDomain(email);
            Account account = domain.authenticateAccount(email, password);

            // load up their member information
            MemberRecord mrec = _memberRepo.loadMember(account.accountName);
            if (mrec == null) {
                log.warning("Missing member record for authenticated user", "email", email);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }
            account.firstLogon = (mrec.sessions == 0);

            // validate that they can logon from the domain
            try {
                domain.validateAccount(account);
            } catch (ServiceException se) {
                checkBan(se, mrec.memberId);
            }

            // validate that they can logon locally
            checkWarnAndBan(mrec.memberId);

            return mrec;

        } catch (RuntimeException e) {
            log.warning("Error authenticating user", "who", email, e);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }
    }

    /**
     * Authenticates a web session, verifying the supplied external credentials and loading,
     * creating (or reusing) a member record.
     *
     * @return the user's member record.
     */
    public MemberRecord authenticateSession (
        ExternalCreds creds, VisitorInfo vinfo, AffiliateCookie affiliate, int appId)
        throws ServiceException
    {
        try {
            ExternalAuthHandler handler = _extLogic.getHandler(creds.getSite());
            if (handler == null) {
                log.warning("Asked to auth using unsupported external source", "creds", creds);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            // make sure the supplied external creds are valid
            handler.validateCredentials(creds);

            // see if we've already got member information for this user from any related site
            ExternalMapRecord mapping = _memberRepo.lookupAnyExternalAccount(
                creds.getSite(), creds.getUserId());
            if (mapping != null) {
                MemberRecord mrec = _memberRepo.loadMember(mapping.memberId);
                if (mrec == null) {
                    log.warning("Missing member record for which we have an extermal mapping",
                                "creds", creds, "memberId", mapping.memberId);
                    throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
                }
                checkWarnAndBan(mrec.memberId);
                if (!mapping.getSiteId().equals(creds.getSite())) {
                    // insert a new mapping if they are entering from a new site
                    _memberRepo.mapExternalAccount(
                        creds.getSite(), creds.getUserId(), mrec.memberId);
                }
                // if they have an external session key, update that here
                if (creds.getSessionKey() != null) {
                    _memberRepo.updateExternalSessionKey(
                        creds.getSite(), mrec.memberId, creds.getSessionKey());
                }
                return mrec;
            }

            // otherwise we need to create their account for the first time which requires getting
            // information from the external authentication source
            ExternalAuthHandler.Info info = handler.getInfo(creds);

            int themeId = 0;
            if (appId > 0) {
                try {
                    themeId = _appRepo.loadAppInfo(appId).groupId;
                } catch (Exception e) {
                    log.warning("Failed to assign proper theme to new app user", "appId", appId, e);
                }
            }

            // create their account
            MemberRecord mrec = _accountLogic.createExternalAccount(
                creds.getPlaceholderAddress(), info.displayName, info.profile, vinfo, themeId,
                affiliate, creds.getSite(), creds.getUserId());

            // if they have an external session key, update that here
            if (creds.getSessionKey() != null) {
                _memberRepo.updateExternalSessionKey(
                    creds.getSite(), mrec.memberId, creds.getSessionKey());
            }

            // wire them up to any friends they might have
            if (info.friendIds != null) {
                try {
                    _extLogic.wireUpExternalFriends(
                        mrec.memberId, creds.getSite().auther, info.friendIds);
                } catch (Exception e) {
                    log.warning("Failed to connect autocreated external user to friends",
                                "creds", creds, "friendIds", info.friendIds, e);
                }
            }

            return mrec;

        } catch (RuntimeException re) {
            log.warning("Error authenticating user", "creds", creds, re);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }
    }

    @Override // from Authenticator
    protected AuthResponseData createResponseData ()
    {
        return new MsoyAuthResponseData();
    }

    @Override // from Authenticator
    protected void processAuthentication (AuthingConnection conn, AuthResponse rsp)
        throws Exception
    {
        AuthRequest req = conn.getAuthRequest();
        MsoyAuthResponseData rdata = (MsoyAuthResponseData) rsp.getData();
        WorldCredentials creds = null;

        try {
            // make sure they've got the correct version
            String cvers = req.getVersion(), svers = DeploymentConfig.version;
            if (!svers.equals(cvers)) {
                log.info("Refusing wrong version",
                    "creds", req.getCredentials(), "cvers", cvers, "svers", svers);
                boolean haveNewer = (cvers != null) && (svers.compareTo(cvers) < 0);
                throw new ServiceException(
                    haveNewer ? MsoyAuthCodes.NEWER_VERSION :
                    MessageBundle.tcompose(MsoyAuthCodes.VERSION_MISMATCH, svers));
            }

            // make sure they've sent valid credentials
            try {
                creds = (WorldCredentials) req.getCredentials();
            } catch (ClassCastException cce) {
                log.warning("Invalid creds", "creds", req.getCredentials(), cce);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }
            if (creds == null) {
                log.info("No credentials provided with auth request", "req", req);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            // finish processing our authentication (the helper method helps simplify the code)
            rsp.authdata = processAuthentication(conn, creds, rdata);

        } catch (ServiceException se) {
            if (se instanceof BannedException) {
                BannedException be = (BannedException)se;
                rdata.code = MessageBundle.compose(be.getMessage(),
                    MessageBundle.taint(be.getWarning()),
                    TimeUtil.getTimeOrderString(
                        be.getExpires() * 1000L, TimeUtil.SECOND, TimeUtil.DAY));

            } else {
                rdata.code = se.getMessage();
            }
            log.info("Rejecting authentication", "creds", creds, "code", rdata.code);
        }
    }

    protected Account processAuthentication (
        AuthingConnection conn, WorldCredentials creds, MsoyAuthResponseData rdata)
        throws ServiceException
    {
        if (creds.sessionToken != null) {
            final MemberRecord member = _memberRepo.loadMemberForSession(creds.sessionToken);
            if (member == null || member.isDeleted()) {
                throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
            }
            return authenticateMember(conn, creds, rdata, member, false, member.accountName,
                                      AuthenticationDomain.PASSWORD_BYPASS);
        }

        if (creds.getUsername() != null) {
            String aname = creds.getUsername().toString().toLowerCase();
            try {
                return authenticateMember(
                    conn, creds, rdata, null, true, aname, creds.getPassword());
            } catch (ServiceException se) {
                // it's possible that the permaguest account requested has been purged, so instead
                // of failing the logon, just fall through and create a new permaguest account
                if (!AuthLogic.fixPurgedPermaguest(se, creds)) {
                    throw se;
                }
            }
        }

        if (creds.featuredPlaceView) {
            // we're a "featured whirled" client so we'll be an ephemeral guest with id 0
            // TODO: throttle logins for featured place view?
            authenticateLurker(conn, creds, rdata, 0);
            return null;
        }

        // create a new guest account
        MemberRecord mrec = _accountLogic.createGuestAccount(
            conn.getInetAddress().toString(), creds.visitorId, creds.themeId,
            AffiliateCookie.fromCreds(creds.affiliateId));

        // now authenticate just to make sure everything is in order and get the token
        return authenticateMember(conn, creds, rdata, mrec, true, mrec.accountName,
            AccountLogic.PERMAGUEST_PASSWORD);
    }

    protected void authenticateLurker (AuthingConnection conn, WorldCredentials creds,
                                      MsoyAuthResponseData rdata, int memberId)
        throws ServiceException
    {
        if (!_runtime.server.nonAdminsAllowed) {
            throw new ServiceException(MsoyAuthCodes.SERVER_CLOSED);
        }

        // they're a "featured whirled" client, create a unique name for them
        String name = conn.getInetAddress().getHostAddress() + ":" + System.currentTimeMillis();
        conn.setAuthName(new LurkerName(name));
        rdata.code = MsoyAuthResponseData.SUCCESS;
        _eventLog.userLoggedIn(memberId, creds.visitorId, creds.vector, false,
            true, System.currentTimeMillis());
    }

    protected Account authenticateMember (AuthingConnection conn, WorldCredentials creds,
                                          MsoyAuthResponseData rdata, MemberRecord member,
                                          boolean needSessionToken,
                                          String accountName, String password)
        throws ServiceException
    {
        // obtain the authentication domain appropriate to their account name
        AuthenticationDomain domain = _accountLogic.getDomain(accountName);

        boolean newIdent = false;
        // see if we need to generate a new ident
        for (int ii = 0; ii < MAX_TRIES && StringUtil.isBlank(creds.ident); ii++) {
            String ident = AuthLogic.generateIdent(accountName, ii);
            if (domain.isUniqueIdent(ident)) {
                creds.ident = ident;
                newIdent = true;
                rdata.ident = creds.ident;
            }
        }
        if (StringUtil.isBlank(creds.ident)) {
            log.warning("Unable to generate unique machIdent for user", "accountName", accountName);
            creds.ident = "";
        }

        // load up and authenticate their domain account record
        final Account account = domain.authenticateAccount(accountName, password);

        // we need to find out if this account has ever logged in so that we can decide how to
        // handle tainted idents; so we load up the member record for this account
        if (member == null) {
            member = _memberRepo.loadMember(account.accountName);
            if (member == null) {
                log.warning("Missing member record for authenticated user",
                            "email", account.accountName);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            // if this is their first logon, make a note of it
            account.firstLogon = (member.sessions == 0);
        }

        if (needSessionToken) {
            rdata.sessionToken = _memberRepo.startOrJoinSession(member.memberId);
        }

        // check to see whether this account has been banned or if this is a first time user
        // logging in from a tainted machine
        try {
            domain.validateAccount(account, creds.ident, newIdent);
        } catch (ServiceException se) {
            checkBan(se, member.memberId);
        }

        // validate the account locally
        rdata.warning = checkWarnAndBan(member.memberId);

        // fill in our access control tokens
        account.tokens = member.toTokenRing();

        // check whether we're restricting non-admin login
        if (!_runtime.server.nonAdminsAllowed && !account.tokens.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.SERVER_CLOSED);
        }

        // log.info("User logged on [user=" + user.username + "].");
        conn.setAuthName(new MsoyAuthName(member.accountName, member.memberId));
        rdata.code = MsoyAuthResponseData.SUCCESS;
        _eventLog.userLoggedIn(member.memberId, member.visitorId, creds.vector, account.firstLogon,
                               member.isPermaguest(), member.created.getTime());

        return account;
    }

    /**
     * Transforms a  {@link GwtAuthCodes#BANNED} {@link com.threerings.web.gwt.ServiceException} into a
     * {@link BannedException} annotated with the warning supplied for the ban.
     */
    protected void checkBan (ServiceException se, int memberId)
        throws ServiceException
    {
        // peek at the guts of the exception
        if (!MsoyAuthCodes.BANNED.equals(se.getMessage())) {
            throw se;
        }
        // if it's BANNED, mutate it!
        MemberWarningRecord record = _memberRepo.loadMemberWarningRecord(memberId);
        throw new BannedException(MsoyAuthCodes.BANNED, (record != null) ? record.warning : null);
    }

    /**
     * Validates an account checking for possible temp bans or warning messages.
     *
     * @return any warning message configured for this member or null.
     * @exception ServiceException thrown if the member is currently banned.
     */
    protected String checkWarnAndBan (int memberId)
        throws ServiceException
    {
        MemberWarningRecord record = _memberRepo.loadMemberWarningRecord(memberId);
        if (record == null) {
            return null;
        }

        if (record.banExpires != null) {
            // figure out how many seconds are left on the temp ban
            Date now = new Date();
            if (now.before(record.banExpires)) {
                int expires = (int)((record.banExpires.getTime() - now.getTime())/1000L);
                throw new BannedException(MsoyAuthCodes.TEMP_BANNED, record.warning, expires);
            }
        }

        return record.warning;
    }

    /**
     * Validates the supplied external credentials, usually by computing some signature on the
     * credential information and comparing that to a supplied signature.
     *
     * @exception ServiceException thrown if the supplied credentials are not valid.
     */
    protected void validateExternalCreds (ExternalCreds creds)
        throws ServiceException
    {
        switch (creds.getSite().auther) {
        case FACEBOOK:
            // FacebookCreds fbcreds = (FacebookCreds)creds;
            // TODO: validate creds
            break;

        default:
            log.warning("Asked to auth using unsupported external source", "creds", creds);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }
    }

    // our dependencies
    @Inject protected AccountLogic _accountLogic;
    @Inject protected AppRepository _appRepo;
    @Inject protected AuthLogic _authLogic;
    @Inject protected ExternalAuthLogic _extLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected RuntimeConfig _runtime;

    /** The number of times we'll try generate a unique ident before failing. */
    protected static final int MAX_TRIES = 100;
}
