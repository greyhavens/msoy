//
// $Id$

package com.threerings.msoy.web.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.json.JSONException;
import org.json.JSONObject;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.user.OOOUser;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.admin.server.ABTestLogic;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.apps.server.persist.AppInfoRecord;
import com.threerings.msoy.apps.server.persist.AppRepository;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.CoinAwards;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.LaunchConfig;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.facebook.server.FacebookLogic;
import com.threerings.msoy.facebook.server.persist.FacebookInfoRecord;
import com.threerings.msoy.facebook.server.persist.FacebookRepository;
import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.group.server.ThemeLogic;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.notify.server.MsoyNotificationManager;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.server.persist.InvitationRecord;
import com.threerings.msoy.person.server.persist.InviteRepository;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.server.AccountLogic;
import com.threerings.msoy.server.ExternalAuthHandler;
import com.threerings.msoy.server.ExternalAuthLogic;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.PopularPlacesSnapshot.Place;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberWarningRecord;
import com.threerings.msoy.server.persist.MsoyOOOUserRepository;
import com.threerings.msoy.server.util.MailSender.By;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.web.gwt.AccountInfo;
import com.threerings.msoy.web.gwt.BannedException;
import com.threerings.msoy.web.gwt.CaptchaException;
import com.threerings.msoy.web.gwt.ClientMode;
import com.threerings.msoy.web.gwt.ConnectConfig;
import com.threerings.msoy.web.gwt.Embedding;
import com.threerings.msoy.web.gwt.ExternalCreds;
import com.threerings.msoy.web.gwt.ExternalSiteId;
import com.threerings.msoy.web.gwt.RegisterInfo;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.gwt.WebUserService;

import static com.threerings.msoy.Log.log;

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
    public SessionData externalLogon (
        String clientVersion, ExternalCreds creds, VisitorInfo vinfo, int expireDays, int appId)
        throws ServiceException
    {
        // TODO: deal with joining to the permaguest account, if any
        checkClientVersion(clientVersion, creds.getPlaceholderAddress());
        AffiliateCookie affiliate = AffiliateCookie.fromWeb(getThreadLocalRequest());
        return startSession(_author.authenticateSession(creds, vinfo, affiliate, appId), expireDays);
    }

    // from interface WebUserService
    public RegisterData register (String clientVersion, RegisterInfo info, boolean forceValidation)
        throws ServiceException
    {
        checkClientVersion(clientVersion, info.email);

        // check invitation validity
        InvitationRecord invite = null;
        if (info.inviteId != null) {
            invite = _inviteRepo.inviteAvailable(info.inviteId);
            if (invite == null) {
                throw new ServiceException(MsoyAuthCodes.INVITE_ALREADY_REDEEMED);
            }
        }

        // check registration limits
        if (!_runtime.server.registrationEnabled && invite == null) {
            throw new ServiceException(MsoyAuthCodes.NO_REGISTRATIONS);
        }

        // having registered users with placeholder emails would be a pain, so prevent it
        if (MemberMailUtil.isPlaceholderAddress(info.email)) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        }

        // validate the captcha if appropriate
        if (!StringUtil.isBlank(ServerConfig.recaptchaPrivateKey)) {
            verifyCaptcha(info.captchaChallenge, info.captchaResponse);
        }

        final MemberRecord mrec = (info.permaguestId > 0) ?
            _accountLogic.savePermaguestAccount(
                info.permaguestId, info.email, info.password, info.displayName, info.info.realName,
                invite, info.visitor, info.birthday) :
            _accountLogic.createWebAccount(
                info.email, info.password, info.displayName, info.info.realName, invite,
                info.visitor, 0, AffiliateCookie.fromWeb(getThreadLocalRequest()), info.birthday);

        // TODO: consider moving the below code into AccountLogic

        // if we are responding to an invitation, wire that all up
        if (invite != null && invite.inviterId != 0) {
            _inviteRepo.linkInvite(info.inviteId, mrec);

            // TODO: if forceValidation, we shouldn't tell the inviter anything yet
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

        // send a welcome or validation email to our new registrant
        String tmpl = forceValidation ? "forceValidation" : "welcome";
        _mailer.sendTemplateEmail(
            MailSender.By.HUMAN, info.email, ServerConfig.getFromAddress(), tmpl,
            "server_url", ServerConfig.getServerURL(), "name", info.displayName,
            "email", info.email, "memberId", mrec.memberId,
            "code", _accountLogic.generateValidationCode(mrec));

        // start our first session with the newly created account
        RegisterData data = new RegisterData();
        startSession(mrec, info.expireDays, data);

        // and load up our entry vector and send it back to GWT for its nefarious porpoises
        data.entryVector = _memberRepo.loadEntryVector(mrec.memberId);

        // we are not doing a test, but assign them a group to indicate a new account
        data.group = SessionData.Group.A;

        return data;
    }

    // from interface WebUserService
    public SessionData validateSession (
        String clientVersion, String authtok, int expireDays, int appId)
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
            SessionData data = new SessionData();
            MemberMoney money = _moneyLogic.getMoneyFor(mrec.memberId);
            initSessionData(mrec, creds, money, data);
            try {
                _facebookLogic.initSessionData(
                    ExternalSiteId.facebookApp(appId), mrec, money, data);
            } catch (Exception e) {
                log.warning("Failed to set up SessionData.Extra",
                    "memberId", mrec.memberId, e);
            }
            return data;

        } catch (Exception e) {
            log.warning("Failed to validate session", "tok", authtok, e);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    // from interface WebUserService
    public boolean linkExternalAccount (ExternalCreds creds, boolean override)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        ExternalAuthHandler handler = _extLogic.getHandler(creds.getSite());
        if (handler == null) {
            log.warning("Requested to link to unknown external account type", "who", mrec.who(),
                        "creds", creds, "override", override);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // make sure the credentials are kosher
        handler.validateCredentials(creds);

        // TODO: expose the default FB connect info to the client
        ExternalSiteId siteId = creds.getSite();
        if (!siteId.equals(ExternalSiteId.FB_CONNECT_DEFAULT)) {
            log.warning("Non-default external site?", "siteId", siteId);
            siteId = ExternalSiteId.FB_CONNECT_DEFAULT;
        }
        // determine whether or not this external id is already mapped
        int memberId = _memberRepo.lookupExternalAccount(siteId, creds.getUserId());
        if (memberId != 0 && !override) {
            return false;
        }

        // if we made it this far, then wire things on up
        _memberRepo.mapExternalAccount(siteId, creds.getUserId(), mrec.memberId);

        // look to see if we should map any friends
        ExternalAuthHandler.Info info = null;
        try {
            info = handler.getInfo(creds);
            _extLogic.wireUpExternalFriends(mrec.memberId, creds.getSite().auther, info.friendIds);
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
    public LaunchConfig loadLaunchConfig (int gameId)
        throws ServiceException
    {
        return _gameLogic.loadLaunchConfig(gameId);
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
            MailSender.By.HUMAN, email, ServerConfig.getFromAddress(), "forgotPassword",
            "server_url", ServerConfig.getServerURL(), "email", mrec.accountName,
            "memberId", mrec.memberId, "code", code);
    }

    // from interface WebUserService
    public void updateEmail (String newEmail)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        _accountLogic.updateAccountName(mrec, newEmail);
    }

    // from interface WebUserService
    public void updatePrefs (boolean emailOnWhirledMail, boolean emailAnnouncements, boolean autoFlash)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // update their mail preferences if appropriate
        int oflags = mrec.flags;
        mrec.setFlag(MemberRecord.Flag.NO_WHIRLED_MAIL_TO_EMAIL, !emailOnWhirledMail);
        mrec.setFlag(MemberRecord.Flag.NO_ANNOUNCE_EMAIL, !emailAnnouncements);
        mrec.setFlag(MemberRecord.Flag.NO_AUTO_FLASH, !autoFlash);
        if (mrec.flags != oflags) {
            _memberRepo.storeFlags(mrec);
        }
    }

    // from interface WebUserService
    public void updatePassword (String newPassword)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        _accountLogic.updatePassword(mrec, newPassword);
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
        _accountLogic.updatePassword(mrec, newPassword);
        return true;
    }

    // from interface WebUserService
    public void configurePermaName (String permaName)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        _accountLogic.configurePermaName(mrec, permaName);
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
        ainfo.autoFlash = !mrec.isSet(MemberRecord.Flag.NO_AUTO_FLASH);
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
        ainfo.externalSites = _memberRepo.loadExternalMappings(mrec.memberId);

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
        _accountLogic.sendValidationEmail(mrec);
    }

    // from interface WebUserService
    public SessionData validateEmail (int memberId, String code)
        throws ServiceException
    {
        MemberRecord mrec = _memberRepo.loadMember(memberId);
        if (mrec == null || !_accountLogic.generateValidationCode(mrec).equals(code)) {
            return null; // invalido!
        }

        // are they banned?
        OOOUser user = _authrep.loadUserByEmail(mrec.accountName, false);
        if (user == null || user.holdsToken(OOOUser.MSOY_BANNED)) {
            throw new BannedException(MsoyAuthCodes.BANNED, null);
        }

        // are they temp-banned?
        MemberWarningRecord record = _memberRepo.loadMemberWarningRecord(memberId);
        if (record != null && record.banExpires != null) {
            // figure out how many seconds are left on the temp ban
            Date now = new Date();
            if (now.before(record.banExpires)) {
                int expires = (int)((record.banExpires.getTime() - now.getTime())/1000L);
                throw new BannedException(MsoyAuthCodes.TEMP_BANNED, record.warning, expires);
            }
        }

        // update their validated flag in the repository
        mrec.setFlag(MemberRecord.Flag.VALIDATED, true);
        _memberRepo.storeFlags(mrec);

        // automatically start a session for them
        SessionData data = startSession(mrec, 1); // TODO: expire days?

        // we are not doing a test, but assign them a group to indicate a new account
        data.group = SessionData.Group.A;

        return data;
    }

    // from interface WebUserService
    public void requestAccountDeletion ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        _mailer.sendTemplateEmail(By.HUMAN, mrec.accountName, ServerConfig.getFromAddress(),
            "deleteAccount", "name", mrec.name, "code", generateDeleteSecret(mrec),
            "server_url", DeploymentConfig.serverURL);
    }

    // from interface WebUserService
    public void deleteAccount (String password, String code)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        if (!ObjectUtil.equals(code, generateDeleteSecret(mrec))) {
            throw new ServiceException(ServiceCodes.E_ACCOUNT_DELETE_HASH_MISMATCH);
        }

        // this will throw an exception if the password is invalid (it should not be possible for
        // the account not to exist)
        _accountLogic.getDomain(mrec.accountName).authenticateAccount(mrec.accountName, password);

        // if they supplied the right password, then do the deed; bye bye bonzo
        log.info("Deleting account (bye bye)", "who", mrec.accountName);
        _memberLogic.deleteMembers(Collections.singletonList(mrec.memberId));
    }

    @Override // from WebUserService
    public AppResult getApp (int appId)
        throws ServiceException
    {
        AppResult result = new AppResult();
        AppInfoRecord appInfo = _appRepo.loadAppInfo(appId);
        if (appInfo == null) {
            return null;
        }
        FacebookInfoRecord fbinfo = _facebookRepo.loadAppFacebookInfo(appId);
        result.appId = appId;
        result.facebookAppId = fbinfo.fbUid;
        result.facebookApiKey = fbinfo.apiKey;
        return result;
    }

    @Override
    public Embedding getEmbedding ()
        throws ServiceException
    {
        String host = getThreadLocalRequest().getHeader("Host");
        if (host == null) {
            return null;
        }

        // Strip out the port
        int idx = host.indexOf(':');
        String domain = (idx >= 0) ? host.substring(0, idx) : host;

        // Avoid hitting the DB if this request is for the hard coded default
        if (domain.equals(DeploymentConfig.serverHost)) {
            return null;
        }

        AppInfoRecord app = _appRepo.loadAppInfo(domain);
        if (app == null) {
            return null;
        }

        return new Embedding(app.clientMode, app.appId);
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

    /**
     * Called when we logon or register.
     */
    protected SessionData startSession (MemberRecord mrec, int expireDays)
        throws ServiceException
    {
        SessionData data = new SessionData();
        startSession(mrec, expireDays, data);
        return data;
    }

    protected void startSession (MemberRecord mrec, int expireDays, SessionData data)
        throws ServiceException
    {
        AffiliateCookie.clear(getThreadLocalResponse());
        // start or rejoin our session (creating a token)
        WebCreds creds = mrec.toCreds(_memberRepo.startOrJoinSession(mrec.memberId, expireDays));
        // map that token to our member id for faster lookup
        _mhelper.mapMemberId(creds.token, mrec.memberId);
        // finally fill in our various session data
        initSessionData(mrec, creds, _moneyLogic.getMoneyFor(mrec.memberId), data);
    }

    protected void initSessionData (
        MemberRecord mrec, WebCreds creds, MemberMoney money, SessionData data)
    {
        // fill in their web credentials
        data.creds = creds;

        // fill in their flow, gold and level
        data.flow = money.coins;
        data.gold = money.bars;
        data.level = mrec.level;

        // load up their visitor info
        data.visitor = new VisitorInfo(mrec.visitorId, true);

        // load up their new mail count
        try {
            data.newMailCount = _mailLogic.getUnreadConvoCount(mrec.memberId);
        } catch (Exception e) {
            log.warning("Failed to load new mail count [id=" + mrec.memberId + "].", e);
        }

        data.themeId = mrec.themeGroupId;

        List<Place> topThemes = _memberMan.getPPSnapshot().getTopThemes();

        Set<Integer> groupIds = Sets.newHashSet();
        for (Place themePlace : topThemes) {
            if (groupIds.size() == TOP_THEMES) {
                break;
            }
            groupIds.add(themePlace.placeId);
        }
        Map<Integer, GroupRecord> groupMap = Maps.newHashMap();
        for (GroupRecord group : _groupRepo.loadGroups(groupIds)) {
            groupMap.put(group.groupId, group);
        }

        JSONObject themes = new JSONObject();
        for (Place themePlace : topThemes) {
            GroupRecord group = groupMap.get(themePlace.placeId);
            if (group == null) {
                // this signals that we've exhausted our loaded groups and we're done
                break;
            }
            JSONObject themeObj = new JSONObject();
            try {
                themeObj.put("groupId", themePlace.placeId);
                if (group.logoMediaHash != null) {
                    themeObj.put("logoHash", HashMediaDesc.hashToString(group.logoMediaHash));
                    themeObj.put("logoType", group.logoMimeType);
                }
                themeObj.put("pop", themePlace.population);
                themeObj.put("name", themePlace.name);
                themeObj.put("homeId", group.homeSceneId);
                themes.accumulate("themes", themeObj);

            } catch (JSONException e) {
                log.warning("Failed to JSON-encode place: " + themePlace);
            }
        }
        data.topThemes = themes.toString();
    }

    protected static String generateDeleteSecret (MemberRecord mrec)
        throws ServiceException
    {
        return StringUtil.md5hex(mrec.accountName + mrec.memberId + ServerConfig.sharedSecret);
    }

    // our dependencies
    @Inject protected ABTestLogic _testLogic;
    @Inject protected AccountLogic _accountLogic;
    @Inject protected AppRepository _appRepo;
    @Inject protected ExternalAuthLogic _extLogic;
    @Inject protected FacebookLogic _facebookLogic;
    @Inject protected FacebookRepository _facebookRepo;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected InviteRepository _inviteRepo;
    @Inject protected MailLogic _mailLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected MailSender _mailer;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberManager _memberMan;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyAuthenticator _author;
    @Inject protected MsoyOOOUserRepository _authrep;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MsoyNotificationManager _notifyMan;
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected StatLogic _statLogic;
    @Inject protected ThemeLogic _themeLogic;

    protected static final int TOP_THEMES = 6;
}
