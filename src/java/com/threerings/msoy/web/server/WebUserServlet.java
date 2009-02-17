//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.io.StreamUtil;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.net.MailUtil;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.data.CoinAwards;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.server.AccountLogic;
import com.threerings.msoy.server.ExternalAuthHandler;
import com.threerings.msoy.server.ExternalAuthLogic;
import com.threerings.msoy.server.FriendManager;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.web.gwt.AccountInfo;
import com.threerings.msoy.web.gwt.CaptchaException;
import com.threerings.msoy.web.gwt.ConnectConfig;
import com.threerings.msoy.web.gwt.ExternalCreds;
import com.threerings.msoy.web.gwt.LaunchConfig;
import com.threerings.msoy.web.gwt.RegisterInfo;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.gwt.WebUserService;

/**
 * Provides the server implementation of {@link WebUserService}.
 */
public class WebUserServlet extends MsoyServiceServlet
    implements WebUserService
{
    // from interface WebUserService
    public SessionData logon (
        String clientVersion, String username, String password, int expireDays)
        throws ServiceException
    {
        checkClientVersion(clientVersion, username);
        // we are running on a servlet thread at this point and can thus talk to the authenticator
        // directly as it is thread safe (and it blocks) and we are allowed to block
        return startSession(_author.authenticateSession(username, password), expireDays);
    }

    // from interface WebUserService
    public SessionData externalLogon (String clientVersion, ExternalCreds creds, VisitorInfo vinfo,
                                      int expireDays)
        throws ServiceException
    {
        // TODO: deal with joining to the permaguest account, if any
        checkClientVersion(clientVersion, creds.getPlaceholderAddress());
        String affiliate = AffiliateCookie.get(getThreadLocalRequest());
        return startSession(_author.authenticateSession(creds, vinfo, affiliate), expireDays);
    }

    // from interface WebUserService
    public SessionData register (String clientVersion, RegisterInfo info)
        throws ServiceException
    {
        checkClientVersion(clientVersion, info.email);

        // check invitation validity
        InvitationRecord invite = null;
        if (info.inviteId != null) {
            invite = _memberRepo.inviteAvailable(info.inviteId);
            if (invite == null) {
                throw new ServiceException(MsoyAuthCodes.INVITE_ALREADY_REDEEMED);
            }
        }

        // check registration limits
        if (!_runtime.server.registrationEnabled && invite == null) {
            throw new ServiceException(MsoyAuthCodes.NO_REGISTRATIONS);
        }

        // having registered users with permaguest emails would be a pain, so prevent it
        if (MemberName.isPermaguest(info.email)) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        }

        // validate the captcha if appropriate
        if (!StringUtil.isBlank(ServerConfig.recaptchaPrivateKey)) {
            verifyCaptcha(info.captchaChallenge, info.captchaResponse);
        }

        final MemberRecord mrec = info.permaguestId > 0 ? 
            _accountLogic.savePermaguestAccount(info.permaguestId, info.email,
                info.password, info.displayName, info.info.realName, invite, info.visitor,
                AffiliateCookie.get(getThreadLocalRequest()), info.birthday) :
            _accountLogic.createWebAccount(info.email, info.password,
                info.displayName, info.info.realName, invite, info.visitor,
                AffiliateCookie.get(getThreadLocalRequest()), info.birthday);

        // TODO: consider moving the below code into AccountLogic

        // if they have accumulated flow as a guest, transfer that to their account (note: only
        // negative ids are valid guest ids)
        if (info.guestId < 0) {
            _peerMan.invokeNodeAction(new TransferGuestFlowAction(info.guestId, mrec.memberId));
        }

        // if we are responding to an invitation, wire that all up
        if (invite != null && invite.inviterId != 0) {
            _memberRepo.linkInvite(info.inviteId, mrec);

            MemberRecord inviter = _memberRepo.loadMember(invite.inviterId);
            if (inviter != null) {
                // send them a whirled mail informing them of the acceptance
                String subject = _serverMsgs.getBundle("server").get("m.invite_accepted_subject");
                String body = _serverMsgs.getBundle("server").get(
                    "m.invite_accepted_body", invite.inviteeEmail, mrec.name);
                try {
                    _mailLogic.startConversation(mrec, inviter, subject, body, null);
                } catch (Exception e) {
                    log.warning("Failed to sent invite accepted mail", e);
                }

                // establish the inviter's friendship with the invite acceptor
                _memberLogic.establishFriendship(inviter, mrec.memberId);

                try {
                    // pay out a sign up bonus to the inviter
                    _moneyLogic.awardCoins(
                        inviter.memberId, CoinAwards.INVITED_FRIEND_JOINED, true,
                        UserAction.invitedFriendJoined(inviter.memberId, mrec.getName()));
                } catch (Exception e) {
                    log.warning("Failed to wire up friendship for created account",
                        "member", mrec.who(), "inviter", inviter.who(), e);
                }

                // dispatch a notification to the inviter that the invite was accepted
                final InvitationRecord finvite = invite;
                _omgr.postRunnable(new Runnable() {
                    public void run () {
                        // TODO: This is really spammy; in fact, when somebody accepts your invite
                        // TODO: you may get, in practice, four separate notifications:
                        // TODO:  - Foo is now your friend.
                        // TODO:  - Foo accepted your invitation.
                        // TODO:  - You have new mail.
                        // TODO:  - Foo is now online.
                        // TODO: We'd like to bring this down to one or possibly two lines, and
                        // TODO: will tackle this problem when notification has been peerified.

                        // and possibly send a runtime notification as well
                        _notifyMan.notifyInvitationAccepted(
                            finvite.inviterId, mrec.getName(), finvite.inviteeEmail);
                    }
                });

                // increment the inviter's INVITES_ACCEPTED stat
                _statLogic.incrementStat(inviter.memberId, StatType.INVITES_ACCEPTED, 1);
            }
        }

        // send a welcome email to everyone except test group A
        int testGroup = _memberLogic.getABTestGroup("2008 11 welcome email", info.visitor, true);
        if (testGroup != 1) {
            MailSender.Parameters params = new MailSender.Parameters();
            params.set("server_url", ServerConfig.getServerURL());
            params.set("name", info.displayName);
            params.set("email", info.email);
            _mailer.sendTemplateEmail(info.email, ServerConfig.getFromAddress(), "welcome",
                params);
        }

        return startSession(mrec, info.expireDays);
    }

    // from interface WebUserService
    public SessionData validateSession (String clientVersion, String authtok, int expireDays)
        throws ServiceException
    {
        checkClientVersion(clientVersion, authtok);

        // refresh the token associated with their authentication session
        try {
            MemberRecord mrec = _memberRepo.refreshSession(authtok, expireDays);
            if (mrec == null) {
                return null;
            }

            WebCreds creds = mrec.toCreds(authtok);
            _mhelper.mapMemberId(creds.token, mrec.memberId);
            return loadSessionData(mrec, creds, _moneyLogic.getMoneyFor(mrec.memberId));

        } catch (Exception e) {
            log.warning("Failed to refresh session [tok=" + authtok + "].", e);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    // from interface WebUserService
    public boolean linkExternalAccount (ExternalCreds creds, boolean override)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        ExternalAuthHandler handler = _extLogic.getHandler(creds.getAuthSource());
        if (handler == null) {
            log.warning("Requested to link to unknown external account type", "who", mrec.who(),
                        "creds", creds, "override", override);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // make sure the credentials are kosher
        handler.validateCredentials(creds);

        // determine whether or not this external id is already mapped
        int memberId = _memberRepo.lookupExternalAccount(creds.getAuthSource(), creds.getUserId());
        if (memberId != 0 && !override) {
            return false;
        }

        // if we made it this far, then wire things on up
        _memberRepo.mapExternalAccount(creds.getAuthSource(), creds.getUserId(), mrec.memberId);

        // look to see if we should map any friends
        ExternalAuthHandler.Info info = null;
        try {
            info = handler.getInfo(creds);
            _extLogic.wireUpExternalFriends(mrec.memberId, creds.getAuthSource(), info.friendIds);
        } catch (Exception e) {
            log.warning("Failed to wire up external friends", "for", mrec.who(), "creds", creds,
                        "info", info, e);
        }

        return true;
    }

    // from interface WebUserService
    public ConnectConfig getConnectConfig ()
        throws ServiceException
    {
        ConnectConfig config = new ConnectConfig();
        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        config.httpPort = ServerConfig.httpPort;
        return config;
    }

    // from interface WebUserService
    public LaunchConfig loadLaunchConfig (int gameId, boolean assignGuestId)
        throws ServiceException
    {
        return _gameLogic.loadLaunchConfig(gameId, assignGuestId);
    }

    // from interface WebUserService
    public void sendForgotPasswordEmail (String email)
        throws ServiceException
    {
        String code = _accountLogic.generatePasswordResetCode(email);
        if (code == null) {
            throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
        }

        MemberRecord mrec = _memberRepo.loadMember(email);
        if (mrec == null) {
            throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
        }

        // create and send a forgot password email
        _mailer.sendTemplateEmail(
            email, ServerConfig.getFromAddress(), "forgotPassword",
            "server_url", ServerConfig.getServerURL(), "email", mrec.accountName,
            "memberId", mrec.memberId, "code", code);
    }

    // from interface WebUserService
    public void updateEmail (String newEmail)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure the email is valid and not too long (this is also validated on the client)
        if (!MailUtil.isValidAddress(newEmail) ||
            newEmail.length() > MemberName.MAX_EMAIL_LENGTH) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        }
        final String oldEmail = mrec.accountName;

        // first update their MemberRecord and fail if they request a duplicate name
        try {
            _memberRepo.configureAccountName(mrec.memberId, newEmail);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        }

        try {
            // let the authenticator know that we updated our account name
            _accountLogic.updateAccount(mrec.accountName, newEmail, null, null);
        } catch (ServiceException se) {
            // we need to roll back the account name change to preserve a proper mapping between
            // MemberRecord and the authenticator's record
            try {
                _memberRepo.configureAccountName(mrec.memberId, oldEmail);
            } catch (Exception e) {
                log.warning("Failed to roll back account name change", "who", mrec.who(),
                            "newEmail", newEmail, "oldEmail", oldEmail, e);
            }
            throw se;
        }

        // if we made it this far, mark the account as no longer validated
        mrec.setFlag(MemberRecord.Flag.VALIDATED, false);
        _memberRepo.storeFlags(mrec);

        // and send a new validation email
        mrec.accountName = newEmail;
        sendValidationEmail(mrec);
    }

    // from interface WebUserService
    public void updateEmailPrefs (boolean emailOnWhirledMail, boolean emailAnnouncements)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // update their mail preferences if appropriate
        int oflags = mrec.flags;
        mrec.setFlag(MemberRecord.Flag.NO_WHIRLED_MAIL_TO_EMAIL, !emailOnWhirledMail);
        mrec.setFlag(MemberRecord.Flag.NO_ANNOUNCE_EMAIL, !emailAnnouncements);
        if (mrec.flags != oflags) {
            _memberRepo.storeFlags(mrec);
        }
    }

    // from interface WebUserService
    public void updatePassword (String newPassword)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        _accountLogic.updateAccount(mrec.accountName, null, null, newPassword);
    }

    // from interface WebUserService
    public boolean resetPassword (int memberId, String code, String newPassword)
        throws ServiceException
    {
        MemberRecord mrec = _memberRepo.loadMember(memberId);
        if (mrec == null) {
            log.info("No such member for password reset " + memberId + ".");
            return false;
        }

        if (!_accountLogic.validatePasswordResetCode(mrec.accountName, code)) {
            String actual = _accountLogic.generatePasswordResetCode(mrec.accountName);
            log.info("Code mismatch for password reset [id=" + memberId + ", code=" + code +
                     ", actual=" + actual + "].");
            return false;
        }

        _accountLogic.updateAccount(mrec.accountName, null, null, newPassword);
        return true;
    }

    // from interface WebUserService
    public void configurePermaName (String permaName)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        if (mrec.permaName != null) {
            log.warning("Rejecting attempt to reassing permaname [who=" + mrec.accountName +
                        ", oname=" + mrec.permaName + ", nname=" + permaName + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (permaName == null ||
                permaName.length() < MemberName.MINIMUM_PERMANAME_LENGTH ||
                permaName.length() > MemberName.MAXIMUM_PERMANAME_LENGTH ||
                !permaName.matches(PERMANAME_REGEX)) {
            throw new ServiceException("e.invalid_permaname");
        }

        try {
            _memberRepo.configurePermaName(mrec.memberId, permaName);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_PERMANAME);
        }

        // let the authenticator know that we updated our permaname
        _accountLogic.updateAccount(mrec.accountName, null, permaName, null);
    }

    // from interface WebUserService
    public AccountInfo getAccountInfo ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        AccountInfo ainfo = new AccountInfo();
        ProfileRecord prec = _profileRepo.loadProfile(mrec.memberId);
        if (prec != null) {
            ainfo.realName = prec.realName;
        }
        ainfo.emailWhirledMail = !mrec.isSet(MemberRecord.Flag.NO_WHIRLED_MAIL_TO_EMAIL);
        ainfo.emailAnnouncements = !mrec.isSet(MemberRecord.Flag.NO_ANNOUNCE_EMAIL);
        ainfo.charityMemberId = mrec.charityMemberId;

        // Load charities and sort by name.
        List<CharityRecord> charities = _memberRepo.getCharities();
        ainfo.charities = Maps.newHashMap();
        for (CharityRecord charity : charities) {
            ainfo.charities.put(charity.memberId, new CharityInfo(charity.memberId, charity.core,
                charity.description));
        }
        ainfo.charityNames = Lists.newArrayList(
                _memberRepo.loadMemberNames(ainfo.charities.keySet()).values());
        ainfo.charityPhotos = Maps.newHashMap();
        for (ProfileRecord profileRec : _profileRepo.loadProfiles(ainfo.charities.keySet())) {
            ainfo.charityPhotos.put(profileRec.memberId, profileRec.getPhoto());
        }

        // load up any external authentication source mappings for this account
        ainfo.externalAuths = _memberRepo.loadExternalMappings(mrec.memberId);

        return ainfo;
    }

    // from interface WebUserService
    public void updateAccountInfo (AccountInfo info)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        ProfileRecord prec = _profileRepo.loadProfile(mrec.memberId);
        prec.realName = info.realName;
        // TODO: add and use ProfileRepository.updateRealName(), the code as is is racey
        _profileRepo.storeProfile(prec);
    }

    // from interface WebUserService
    public void updateCharity (int selectedCharityId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        _memberRepo.updateSelectedCharity(mrec.memberId, selectedCharityId);
    }

    // from interface WebUserService
    public void resendValidationEmail ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        if (mrec.isValidated()) {
            throw new ServiceException("e.already_validated");
        }
        sendValidationEmail(mrec);
    }

    // from interface WebUserService
    public boolean validateEmail (int memberId, String code)
        throws ServiceException
    {
        MemberRecord mrec = _memberRepo.loadMember(memberId);
        if (mrec == null) {
            return false;
        }
        if (!_accountLogic.generateValidationCode(mrec).equals(code)) {
            return false;
        }
        mrec.setFlag(MemberRecord.Flag.VALIDATED, true);
        _memberRepo.storeFlags(mrec);
        return true;
    }

    protected void checkClientVersion (String clientVersion, String who)
        throws ServiceException
    {
        if (!DeploymentConfig.version.equals(clientVersion)) {
            log.info("Refusing wrong version [who=" + who + ", cvers=" + clientVersion +
                     ", svers=" + DeploymentConfig.version + "].");
            throw new ServiceException(MsoyAuthCodes.VERSION_MISMATCH);
        }
    }

    protected void sendValidationEmail (MemberRecord mrec)
    {
        _mailer.sendTemplateEmail(
            mrec.accountName, ServerConfig.getFromAddress(), "revalidateEmail",
            "server_url", ServerConfig.getServerURL(), "email", mrec.accountName,
            "memberId", mrec.memberId, "code", _accountLogic.generateValidationCode(mrec));
    }

    protected void verifyCaptcha (String challenge, String response)
        throws ServiceException
    {
        if (challenge == null || response == null) {
            log.warning("Registration request with invalid captcha [challenge=" + challenge +
                        ", response=" + response + "].");
            throw new CaptchaException(MsoyAuthCodes.FAILED_CAPTCHA);
        }

        OutputStream out = null;
        InputStream in = null;
        try {
            // the reCaptcha verify api
            URL curl = new URL("http://api-verify.recaptcha.net/verify");
            HttpURLConnection conn = (HttpURLConnection)curl.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String ip = getThreadLocalRequest().getRemoteAddr();
            StringBuilder postData = new StringBuilder("privatekey=");
            postData.append(URLEncoder.encode(ServerConfig.recaptchaPrivateKey, "UTF-8"));
            postData.append("&remoteip=").append(URLEncoder.encode(ip, "UTF-8"));
            postData.append("&challenge=").append(URLEncoder.encode(challenge, "UTF-8"));
            postData.append("&response=").append(URLEncoder.encode(response, "UTF-8"));
            out = conn.getOutputStream();
            out.write(postData.toString().getBytes("UTF-8"));
            out.flush();

            in = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            // see if the response was valid
            if ("true".equals(br.readLine())) {
                return;
            }

            String error = br.readLine();
            // we're not supposed to rely on these error codes, but reCaptcha doesn't give
            // AJAX users any other options for error management
            if (!"incorrect-captcha-sol".equals(error)) {
                log.warning("Failed to verify captcha information [error=" + error + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            throw new CaptchaException(MsoyAuthCodes.FAILED_CAPTCHA);

        } catch (MalformedURLException mue) {
            log.warning("Failed to verify captcha information.", mue);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        } catch (IOException ioe) {
            log.warning("Failed to verify captcha information.", ioe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        } finally {
            StreamUtil.close(in);
            StreamUtil.close(out);
        }
    }

    /**
     * Called when we logon or register.
     */
    protected SessionData startSession (MemberRecord mrec, int expireDays)
        throws ServiceException
    {
        AffiliateCookie.clear(getThreadLocalResponse());

        // if they made it through that gauntlet, create or update their session token
        WebCreds creds = mrec.toCreds(_memberRepo.startOrJoinSession(mrec.memberId, expireDays));
        _mhelper.mapMemberId(creds.token, mrec.memberId);
        return loadSessionData(mrec, creds, _moneyLogic.getMoneyFor(mrec.memberId));
    }

    protected SessionData loadSessionData (MemberRecord mrec, WebCreds creds, MemberMoney money)
    {
        SessionData data = new SessionData();
        data.creds = creds;

        // fill in their flow, gold and level
        data.flow = money.coins;
        data.gold = money.bars;
        data.level = mrec.level;

        // load up their visitor info
        data.visitor = new VisitorInfo(mrec.visitorId, true);

        // load up their new mail count
        try {
            data.newMailCount = _mailRepo.loadUnreadConvoCount(mrec.memberId);
        } catch (Exception e) {
            log.warning("Failed to load new mail count [id=" + mrec.memberId + "].", e);
        }

        return data;
    }

    protected static class TransferGuestFlowAction extends MemberNodeAction
    {
        public TransferGuestFlowAction (int fromGuestId, int toMemberId) {
            super(fromGuestId);
            Preconditions.checkArgument(fromGuestId < 0, "guest id must be < 0: " + fromGuestId);
            _toMemberId = toMemberId;
        }

        public TransferGuestFlowAction () {
        }

        @Override
        protected void execute (MemberObject memobj) {
            final int coins = memobj.coins;
            if (coins > 0) {
                log.info("Transfering guest-accumulated coins to user [guestId=" + _memberId +
                         ", memberId=" + _toMemberId + "].");
                _invoker.postUnit(new Invoker.Unit() {
                    @Override public boolean invoke () {
                        UserAction action = UserAction.transferFromGuest(_toMemberId);
                        try {
                            _moneyLogic.awardCoins(_toMemberId, coins, true, action);
                            return true;
                        } catch (Exception e) {
                            log.warning("Unable to grant coins", "to", _toMemberId,
                                        "action", action, "amount", coins, e);
                            return false;
                        }
                    }
                });
            }
        }

        protected int _toMemberId;

        @Inject protected transient MoneyLogic _moneyLogic;
        @Inject @MainInvoker protected transient Invoker _invoker;
    }

    // our dependencies
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected MsoyAuthenticator _author;
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected ExternalAuthLogic _extLogic;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected FriendManager _friendMan;
    @Inject protected NotificationManager _notifyMan;
    @Inject protected MailSender _mailer;
    @Inject protected MailLogic _mailLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected StatLogic _statLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected AccountLogic _accountLogic;

    /** The regular expression defining valid permanames. */
    protected static final String PERMANAME_REGEX = "^[a-z][_a-z0-9]*$";
}
