//
// $Id$

package com.threerings.msoy.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.codec.binary.Base64;

import com.samskivert.net.MailUtil;
import com.samskivert.util.Calendars;
import com.samskivert.util.StringUtil;

import com.samskivert.depot.DuplicateKeyException;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.CoinAwards;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.server.persist.InvitationRecord;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.server.AuthenticationDomain.Account;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.web.gwt.ExternalSiteId;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.server.AffiliateCookie;

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
        VisitorInfo vinfo, int themeId, AffiliateCookie affiliate, int[] birthdayYMD)
        throws ServiceException
    {
        // TODO: handle affiliate.autoFriend
        AccountData data = new AccountData(
            true, email, password, displayName, vinfo, themeId, affiliate);
        data.realName = realName;
        data.invite = invite;
        data.birthdayYMD = birthdayYMD;
        MemberRecord mrec = createAccount(data);

        // Give the new player a few greeter friends to start out
        List<Integer> greeters = Lists.newArrayList(_memberMan.getPPSnapshot().getOnlineGreeters());
        Collections.shuffle(greeters);
        greeters = greeters.subList(0, Math.min(5, greeters.size()));
        for (int greeterId : greeters) {
            _memberLogic.establishFriendship(mrec.memberId, greeterId);
        }

        return mrec;
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

        // configure our new email address, display name and password (the first might fail if
        // we're using an in-use email, so we have to do that first)
        updateAccountName(mrec, email, false);
        _memberRepo.configureDisplayName(memberId, displayName);
        mrec.name = displayName;
        updatePassword(mrec, password);

        // if they have an invitation record, this overrides any cookied affiliate (that we stored
        // when they created their original permaguest account); this code path only happens if you
        // register via GWT during the *same session* from which you clicked your invite email
        // link, so clearly that's who wins
        if (invite != null) {
            _memberRepo.updateAffiliateMemberId(memberId, invite.inviterId);
            mrec.affiliateMemberId = invite.inviterId;
            if (mrec.isSet(MemberRecord.Flag.FRIEND_AFFILIATE)) {
                mrec.setFlag(MemberRecord.Flag.FRIEND_AFFILIATE, false);
                _memberRepo.storeFlags(mrec);
            }
        }

        // tell panopticon that we "created" an account
        final String inviteId = (invite == null) ? null : invite.inviteId;
        _eventLog.accountCreated(
            mrec.memberId, false, inviteId, mrec.affiliateMemberId, mrec.visitorId);

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
            _moneyLogic.awardCoins(
                memberId, CoinAwards.CREATED_ACCOUNT, true, UserAction.createdAccount(memberId));
        } catch (Exception e) {
            log.warning("Failed to award coins for new account", "memberId", memberId, e);
        }

        log.info("Saved permaguest account", "memberId", mrec.memberId);

        return mrec;
    }

    /**
     * Creates a new account for a guest user. All profile data will be default. The credentials
     * will be a placeholder email address and a default password.
     *
     * @return the newly created member record.
     */
    public MemberRecord createGuestAccount (
        String ipAddress, String visitorId, int themeId, AffiliateCookie affiliate)
        throws ServiceException
    {
        // TODO: handle affiliate.autoFriend
        String email = MemberMailUtil.makePermaguestEmail(
            StringUtil.md5hex(System.currentTimeMillis() + ":" + ipAddress + ":" + Math.random()));
        String displayName = _serverMsgs.getBundle("server").get("m.permaguest_name");
        AccountData data = new AccountData(false, email, PERMAGUEST_PASSWORD, displayName,
            new VisitorInfo(checkCreateId(email, visitorId), false), themeId, affiliate);
        MemberRecord guest =  createAccount(data);
        // now that we have their member id, we can update their display name with it
        guest.name = generatePermaguestDisplayName(guest.memberId);
        _memberRepo.configureDisplayName(guest.memberId, guest.name);
        log.info("Created permaguest account", "username", guest.accountName,
                 "memberId", guest.memberId, "visitorId", visitorId);
        return guest;
    }

    /**
     * Creates a new account linked to an external account.
     *
     * @return the newly created member record.
     */
    public MemberRecord createExternalAccount (
        String email, String displayName, ProfileRecord profile, VisitorInfo vinfo,
        int themeId, AffiliateCookie affiliate, ExternalSiteId exSite, String exAuthUserId)
        throws ServiceException
    {
        // TODO: handle affiliate.autoFriend
        AccountData data = new AccountData(true, email, "", displayName, vinfo, themeId, affiliate);
        data.exSite = exSite;
        data.exAuthUserId = exAuthUserId;
        // TODO: import more information as long as it is not a privacy violation
        if (profile != null) {
            if (profile.birthday != null) {
                data.birthdayYMD = ProfileRecord.toDateVec(profile.birthday);
            }
            data.realName = profile.realName;
            data.location = profile.location;
        }

        MemberRecord member = createAccount(data);
        // TODO(bruno): Do this for username/password logins too if we ever have them
        if (themeId != 0) {
            // Give them a transient home room
            _memberLogic.assignHomeRoom(member, true);
        }

        return member;
    }

    /**
     * Updates the authentication name (email address) for the supplied member.
     */
    public void updateAccountName (MemberRecord mrec, String newAccountName, boolean sendEmail)
        throws ServiceException
    {
        final String oldAccountName = mrec.accountName;

        // make sure we're dealing with lower cased email addresses
        newAccountName = newAccountName.toLowerCase();

        // make sure the email is valid and not too long (this is also validated on the client)
        if (!MailUtil.isValidAddress(newAccountName) ||
            newAccountName.length() > MemberName.MAX_EMAIL_LENGTH) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        }

        // first update their MemberRecord and fail if they request a duplicate name
        try {
            _memberRepo.configureAccountName(mrec.memberId, newAccountName);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        }

        try {
            // let the authenticator know that we updated our account name
            getDomain(oldAccountName).updateAccountName(oldAccountName, newAccountName);
        } catch (ServiceException se) {
            // if that fails, we need to roll back our repository as well
            log.warning("Fuck a duck. The ooouser db thinks we've already got this account.",
                        "old", oldAccountName, "new", newAccountName);
            _memberRepo.configureAccountName(mrec.memberId, oldAccountName);
            throw se;
        }

        // if we made it this far, mark the account as no longer validated
        mrec.setFlag(MemberRecord.Flag.VALIDATED, false);
        _memberRepo.storeFlags(mrec);

        // and send a new validation email
        mrec.accountName = newAccountName;

        if (sendEmail) {
            sendValidationEmail(mrec);
        }
    }

    public void updateAccountName (MemberRecord mrec, String newAccountName)
        throws ServiceException
    {
        updateAccountName(mrec, newAccountName, true);
    }

    /**
     * Configures the supplied member's permaname.
     */
    public void configurePermaName (MemberRecord mrec, String permaName)
        throws ServiceException
    {
        if (mrec.permaName != null) {
            log.warning("Rejecting attempt to reassign permaname", "who", mrec.accountName,
                        "oname", mrec.permaName, "nname", permaName);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (permaName == null ||
            permaName.length() < MemberName.MINIMUM_PERMANAME_LENGTH ||
            permaName.length() > MemberName.MAXIMUM_PERMANAME_LENGTH ||
            !permaName.matches(PERMANAME_REGEX)) {
            throw new ServiceException("e.invalid_permaname");
        }

        // first configure it in our repository to check for duplicates
        try {
            _memberRepo.configurePermaName(mrec.memberId, permaName);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_PERMANAME);
        }

        try {
            // then let the authenticator know that we updated our permaname
            getDomain(mrec.accountName).updatePermaName(mrec.accountName, permaName);
        } catch (ServiceException se) {
            // if that fails, we have to roll back our repository as well
            log.warning("Fuck a duck. The ooouser db thinks we've already got this permaname.",
                        "who", mrec.accountName, "pname", permaName);
            _memberRepo.configurePermaName(mrec.memberId, null);
            throw se;
        }
    }

    /**
     * Updates the supplied member's password.
     */
    public void updatePassword (MemberRecord mrec, String newPassword)
        throws ServiceException
    {
        // we just pass the buck onto our authentication domain
        getDomain(mrec.accountName).updatePassword(mrec.accountName, newPassword);
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
     * Sends a validation email to the supplied member.
     */
    public void sendValidationEmail (MemberRecord mrec)
    {
        _mailer.sendTemplateEmail(
            MailSender.By.HUMAN, mrec.accountName, ServerConfig.getFromAddress(), "revalidateEmail",
            "server_url", ServerConfig.getServerURL(), "email", mrec.accountName,
            "memberId", mrec.memberId, "code", generateValidationCode(mrec));
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
        prec.location = (data.location != null) ? data.location : "";
        try {
            _profileRepo.storeProfile(prec);
        } catch (Exception e) {
            log.warning("Failed to create initial profile", "prec", prec, e);
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

            // set the required fields for registration
            mrec.accountName = account.accountName;
            mrec.name = data.displayName;
            // invite will only be non-null if we are registering (without first logging in via
            // Flash no less) during the same session that we started by clicking on our invite
            // email link; in that case we want the invite member id to be our affiliate regardless
            // of what other affiliate we might have lurking around in cookies
            mrec.affiliateMemberId = (data.invite == null) ?
                data.affiliate.memberId : data.invite.inviterId;
            mrec.visitorId = checkCreateId(account.accountName, data.vinfo);
            mrec.themeGroupId = data.themeId;
            if (data.affiliate.memberId == mrec.affiliateMemberId && data.affiliate.autoFriend) {
                mrec.setFlag(MemberRecord.Flag.FRIEND_AFFILIATE, true);
            }

            // store their member record in the repository making them a real Whirled citizen
            _memberRepo.insertMember(mrec);
            stalerec = mrec;

            // if we're coming from an external authentication source, note that
            if (data.exSite != null) {
                _memberRepo.mapExternalAccount(data.exSite, data.exAuthUserId, mrec.memberId);
            }

            // create their money account, granting them some starting flow
            _moneyLogic.createMoneyAccount(
                mrec.memberId, data.isRegistering ? CoinAwards.CREATED_ACCOUNT : 0);

            // record to the event log that we created a new account
            String iid = (data.invite == null) ? null : data.invite.inviteId;
            String vid = (data.vinfo == null) ? null : data.vinfo.id;
            _eventLog.accountCreated(
                mrec.memberId, !data.isRegistering, iid, mrec.affiliateMemberId, vid);

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
                    _memberRepo.purgeMembers(Collections.singletonList(stalerec.memberId));
                    _memberRepo.deleteMembers(Collections.singletonList(stalerec.memberId));
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
        return _serverMsgs.getBundle("server").get("m.permaguest_name", memberId);
    }

    protected void validateRegistrationInfo (int[] birthdayYMD, String email, String displayName)
        throws ServiceException
    {
        // check age restriction
        if (birthdayYMD != null) {
            java.sql.Date bday = ProfileRecord.fromDateVec(birthdayYMD);
            if (bday.compareTo(Calendars.now().addYears(-13).toDate()) > 0) {
                log.warning("User submitted invalid birthdate", "date", bday);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }
        }

        // make sure the email is valid and not too long (this is also validated on the client)
        if (!MailUtil.isValidAddress(email) ||
            email.length() > MemberName.MAX_EMAIL_LENGTH) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        }

        // validate display name for length, whitespace and some racist content
        _memberLogic.validateDisplayName(displayName, false);
    }

    protected int resolveAffiliate (int affiliateId, InvitationRecord invite)
    {
        return (invite == null) ? affiliateId : invite.inviterId;
    }

    protected String checkCreateId (String account, VisitorInfo visitor)
    {
        return checkCreateId(account, (visitor == null) ? null : visitor.id);
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
        public final AffiliateCookie affiliate;
        public int themeId;

        // optional bits
        public String realName;
        public InvitationRecord invite;
        public int[] birthdayYMD;
        public ExternalSiteId exSite;
        public String exAuthUserId;
        public String location;

        public AccountData (boolean isRegistering, String email, String password,
                            String displayName, VisitorInfo vinfo, int themeId,
                            AffiliateCookie affiliate)
        {
            this.isRegistering = isRegistering;
            this.email = email.trim().toLowerCase();
            this.password = password.trim();
            this.displayName = displayName.trim();
            this.vinfo = vinfo;
            this.affiliate = affiliate;
            this.themeId = themeId;
        }
    }

    @Inject protected AuthenticationDomain _defaultDomain;
    @Inject protected MailSender _mailer;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberManager _memberMan;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected ServerMessages _serverMsgs;

    /** Prefix of permaguest display names. They have to create an account to get a real one. */
    protected static final String PERMAGUEST_DISPLAY_PREFIX = "Guest";

    /** The regular expression defining valid permanames. */
    protected static final String PERMANAME_REGEX = "^[a-z][_a-z0-9]*$";
}
