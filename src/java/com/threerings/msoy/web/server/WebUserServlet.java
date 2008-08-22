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
import java.util.Calendar;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.samskivert.io.PersistenceException;
import com.samskivert.io.StreamUtil;
import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.net.MailUtil;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.data.CoinAwards;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.server.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.money.server.MoneyNodeActions;
import com.threerings.msoy.money.server.MoneyResult;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.server.MailLogic;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.server.FriendManager;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.CaptchaException;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.RegisterInfo;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.PresentsDObjectMgr;

/**
 * Provides the server implementation of {@link WebUserService}.
 */
public class WebUserServlet extends MsoyServiceServlet
    implements WebUserService
{
    // from interface WebUserService
    public SessionData login (
        String clientVersion, String username, String password, int expireDays)
        throws ServiceException
    {
        checkClientVersion(clientVersion, username);
        // we are running on a servlet thread at this point and can thus talk to the authenticator
        // directly as it is thread safe (and it blocks) and we are allowed to block
        return startSession(_author.authenticateSession(username, password), expireDays);
    }

    // from interface WebUserService
    public SessionData register (String clientVersion, RegisterInfo info)
        throws ServiceException
    {
        checkClientVersion(clientVersion, info.email);

        // check age restriction
        java.sql.Date birthday = ProfileRecord.fromDateVec(info.birthday);
        Calendar thirteenYearsAgo = Calendar.getInstance();
        thirteenYearsAgo.add(Calendar.YEAR, -13);
        if (birthday.compareTo(thirteenYearsAgo.getTime()) > 0) {
            log.warning("User submitted invalid birtdate [date=" + birthday + "].");
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        // check invitation validity
        boolean ignoreRestrict = false;
        InvitationRecord invite = null;
        if (info.inviteId != null) {
            try {
                invite = _memberRepo.inviteAvailable(info.inviteId);
                if (invite == null) {
                    throw new ServiceException(MsoyAuthCodes.INVITE_ALREADY_REDEEMED);
                }
                ignoreRestrict = true;
            } catch (PersistenceException pe) {
                log.warning("Checking invite availability failed", "inviteId", info.inviteId, pe);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }
        }

        // validate display name length (this is enforced on the client)
        String displayName = info.displayName.trim();
        if (!MemberName.isValidDisplayName(displayName) ||
            !MemberName.isValidNonSupportName(displayName)) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // validate the captcha if appropriate
        if (!StringUtil.isBlank(ServerConfig.recaptchaPrivateKey)) {
            verifyCaptcha(info.captchaChallenge, info.captchaResponse);
        }

        // we are running on a servlet thread at this point and can thus talk to the authenticator
        // directly as it is thread safe (and it blocks) and we are allowed to block
        final MemberRecord mrec = _author.createAccount(
            info.email, info.password, info.displayName, ignoreRestrict, invite,
            info.referral);

        // store the user's birthday and realname in their profile
        ProfileRecord prec = new ProfileRecord();
        prec.memberId = mrec.memberId;
        prec.birthday = birthday;
        prec.realName = info.info.realName;
        prec.setPhoto(info.photo);
        try {
            _profileRepo.storeProfile(prec);
        } catch (PersistenceException pe) {
            log.warning("Failed to create initial profile [prec=" + prec + "]", pe);
            // keep on keepin' on
        }

        // if they have accumulated flow as a guest, transfer that to their account (note: only
        // negative ids are valid guest ids)
        if (info.guestId < 0) {
            _peerMan.invokeNodeAction(new TransferGuestFlowAction(info.guestId, mrec.memberId));
        }

        // if we are responding to an invitation, wire that all up
        if (invite != null && invite.inviterId != 0) {
            try {
                _memberRepo.linkInvite(info.inviteId, mrec);
            } catch (PersistenceException pe) {
                log.warning("Linking invites failed", "inviteId", info.inviteId,
                            "memberId", mrec.memberId, pe);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            MemberRecord inviter;
            try {
                inviter = _memberRepo.loadMember(invite.inviterId);
            } catch (PersistenceException pe) {
                log.warning("Failed to lookup inviter [inviteId=" + info.inviteId +
                        ", memberId=" + invite.inviterId + "]", pe);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            if (inviter != null) {
                // send them a whirled mail informing them of the acceptance
                String subject = _serverMsgs.getBundle("server").get("m.invite_accepted_subject");
                String body = _serverMsgs.getBundle("server").get(
                    "m.invite_accepted_body", invite.inviteeEmail, displayName);
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
                        inviter.memberId, 0, 0, null, CoinAwards.INVITED_FRIEND_JOINED,
                        "", UserAction.INVITED_FRIEND_JOINED);
                } catch (Exception e) {
                    log.warning("Failed to wire up friendship for created account " +
                            "[member=" + mrec.who() + ", inviter=" + inviter.who() + "].", e);
                }

                // dispatch a notification to the inviter that the invite was accepted
                final InvitationRecord finvite = invite;
                final String fdisplayName = displayName;
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
                            finvite.inviterId, fdisplayName, mrec.memberId, finvite.inviteeEmail);
                    }
                });

                // increment the inviter's INVITES_ACCEPTED stat
                _statLogic.incrementStat(inviter.memberId, StatType.INVITES_ACCEPTED, 1);
            }
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

        } catch (PersistenceException pe) {
            log.warning("Failed to refresh session [tok=" + authtok + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
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
        try {
            String code = _author.generatePasswordResetCode(email);
            if (code == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }

            MemberRecord mrec = _memberRepo.loadMember(email);
            if (mrec == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }

            // create and send a forgot password email
            try {
                MailSender.sendEmail(email, ServerConfig.getFromAddress(), "forgotPassword",
                                     "server_url", ServerConfig.getServerURL(),
                                     "email", mrec.accountName,
                                     "memberId", mrec.memberId,
                                     "code", code);
            } catch (Exception e) {
                throw new ServiceException(e.getMessage());
            }

        } catch (PersistenceException pe) {
            log.warning("Failed to lookup account [email=" + email + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    // from interface WebUserService
    public void updateEmail (String newEmail)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        if (!MailUtil.isValidAddress(newEmail)) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        }

        try {
            _memberRepo.configureAccountName(mrec.memberId, newEmail);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        } catch (PersistenceException pe) {
            log.warning("Failed to set email [who=" + mrec.memberId +
                    ", email=" + newEmail + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // let the authenticator know that we updated our account name
        _author.updateAccount(mrec.accountName, newEmail, null, null);
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
            try {
                _memberRepo.storeFlags(mrec);
            } catch (PersistenceException pe) {
                log.warning("Failed to update flags [who=" + mrec.memberId +
                        ", flags=" + mrec.flags + "].", pe);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }
    }

    // from interface WebUserService
    public void updatePassword (String newPassword)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        _author.updateAccount(mrec.accountName, null, null, newPassword);
    }

    // from interface WebUserService
    public boolean resetPassword (int memberId, String code, String newPassword)
        throws ServiceException
    {
        try {
            MemberRecord mrec = _memberRepo.loadMember(memberId);
            if (mrec == null) {
                log.info("No such member for password reset " + memberId + ".");
                return false;
            }

            if (!_author.validatePasswordResetCode(mrec.accountName, code)) {
                String actual = _author.generatePasswordResetCode(mrec.accountName);
                log.info("Code mismatch for password reset [id=" + memberId + ", code=" + code +
                         ", actual=" + actual + "].");
                return false;
            }

            _author.updateAccount(mrec.accountName, null, null, newPassword);
            return true;

        } catch (PersistenceException pe) {
            log.warning("Failed to reset password [who=" + memberId +
                    ", code=" + code + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
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
        } catch (PersistenceException pe) {
            log.warning("Failed to set permaname [who=" + mrec.memberId +
                    ", pname=" + permaName + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // let the authenticator know that we updated our permaname
        _author.updateAccount(mrec.accountName, null, permaName, null);
    }

    // from interface WebUserService
    public AccountInfo getAccountInfo ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        try {
            AccountInfo ainfo = new AccountInfo();
            ProfileRecord prec = _profileRepo.loadProfile(mrec.memberId);
            if (prec != null) {
                ainfo.realName = prec.realName;
            }
            ainfo.emailWhirledMail = !mrec.isSet(MemberRecord.Flag.NO_WHIRLED_MAIL_TO_EMAIL);
            ainfo.emailAnnouncements = !mrec.isSet(MemberRecord.Flag.NO_ANNOUNCE_EMAIL);
            return ainfo;

        } catch (PersistenceException pe) {
            log.warning("Failed to fetch account info [who=" + mrec.memberId +
                "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WebUserService
    public void updateAccountInfo (AccountInfo info)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        try {
            ProfileRecord prec = _profileRepo.loadProfile(mrec.memberId);
            prec.realName = info.realName;
            _profileRepo.storeProfile(prec);
        } catch (PersistenceException pe) {
            log.warning("Failed to update user account info [who=" + mrec.memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
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

    protected SessionData startSession (MemberRecord mrec, int expireDays)
        throws ServiceException
    {
        try {
            // if they made it through that gauntlet, create or update their session token
            WebCreds creds = mrec.toCreds(_memberRepo.startOrJoinSession(mrec.memberId, expireDays));
            _mhelper.mapMemberId(creds.token, mrec.memberId);
            return loadSessionData(mrec, creds, _moneyLogic.getMoneyFor(mrec.memberId));

        } catch (PersistenceException pe) {
            log.warning("Failed to start session [for=" + mrec.accountName + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    protected SessionData loadSessionData (MemberRecord mrec, WebCreds creds, MemberMoney money)
    {
        SessionData data = new SessionData();
        data.creds = creds;

        // fill in their flow, gold and level
        data.flow = money.getCoins();
        // data.gold = TODO
        data.level = mrec.level;

        // load up their new mail count
        try {
            data.newMailCount = _mailRepo.loadUnreadConvoCount(mrec.memberId);
        } catch (PersistenceException pe) {
            log.warning("Failed to load new mail count [id=" + mrec.memberId + "].", pe);
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
            final int flow = memobj.flow;
            if (flow > 0) {
                log.info("Transfering guest-accumulated flow to user [guestId=" + _memberId +
                         ", memberId=" + _toMemberId + "].");
                _invoker.postUnit(new Invoker.Unit() {
                    @Override public boolean invoke () {
                        try {
                            MoneyResult res = _moneyLogic.awardCoins(
                                _toMemberId, 0, 0, null, flow, "", UserAction.TRANSFER_FROM_GUEST);
                            _moneyNodeActions.moneyUpdated(res.getNewMemberMoney());
                            return true;
                        } catch (Exception e) {
                            log.warning("Unable to grant coins", "to", _toMemberId,
                                        "action", UserAction.TRANSFER_FROM_GUEST, "amount", flow, e);
                            return false;
                        }
                    }
                });
            }
        }

        protected int _toMemberId;

        @Inject protected transient MoneyLogic _moneyLogic;
        @Inject protected transient MoneyNodeActions _moneyNodeActions;
        @Inject @MainInvoker protected transient Invoker _invoker;
    }

    // our dependencies
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected MsoyAuthenticator _author;
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected FriendManager _friendMan;
    @Inject protected NotificationManager _notifyMan;
    @Inject protected MailLogic _mailLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected StatLogic _statLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected MoneyLogic _moneyLogic;

    /** The regular expression defining valid permanames. */
    protected static final String PERMANAME_REGEX = "^[a-z][_a-z0-9]*$";
}
