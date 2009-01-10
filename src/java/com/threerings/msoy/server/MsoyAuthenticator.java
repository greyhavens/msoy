//
// $Id$

package com.threerings.msoy.server;

import static com.threerings.msoy.Log.log;

import java.util.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.msoy.data.CoinAwards;
import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.WorldCredentials;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.persist.AffiliateMapRepository;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MemberWarningRecord;

import com.threerings.msoy.web.gwt.BannedException;
import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.ExternalCreds;
// import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;

/**
 * Handles authentication for the MetaSOY server. We rely on underlying authentication domain
 * implementations to actually validate a username and password. From there, we maintain all
 * relevant information within MetaSOY, indexed initially on the domain-specific account name (an
 * email address).
 */
@Singleton
public class MsoyAuthenticator extends Authenticator
{
    /** Used to coordinate with authentication domains. */
    public static class Account
    {
        /** The account name in question. */
        public String accountName;

        /** The access privileges conferred to this account. */
        public MsoyTokenRing tokens;

        /** Whether or not this account is logging on for the first time. */
        public boolean firstLogon;
    }

    /** Provides authentication information for a particular partner. */
    public static interface Domain
    {
        /** A string that can be passed to the Domain to bypass password checking. Pass this actual
         * instance. */
        public static final String PASSWORD_BYPASS = "pwBypass";

        /**
         * Creates a new account for this authentication domain.
         */
        public Account createAccount (String accountName, String password)
            throws ServiceException;

        /**
         * Uncreates an account that was created but needs to be deleted because of a later failure
         * in the account creation process.
         */
        public void uncreateAccount (String accountName);

        /**
         * Notifies the authentication domain that the supplied information was modified for the
         * specified account.
         *
         * @param newAccountName if non-null, a new email address for this account.
         * @param newPermaName if non-null, the permaname assigned to this account.
         * @param newPassword if non-null, the new password to be assigned to this account.
         */
        public void updateAccount (String accountName, String newAccountName, String newPermaName,
                                   String newPassword)
            throws ServiceException;

        /**
         * Loads up account information for the specified account and checks the supplied password.
         *
         * @exception ServiceException thrown with {@link MsoyAuthCodes#NO_SUCH_USER} if the account
         * does not exist or with {@link MsoyAuthCodes#INVALID_PASSWORD} if the provided password
         * is invalid.
         */
        public Account authenticateAccount (String accountName, String password)
            throws ServiceException;

        /**
         * Called with an account loaded from {@link #authenticateAccount} to check whether the
         * specified account is banned or if the supplied machine identifier should be prevented
         * from creating a new account.
         *
         * @param machIdent a unique identifier assigned to the machine from which this account is
         * logging in.
         * @param newIdent if the machIdent was generated on the server
         *
         * @exception ServiceException thrown with {@link MsoyAuthCodes#BANNED} if the account is
         * banned or {@link MsoyAuthCodes#MACHINE_TAINTED} if the machine identifier provided is
         * associated with a banned account and this is the account's first logon.
         */
        public void validateAccount (Account account, String machIdent, boolean newIdent)
            throws ServiceException;

        /**
         * Called with an account loaded from {@link #authenticateAccount} to check whether the
         * specified account is banned. This is used when the account logs in from a
         * non-machine-ident supporting client (like the web browser).
         *
         * @exception ServiceException thrown with {@link MsoyAuthCodes#BANNED} if the account is
         * banned.
         */
        public void validateAccount (Account account)
            throws ServiceException;

        /**
         * Generates a secret code that can be emailed to a user and then subsequently passed to
         * {@link #validatePasswordResetCode} to confirm that the user is in fact receiving email
         * sent to the address via which their account is registered.
         *
         * @return null if no account is registered for that address, a secret code otherwise.
         */
        public String generatePasswordResetCode (String accountName)
            throws ServiceException;

        /**
         * Validates that the supplied password reset code is the one earlier provided by a call to
         * {@link #generatePasswordResetCode}.
         *
         * @return true if the code is valid, false otherwise.
         */
        public boolean validatePasswordResetCode (String accountName, String code)
            throws ServiceException;

        /**
         * Validates that this is a unique machine identifier.
         */
        public boolean isUniqueIdent (String machIdent);
    }

    /**
     * Verifies that an ident is valid.
     */
    public static boolean isValidIdent (final String ident)
    {
        if (ident == null || ident.length() != 48) {
            return false;
        }
        return ident.substring(40, 48).equals(generateIdentChecksum(ident.substring(0, 40)));
    }

    /**
     * Generates a guest name from the given member id.
     */
    public static String generateGuestName (final int memberId)
    {
        return "Guest" + Math.abs(memberId);
    }

    /**
     * Returns the default domain used for the internal support tools.
     */
    public Domain getDefaultDomain ()
    {
        return _defaultDomain;
    }

    /**
     * Creates a new account with the supplied credentials.
     *
     * @param email the email address of the to-be-created account.
     * @param password the MD5 encrypted password for this account.
     * @param displayName the user's initial display name.
     * @param ignoreRestrict whether to ignore the registration enabled toggle.
     *
     * @return the newly created member record.
     */
    public MemberRecord createAccount (
        String email, final String password, final String displayName, boolean ignoreRestrict,
        final InvitationRecord invite, final VisitorInfo visitor, String affiliate)
        throws ServiceException
    {
        if (!_runtime.server.registrationEnabled && !ignoreRestrict) {
            throw new ServiceException(MsoyAuthCodes.NO_REGISTRATIONS);
        }
        return createAccount(email, password, displayName, invite, visitor, affiliate, null, null);
    }

    /**
     * Updates any of the supplied authentication information for the supplied account. Any of the
     * new values may be null to indicate that they are not to be updated.
     */
    public void updateAccount (String oldEmail, String newEmail, String newPermaName, String newPass)
        throws ServiceException
    {
        try {
            // make sure we're dealing with lower cased email addresses
            oldEmail = oldEmail.toLowerCase();
            if (newEmail != null) {
                newEmail = newEmail.toLowerCase();
            }
            getDomain(oldEmail).updateAccount(oldEmail, newEmail, newPermaName, newPass);
        } catch (final RuntimeException e) {
            log.warning("Error updating account", "for", oldEmail, "nemail", newEmail,
                        "nperma", newPermaName, "npass", newPass, e);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }
    }

    /**
     * Generates a secret code that can be emailed to a user and then subsequently passed to {@link
     * #validatePasswordResetCode} to confirm that the user is in fact receiving email sent to the
     * address via which their account is registered.
     *
     * @return null if no account is registered for that address, a secret code otherwise.
     */
    public String generatePasswordResetCode (String email)
        throws ServiceException
    {
        // make sure we're dealing with a lower cased email
        email = email.toLowerCase();
        return getDomain(email).generatePasswordResetCode(email);
    }

    /**
     * Validates that the supplied password reset code is the one earlier provided by a call to
     * {@link #generatePasswordResetCode}.
     *
     * @return true if the code is valid, false otherwise.
     */
    public boolean validatePasswordResetCode (String email, final String code)
        throws ServiceException
    {
        // make sure we're dealing with a lower cased email
        email = email.toLowerCase();
        return getDomain(email).validatePasswordResetCode(email, code);
    }

    /**
     * Authenticates a web sesssion, verifying the supplied email and password and loading,
     * creating (or reusing) a member record.
     *
     * @param email the email address identifying account to be authenticated.
     * @param password the MD5 encrypted password for this account.
     *
     * @return the user's member record.
     */
    public MemberRecord authenticateSession (String email, final String password)
        throws ServiceException
    {
        try {
            // make sure we're dealing with a lower cased email
            email = email.toLowerCase();

            // validate their account credentials; make sure they're not banned
            final Domain domain = getDomain(email);
            final Account account = domain.authenticateAccount(email, password);

            // load up their member information
            MemberRecord mrec = _memberRepo.loadMember(account.accountName);
            if (mrec == null) {
                log.warning("Missing member record for authenticated user", "email", email);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }
            account.firstLogon = (mrec.sessions == 0);

            // validate that they can logon from the domain
            domain.validateAccount(account);

            // validate that they can logon locally
            checkWarnAndBan(mrec.memberId);

            return mrec;

        } catch (final RuntimeException e) {
            log.warning("Error authenticating user [who=" + email + "].", e);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }
    }

    /**
     * Authenticates a web sesssion, verifying the supplied external credentials and loading,
     * creating (or reusing) a member record.
     *
     * @return the user's member record.
     */
    public MemberRecord authenticateSession (ExternalCreds creds, VisitorInfo visitor,
                                             String affiliate)
        throws ServiceException
    {
        try {
            ExternalAuthHandler handler = _extLogic.getHandler(creds.getAuthSource());
            if (handler == null) {
                log.warning("Asked to auth using unsupported external source", "creds", creds);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            // make sure the supplied external creds are valid
            handler.validateCredentials(creds);

            // see if we've already got member information for this user
            int memberId = _memberRepo.lookupExternalAccount(
                creds.getAuthSource(), creds.getUserId());
            if (memberId > 0) {
                MemberRecord mrec = _memberRepo.loadMember(memberId);
                if (mrec == null) {
                    log.warning("Missing member record for which we have an extermal mapping",
                                "creds", creds, "memberId", memberId);
                    throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
                }
                checkWarnAndBan(mrec.memberId);
                return mrec;
            }

            // otherwise we need to create their account for the first time which requires getting
            // information from the external authentication source
            ExternalAuthHandler.Info info = handler.getInfo(creds);

            // create their account
            MemberRecord mrec = createAccount(
                creds.getPlaceholderAddress(), "", info.displayName, null, visitor, null,
                creds.getAuthSource(), creds.getUserId());

            // store their profile
            try {
                info.profile.memberId = mrec.memberId;
                _profileRepo.storeProfile(info.profile);
            } catch (Exception e) {
                log.warning("Failed to store initial profile for autocreated external user",
                            "creds", creds, "info", info, e);
            }

            // wire them up to any friends they might have
            if (info.friendIds != null) {
                try {
                    _extLogic.wireUpExternalFriends(
                        mrec.memberId, creds.getAuthSource(), info.friendIds);
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
    protected void processAuthentication (final AuthingConnection conn, final AuthResponse rsp)
        throws Exception
    {
        final AuthRequest req = conn.getAuthRequest();
        final MsoyAuthResponseData rdata = (MsoyAuthResponseData) rsp.getData();
        WorldCredentials creds = null;

        try {
            // make sure they've got the correct version
            final String cvers = req.getVersion(), svers = DeploymentConfig.version;
            if (!svers.equals(cvers)) {
                log.info("Refusing wrong version [creds=" + req.getCredentials() +
                         ", cvers=" + cvers + ", svers=" + svers + "].");
                boolean haveNewer = (cvers != null) && (svers.compareTo(cvers) < 0);
                throw new ServiceException(
                    haveNewer ? MsoyAuthCodes.NEWER_VERSION :
                    MessageBundle.tcompose(MsoyAuthCodes.VERSION_MISMATCH, svers));
            }

            // make sure they've sent valid credentials
            try {
                creds = (WorldCredentials) req.getCredentials();
            } catch (final ClassCastException cce) {
                log.warning("Invalid creds " + req.getCredentials() + ".", cce);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }
            if (creds == null) {
                log.info("No credentials provided with auth request " + req + ".");
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            if (creds.sessionToken != null) {
                if (WorldCredentials.isGuestSessionToken(creds.sessionToken)) {
                    authenticateGuest(conn, creds, rdata,
                                      WorldCredentials.getGuestMemberId(creds.sessionToken));

                } else {
                    final MemberRecord member = _memberRepo.loadMemberForSession(creds.sessionToken);
                    if (member == null) {
                        throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
                    }
                    rsp.authdata = authenticateMember(
                        creds, rdata, member, member.accountName, Domain.PASSWORD_BYPASS);
                }

            } else if (creds.getUsername() != null) {
                final String aname = creds.getUsername().toString().toLowerCase();
                rsp.authdata = authenticateMember(creds, rdata, null, aname, creds.getPassword());

            } else {
                // if this is not just a "featured whirled" client; assign this guest a member id
                // for the duration of their session
                final int memberId = creds.featuredPlaceView ? 0 : _peerMan.getNextGuestId();
                authenticateGuest(conn, creds, rdata, memberId);
            }

        } catch (final ServiceException se) {
            rdata.code = se.getMessage();
            log.info("Rejecting authentication [creds=" + creds + ", code=" + rdata.code + "].");
        }
    }

    protected void authenticateGuest (final AuthingConnection conn, final WorldCredentials creds,
                                      final MsoyAuthResponseData rdata, final int memberId)
        throws ServiceException
    {
        if (!_runtime.server.nonAdminsAllowed) {
            throw new ServiceException(MsoyAuthCodes.SERVER_CLOSED);
        }

        // if they're a "featured whirled" client, create a unique name for them
        if (creds.featuredPlaceView) {
            String name = conn.getInetAddress().getHostAddress() + ":" + System.currentTimeMillis();
            creds.setUsername(new LurkerName(name));
        } else {
            // if they supplied a name with their credentials, use that, otherwise generate one
            final String name = (creds.getUsername() == null) ?
                generateGuestName(memberId) : creds.getUsername().toString();
            creds.setUsername(new MemberName(name, memberId));
        }
        rdata.sessionToken = WorldCredentials.makeGuestSessionToken(memberId);
        rdata.code = MsoyAuthResponseData.SUCCESS;
        _eventLog.userLoggedIn(memberId, creds.visitorId, false, System.currentTimeMillis());
    }

    protected Account authenticateMember (WorldCredentials creds, MsoyAuthResponseData rdata,
                                          MemberRecord member, String accountName, String password)
        throws ServiceException
    {
        // obtain the authentication domain appropriate to their account name
        final Domain domain = getDomain(accountName);

        boolean newIdent = false;
        // see if we need to generate a new ident
        for (int ii = 0; ii < MAX_TRIES && StringUtil.isBlank(creds.ident); ii++) {
            final String ident = generateIdent(accountName, ii);
            if (domain.isUniqueIdent(ident)) {
                creds.ident = ident;
                newIdent = true;
                rdata.ident = creds.ident;
            }
        }
        if (StringUtil.isBlank(creds.ident)) {
            log.warning("Unable to generate unique machIdent for user [accountName=" +
                    accountName + "].");
            creds.ident = "";
        }

        // load up and authenticate their domain account record
        final Account account = domain.authenticateAccount(accountName, password);

        // we need to find out if this account has ever logged in so that we can decide how to
        // handle tainted idents; so we load up the member record for this account
        if (member == null) {
            member = _memberRepo.loadMember(account.accountName);
            // if this is their first logon, create them a member record
            if (member == null) {
                log.warning("Missing member record for authenticated user",
                            "email", account.accountName);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            } else {
                account.firstLogon = (member.sessions == 0);
            }
            rdata.sessionToken = _memberRepo.startOrJoinSession(member.memberId, 1);
        }

        // check to see whether this account has been banned or if this is a first time user
        // logging in from a tainted machine
        domain.validateAccount(account, creds.ident, newIdent);

        // validate the account locally
        rdata.warning = checkWarnAndBan(member.memberId);

        // fill in our access control tokens
        account.tokens = member.toTokenRing();

        // check whether we're restricting non-admin login
        if (!_runtime.server.nonAdminsAllowed && !account.tokens.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.SERVER_CLOSED);
        }

        // rewrite this member's username to their canonical account name
        creds.setUsername(new Name(account.accountName));

        // log.info("User logged on [user=" + user.username + "].");
        rdata.code = MsoyAuthResponseData.SUCCESS;
        _eventLog.userLoggedIn(member.memberId, member.visitorId, account.firstLogon,
                               member.created.getTime());

        return account;
    }

    /**
     * Returns the authentication domain to use for the supplied account name. We support
     * federation of authentication domains based on the domain of the address. For example, we
     * could route all @yahoo.com addresses to a custom authenticator that talked to Yahoo!  to
     * authenticate user accounts.
     */
    protected Domain getDomain (final String accountName)
    {
        // TODO: fancy things based on the user's email domain for our various exciting partners
        return _defaultDomain;
    }

    /**
     * Does the complicated and failure complex account creation process. Whee!
     */
    protected MemberRecord createAccount (
        String email, String password, String displayName, InvitationRecord invite,
        VisitorInfo visitor, String affiliate, ExternalAuther exAuther, String externalId)
        throws ServiceException
    {
        // make sure we're dealing with a lower cased email
        email = email.toLowerCase();

        Domain domain = null;
        Account account = null;
        MemberRecord stalerec = null;
        try {
            // create and validate the new account
            domain = getDomain(email);
            account = domain.createAccount(email, password);
            account.firstLogon = true;
            domain.validateAccount(account);

//             // create a new member record for the account
//             MemberRecord mrec = createMember(
//                 account.accountName, displayName, invite, visitor, affiliate);

            // normalize blank affiliates to null
            if (StringUtil.isBlank(affiliate)) {
                affiliate = null;
            }

            // create their main member record
            final MemberRecord mrec = new MemberRecord();
            stalerec = mrec;
            mrec.accountName = account.accountName;
            mrec.name = displayName;
            if (invite != null) {
                String inviterStr = String.valueOf(invite.inviterId);
                if (affiliate != null && !affiliate.equals(inviterStr)) {
                    log.warning("New member has both an inviter and an affiliate. Using inviter.",
                                "email", mrec.accountName, "inviter", inviterStr,
                                "affiliate", affiliate);
                }
                affiliate = inviterStr; // turn the inviter into an affiliate
            }
            if (affiliate != null) {
                // look up their affiliate's memberId, if any
                mrec.affiliateMemberId = _affMapRepo.getAffiliateMemberId(affiliate);
            }
            if (visitor != null) {
                mrec.visitorId = visitor.id;
            } else {
                log.warning("Missing visitor id when creating user " + account.accountName);
            }

            // store their member record in the repository making them a real Whirled citizen
            _memberRepo.insertMember(mrec);

            // if we're coming from an external authentication source, note that
            if (exAuther != null) {
                _memberRepo.mapExternalAccount(exAuther, externalId, mrec.memberId);
            }

            // create a blank room for them, store it
            final String name = _serverMsgs.getBundle("server").get("m.new_room_name", mrec.name);
            mrec.homeSceneId = _sceneRepo.createBlankRoom(
                MsoySceneModel.OWNER_TYPE_MEMBER, mrec.memberId, name, null, true);
            _memberRepo.setHomeSceneId(mrec.memberId, mrec.homeSceneId);

            // create their money account, granting them some starting flow
            _moneyLogic.createMoneyAccount(mrec.memberId, CoinAwards.CREATED_ACCOUNT);

            // store their affiliate, if any (may also be the inviter's memberId)
            if (affiliate != null) {
                _memberRepo.setAffiliate(mrec.memberId, affiliate);
            }

            // record to the event log that we created a new account
            final String iid = (invite == null) ? null : invite.inviteId;
            final String vid = (visitor == null) ? null : visitor.id;
            _eventLog.accountCreated(mrec.memberId, iid, mrec.affiliateMemberId, vid);

            // clear out account and stalerec to let the finally block know that all went well and
            // we need not roll back the domain account and member record creation
            account = null;
            stalerec = null;

            return mrec;

        } catch (final RuntimeException e) {
            log.warning("Error creating member record", "for", email, e);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);

        } finally {
            if (account != null) {
                try {
                    domain.uncreateAccount(email);
                } catch (final RuntimeException e) {
                    log.warning("Failed to rollback account creation", "email", email, e);
                }
            }
            if (stalerec != null) {
                try {
                    _memberRepo.deleteMember(stalerec);
                } catch (RuntimeException e) {
                    log.warning("Failed to rollback MemberRecord creation", "mrec", stalerec, e);
                }
            }
        }
    }

    /**
     * Validates an account checking for possible temp bans or warning messages.
     *
     * @return any warning message configured for this member or null.
     * @exception ServiceException thrown if the member is currently banned.
     */
    protected String checkWarnAndBan (final int memberId)
        throws ServiceException
    {
        final MemberWarningRecord record = _memberRepo.loadMemberWarningRecord(memberId);
        if (record == null) {
            return null;
        }

        if (record.banExpires != null) {
            // figure out how many hours are left on the temp ban
            final Date now = new Date();
            if (now.before(record.banExpires)) {
                final int expires = (int)((record.banExpires.getTime() - now.getTime())/ONE_HOUR);
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
        switch (creds.getAuthSource()) {
        case FACEBOOK:
            // FacebookCreds fbcreds = (FacebookCreds)creds;
            // TODO: validate creds
            break;

        default:
            log.warning("Asked to auth using unsupported external source", "creds", creds);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }
    }

    /**
     * Generate a new unique ident for this flash client.
     */
    protected static String generateIdent (final String accountName, final int offset)
    {
        final String seed = StringUtil.sha1hex(
                Long.toHexString(System.currentTimeMillis() + offset*1000L) + accountName);
        return seed + generateIdentChecksum(seed);
    }

    /**
     * Generates a checksum for an ident.
     */
    protected static String generateIdentChecksum (final String seed)
    {
        return StringUtil.sha1hex(seed.substring(10, 20) + seed.substring(30, 40) +
            seed.substring(20, 30) + seed.substring(0, 10)).substring(0, 8);
    }

    // our dependencies
    @Inject protected Domain _defaultDomain;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected ExternalAuthLogic _extLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected AffiliateMapRepository _affMapRepo;
    @Inject protected MsoySceneRepository _sceneRepo;

    /** The number of times we'll try generate a unique ident before failing. */
    protected static final int MAX_TRIES = 100;

    /** The number of milliseconds in an hour. */
    protected static final long ONE_HOUR = 60 * 60 * 1000L;
}
