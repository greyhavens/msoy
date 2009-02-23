//
// $Id$

package com.threerings.msoy.server;

import java.util.Calendar;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.codec.binary.Base64;

import com.samskivert.depot.DuplicateKeyException;

import com.samskivert.net.MailUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;

import com.threerings.msoy.data.CoinAwards;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.server.AuthenticationDomain.Account;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

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
    public MemberRecord createWebAccount (
        String email, String password, String displayName, String realName, InvitationRecord invite,
        VisitorInfo vinfo, int affiliateId, int[] birthdayYMD)
        throws ServiceException
    {
        AccountData data = new AccountData(true, email, password, displayName, vinfo, affiliateId);
        data.realName = realName;
        data.invite = invite;
        data.birthdayYMD = birthdayYMD;
        return createAccount(data);
    }

    /**
     * Registers a previously created permaguest account.
     */
    public MemberRecord savePermaguestAccount (
        int memberId, String email, String password, String displayName, String realName,
        InvitationRecord invite, VisitorInfo vinfo, int[] birthdayYMD)
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
        if (!MemberMailUtil.isPermaguest(mrec.accountName)) {
            log.warning("Account not a permaguest", "email", mrec.accountName);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        // visitor id can't change, can it?
        String visitorId = checkCreateId(mrec.accountName, vinfo);
        if (visitorId != null && !visitorId.equals(mrec.visitorId)) {
            log.warning("Permaguest visitor id changed", "original", mrec.visitorId,
                "new", visitorId, "memberId", mrec.memberId);
        }

        // update member data
        try {
            _memberRepo.updateRegistration(memberId, email, displayName);
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

        // if they have an invitation record, this overrides any cookied affiliate (that we stored
        // when they created their original permaguest account); this code path only happens if you
        // register via GWT during the *same session* from which you clicked your invite email
        // link, so clearly that's who wins
        if (invite != null) {
            _memberRepo.updateAffiliateMemberId(memberId, invite.inviterId);
            mrec.affiliateMemberId = invite.inviterId;
        }

        // tell panopticon that we "created" an account
        final String inviteId = (invite == null) ? null : invite.inviteId;
        _eventLog.accountCreated(mrec.memberId, inviteId, mrec.affiliateMemberId, mrec.visitorId);

        // fill in fields so we are returning up-to-date information
        mrec.accountName = email;
        mrec.name = displayName;

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

        // award coins
        try {
            _moneyLogic.awardCoins(memberId, CoinAwards.CREATED_ACCOUNT, true,
                UserAction.createdAccount(memberId));
        } catch (Exception e) {
            log.warning("Failed to award coins for new account", "memberId", memberId, e);
        }

        return mrec;
    }

    /**
     * Creates a new account for a guest user. All profile data will be default. The credentials
     * will be a placeholder email address and a default password.
     *
     * @return the newly created member record.
     */
    public MemberRecord createGuestAccount (String ipAddress, String visitorId, int affiliateId)
        throws ServiceException
    {
        String email = MemberMailUtil.makePermaguestEmail(
            StringUtil.md5hex(System.currentTimeMillis() + ":" + ipAddress + ":" + Math.random()));
        String displayName = _serverMsgs.getBundle("server").get("m.permaguest_name");
        AccountData data = new AccountData(
            false, email, PERMAGUEST_PASSWORD, displayName,
            new VisitorInfo(checkCreateId(email, visitorId), false), affiliateId);
        MemberRecord guest =  createAccount(data);
        // now that we have their member id, we can update their display name with it
        guest.name = generatePermaguestDisplayName(guest.memberId);
        _memberRepo.configureDisplayName(guest.memberId, guest.name);
        log.info("Created permaguest account", "username", guest.accountName,
                 "memberId", guest.memberId);
        return guest;
    }

    /**
     * Creates a new account linked to an external account.
     *
     * @return the newly created member record.
     */
    public MemberRecord createExternalAccount (
        String email, String displayName, VisitorInfo vinfo, int affiliateId,
        ExternalAuther exAuther, String exAuthUserId)
        throws ServiceException
    {
        AccountData data = new AccountData(true, email, "", displayName, vinfo, affiliateId);
        data.exAuther = exAuther;
        data.exAuthUserId = exAuthUserId;
        return createAccount(data);
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
     * Computes an email validation code for the supplied account.
     */
    public String generateValidationCode (MemberRecord mrec)
    {
        String data = ServerConfig.sharedSecret + mrec.memberId + mrec.accountName;
        String code = new String(Base64.encodeBase64(StringUtil.md5(data)));
        return code.substring(0, code.length()-2); // strip off the trailing ==
    }

    /**
     * Creates an account and profile record, with some validation.
     */
    protected MemberRecord createAccount (AccountData data)
        throws ServiceException
    {
        validateRegistrationInfo(data.birthdayYMD, data.email, data.displayName);

        // attempt to create the member record (and other bits)
        final MemberRecord mrec = createMember(data);

        // store the user's birthday and realname in their profile
        ProfileRecord prec = new ProfileRecord();
        prec.memberId = mrec.memberId;
        prec.birthday = (data.birthdayYMD != null) ?
            ProfileRecord.fromDateVec(data.birthdayYMD) : null;
        prec.realName = (data.realName != null) ? data.realName : "";
        try {
            _profileRepo.storeProfile(prec);
        } catch (Exception e) {
            log.warning("Failed to create initial profile [prec=" + prec + "]", e);
            // keep on keepin' on
        }

        return mrec;
    }

    /**
     * Creates a new member record and its various associated records. Handles the various
     * complicated failure fallbackery. Don't call this, call {@link #createAccount}.
     */
    protected MemberRecord createMember (AccountData data)
        throws ServiceException
    {
        AuthenticationDomain domain = null;
        Account account = null;
        MemberRecord stalerec = null;
        try {
            // create and validate the new account
            domain = getDomain(data.email);
            account = domain.createAccount(data.email, data.password);
            account.firstLogon = true;
            domain.validateAccount(account);

            // create their main member record
            final MemberRecord mrec = new MemberRecord();
            stalerec = mrec;

            // set the required fields for registration
            mrec.accountName = account.accountName;
            mrec.name = data.displayName;
            // invite will only be non-null if we are registering (without first logging in via
            // Flash no less) during the same session that we started by clicking on our invite
            // email link; in that case we want the invite member id to be our affiliate regardless
            // of what other affiliate we might have lurking around in cookies
            mrec.affiliateMemberId = (data.invite == null) ?
                data.affiliateId : data.invite.inviterId;
            mrec.visitorId = checkCreateId(account.accountName, data.vinfo);

            // store their member record in the repository making them a real Whirled citizen
            _memberRepo.insertMember(mrec);

            // if we're coming from an external authentication source, note that
            if (data.exAuther != null) {
                _memberRepo.mapExternalAccount(data.exAuther, data.exAuthUserId, mrec.memberId);
            }

            // create a blank room for them, store it
            final String name = _serverMsgs.getBundle("server").get("m.new_room_name", mrec.name);
            mrec.homeSceneId = _sceneRepo.createBlankRoom(
                MsoySceneModel.OWNER_TYPE_MEMBER, mrec.memberId, name, null, true).sceneId;
            _memberRepo.setHomeSceneId(mrec.memberId, mrec.homeSceneId);

            // create their money account, granting them some starting flow
            _moneyLogic.createMoneyAccount(
                mrec.memberId, data.isRegistering ? CoinAwards.CREATED_ACCOUNT : 0);

            // record to the event log that we created a new account
            if (data.isRegistering) {
                final String iid = (data.invite == null) ? null : data.invite.inviteId;
                final String vid = (data.vinfo == null) ? null : data.vinfo.id;
                _eventLog.accountCreated(mrec.memberId, iid, mrec.affiliateMemberId, vid);
            }

            // clear out account and stalerec to let the finally block know that all went well and
            // we need not roll back the domain account and member record creation
            account = null;
            stalerec = null;

            return mrec;

        } catch (final RuntimeException e) {
            log.warning("Error creating member record", "for", data.email, e);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);

        } finally {
            if (account != null) {
                try {
                    domain.uncreateAccount(data.email);
                } catch (final RuntimeException e) {
                    log.warning("Failed to rollback account creation", "email", data.email, e);
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

    protected int resolveAffiliate (int affiliateId, InvitationRecord invite)
    {
        return (invite == null) ? affiliateId : invite.inviterId;
    }

    protected String checkCreateId (String account, VisitorInfo visitor)
    {
        return checkCreateId(account, visitor == null ? null : visitor.id);
    }

    protected String checkCreateId (String account, String visitorId)
    {
        if (visitorId != null) {
            return visitorId;
        } else {
            log.warning("Missing visitor id when creating user", "email", account);
            return new VisitorInfo().id; // create a new id for the account
        }
    }

    protected static class AccountData
    {
        // mandatory bits
        public final boolean isRegistering;
        public final String email;
        public final String password;
        public final String displayName;
        public final VisitorInfo vinfo;
        public final int affiliateId;

        // optional bits
        public String realName;
        public InvitationRecord invite;
        public int[] birthdayYMD;
        public ExternalAuther exAuther;
        public String exAuthUserId;

        public AccountData (boolean isRegistering, String email, String password,
                            String displayName, VisitorInfo vinfo, int affiliateId)
        {
            this.isRegistering = isRegistering;
            this.email = email.trim().toLowerCase();
            this.password = password.trim();
            this.displayName = displayName.trim();
            this.vinfo = vinfo;
            this.affiliateId = affiliateId;
        }
    }

    @Inject protected AuthenticationDomain _defaultDomain;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoySceneRepository _sceneRepo;

    /** Prefix of permaguest display names. They have to create an account to get a real one. */
    protected static final String PERMAGUEST_DISPLAY_PREFIX = "Guest";
}
