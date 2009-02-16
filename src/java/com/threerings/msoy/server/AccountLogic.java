//
// $Id$

package com.threerings.msoy.server;

import java.security.MessageDigest;
import java.util.Calendar;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DuplicateKeyException;

import com.samskivert.net.MailUtil;

import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.CoinAwards;
import com.threerings.msoy.data.MsoyAuthCodes;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.money.server.MoneyLogic;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.room.data.MsoySceneModel;

import com.threerings.msoy.room.server.persist.MsoySceneRepository;

import com.threerings.msoy.server.AuthenticationDomain.Account;

import com.threerings.msoy.server.persist.AffiliateMapRepository;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;

import static com.threerings.msoy.Log.log;

@BlockingThread @Singleton
public class AccountLogic
{
    /** Password used when creating a permaguest account. */
    public static final String PERMAGUEST_PASSWORD = "";

    /**
     * Returns the authentication domain to use for the supplied account name. We support
     * federation of authentication domains based on the domain of the address. For example, we
     * could route all @yahoo.com addresses to a custom authenticator that talked to Yahoo!  to
     * authenticate user accounts.
     */
    public AuthenticationDomain getDomain (final String accountName)
    {
        // TODO: fancy things based on the user's email domain for our various exciting partners
        return _defaultDomain;
    }

    /**
     * Creates a new account from user-input web form data. The caller is expected to take care of
     * guest coin transfer and invitation linking and notification.
     * @return the new MemberRecord for the account
     * @throws ServiceException if any provided information is invalid or a runtime error occurs
     */
    public MemberRecord createWebAccount (String email, String password,
        String displayName, String realName, InvitationRecord invite, VisitorInfo vinfo,
        String cookie, int[] birthdayYMD)
        throws ServiceException
    {
        return createAccount(email, password, displayName, realName, invite, vinfo, cookie,
            birthdayYMD, null, null);
    }

    /**
     * Registers a previously created permaguest account. 
     */
    public MemberRecord savePermaguestAccount (int memberId, String email, String password,
        String displayName, String realName, InvitationRecord invite, VisitorInfo vinfo,
        String cookie, int[] birthdayYMD)
        throws ServiceException
    {
        // check basic validity and age limit
        displayName = displayName.trim();
        validateRegistrationInfo(birthdayYMD, email, displayName);

        // load the member
        MemberRecord mrec = _memberRepo.loadMember(memberId);
        if (mrec == null) {
            throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
        }

        // sanity check permaguest-ness
        if (!MemberName.isPermaguest(mrec.accountName)) {
            log.warning("Account not a permaguest", "email", mrec.accountName);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        // visitor id can't change, can it?
        String visitorId = checkVisitorInfo(mrec.accountName, vinfo);
        if (visitorId != null && !visitorId.equals(mrec.visitorId)) {
            log.warning("Permaguest visitor id changed", "original", mrec.visitorId,
                "new", visitorId, "memberId", mrec.memberId);
        }

        // get the affiliate
        int affiliate = resolveAffiliate(email, cookie, invite);

        // update member data
        try {
            _memberRepo.updateRegistration(memberId, email, displayName, affiliate);

        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        }

        try {
            // update the account name and password with the domain
            updateAccount(mrec.accountName, email, null, password);

        } catch (ServiceException se) {
            // we need to roll back the account name change to preserve a proper mapping between
            // MemberRecord and the authenticator's record
            try {
                _memberRepo.configureAccountName(mrec.memberId, mrec.accountName);

            } catch (Exception e) {
                log.warning("Failed to roll back account name change", "who", mrec.who(),
                            "newEmail", email, "oldEmail", mrec.accountName, e);
            }
            throw se;
        }

        // TODO: event logging, this is really the account creation

        // fill in fields so we are returning up-to-date information
        mrec.accountName = email;
        mrec.name = displayName;
        mrec.affiliateMemberId = affiliate;

        // update profile
        ProfileRecord prec = _profileRepo.loadProfile(memberId);
        prec.birthday = ProfileRecord.fromDateVec(birthdayYMD);
        prec.realName = realName;
        try {
            _profileRepo.storeProfile(prec);

        } catch (Exception e) {
            log.warning("Failed to create profile", "prec", prec, e);
            // move along
        }

        return mrec;
    }

    /**
     * Creates a new account for a guest user. All profile data will be default. The credentials
     * will be a placeholder email address and a default password.
     * @return the new guest account record
     * @throws ServiceException if a runtime error occurs
     */
    public MemberRecord createGuestAccount (String inetAddress, String visitorId)
        throws ServiceException
    {
        String name = _serverMsgs.getBundle("server").get("m.permaguest_name");
        MemberRecord guest =  createAccount(createPermaguestAccountName(inetAddress),
            PERMAGUEST_PASSWORD, name, null, null, new VisitorInfo(visitorId, false), null, null,
            null, null);
        guest.name = generatePermaguestDisplayName(guest.memberId);
        _memberRepo.configureDisplayName(guest.memberId, guest.name);
        log.info("Created permaguest account", "username", guest.accountName,
                 "memberId", guest.memberId);
        return guest;
    }

    /**
     * Creates a new account linked to an external account.
     * @return the newly created member record
     * @throws ServiceException if a runtime error occurs
     */
    public MemberRecord createExternalAccount (String email, String displayName,
        VisitorInfo vinfo, ExternalAuther exAuther, String exAuthUserId)
        throws ServiceException
    {
        return createAccount(email, "", displayName, null, null, vinfo, null,
            null, exAuther, exAuthUserId);
    }

    /**
     * Updates any of the supplied authentication information for the supplied account. Any of the
     * new values may be null to indicate that they are not to be updated.
     */
    public void updateAccount (
        String oldEmail, String newEmail, String newPermaName, String newPass)
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
     * Creates an account and profile record, with some validation.
     */
    protected MemberRecord createAccount (String email, String password,
        String displayName, String realName, InvitationRecord invite, VisitorInfo vinfo,
        String cookie, int[] birthdayYMD, ExternalAuther exAuther, String exAuthUserId)
        throws ServiceException
    {
        displayName = displayName.trim();

        validateRegistrationInfo(birthdayYMD, email, displayName);

        // attempt to create the account
        final MemberRecord mrec = createAccount(
            email, password, displayName, invite, vinfo, cookie, exAuther, exAuthUserId);

        // store the user's birthday and realname in their profile
        ProfileRecord prec = new ProfileRecord();
        prec.memberId = mrec.memberId;
        prec.birthday = birthdayYMD != null ? ProfileRecord.fromDateVec(birthdayYMD) : null;
        prec.realName = realName != null ? realName : "";
        try {
            _profileRepo.storeProfile(prec);
        } catch (Exception e) {
            log.warning("Failed to create initial profile [prec=" + prec + "]", e);
            // keep on keepin' on
        }

        return mrec;
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

        AuthenticationDomain domain = null;
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

            // set the required fields for registration
            mrec.accountName = account.accountName;
            mrec.name = displayName;
            mrec.affiliateMemberId = resolveAffiliate(account.accountName, affiliate, invite);
            mrec.visitorId = checkVisitorInfo(account.accountName, visitor);

            // store their member record in the repository making them a real Whirled citizen
            _memberRepo.insertMember(mrec);

            // if we're coming from an external authentication source, note that
            if (exAuther != null) {
                _memberRepo.mapExternalAccount(exAuther, externalId, mrec.memberId);
            }

            // create a blank room for them, store it
            final String name = _serverMsgs.getBundle("server").get("m.new_room_name", mrec.name);
            mrec.homeSceneId = _sceneRepo.createBlankRoom(
                MsoySceneModel.OWNER_TYPE_MEMBER, mrec.memberId, name, null, true).sceneId;
            _memberRepo.setHomeSceneId(mrec.memberId, mrec.homeSceneId);

            // create their money account, granting them some starting flow
            _moneyLogic.createMoneyAccount(mrec.memberId, CoinAwards.CREATED_ACCOUNT);

            // store their affiliate, if any (may also be the inviter's memberId)
            if (affiliate != null) {
                _memberRepo.setAffiliate(mrec.memberId, affiliate);
            }

            // record to the event log that we created a new account
            // TODO: correct/remove for permaguests
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
     * Generates a generic display name for permaguests.
     */
    protected String generatePermaguestDisplayName (int memberId)
    {
        String prefix = _serverMsgs.getBundle("server").get("m.permaguest_name");
        return prefix + " " + memberId;
    }

    protected void validateRegistrationInfo (int[] birthdayYMD, String email, String displayName)
        throws ServiceException
    {
        // check age restriction
        if (birthdayYMD != null) {
            Calendar thirteenYearsAgo = Calendar.getInstance();
            thirteenYearsAgo.add(Calendar.YEAR, -13);
            java.sql.Date bday = ProfileRecord.fromDateVec(birthdayYMD);
            if (bday.compareTo(thirteenYearsAgo.getTime()) > 0) {
                log.warning("User submitted invalid birtdate", "date", bday);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }
        }

        // make sure the email is valid and not too long (this is also validated on the client)
        if (!MailUtil.isValidAddress(email) ||
            email.length() > MemberName.MAX_EMAIL_LENGTH) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        }

        // validate display name length (this is enforced on the client)
        if (!MemberName.isValidDisplayName(displayName) ||
            !MemberName.isValidNonSupportName(displayName)) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected int resolveAffiliate (String account, String affiliate, InvitationRecord invite)
    {
        if (invite != null) {
            String inviterStr = String.valueOf(invite.inviterId);
            if (affiliate != null && !affiliate.equals(inviterStr)) {
                log.warning("New member has both an inviter and an affiliate. Using inviter.",
                    "email", account, "inviter", inviterStr, "affiliate", affiliate);
            }
            affiliate = inviterStr; // turn the inviter into an affiliate
        }
        if (affiliate != null) {
            // look up their affiliate's memberId, if any
            return _affMapRepo.getAffiliateMemberId(affiliate);
        }
        return 0;
    }

    protected String checkVisitorInfo (String account, VisitorInfo visitor)
    {
        if (visitor != null) {
            return visitor.id;
        } else {
            log.warning("Missing visitor id when creating user", "email", account);
            return null;
        }        
    }

    /**
     * Creates a username to give permanence to unregistered users.
     */
    protected static String createPermaguestAccountName (String ipAddress)
    {
        // generate some unique stuff
        String hashSource = "" + System.currentTimeMillis() + ":" + ipAddress + ":" + Math.random();

        // hash it
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("MD5").digest(hashSource.getBytes());

        } catch (java.security.NoSuchAlgorithmException nsae) {
            throw new RuntimeException("MD5 not found!?");
        }

        if (digest.length != 16) {
            throw new RuntimeException("Odd MD5 digest: " + StringUtil.hexlate(digest));
        }

        // convert to an email address
        return MemberName.PERMAGUEST_EMAIL_PREFIX + StringUtil.hexlate(digest) +
            MemberName.PERMAGUEST_EMAIL_SUFFIX;
    }

    @Inject protected AuthenticationDomain _defaultDomain;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected AffiliateMapRepository _affMapRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected ServerMessages _serverMsgs;

    /** Prefix of permaguest display names. They have to create an account to get a real one. */ 
    protected static final String PERMAGUEST_DISPLAY_PREFIX = "Guest";
}
