//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import java.util.logging.Level;

import org.apache.velocity.VelocityContext;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.net.MailUtil;
import com.samskivert.util.ResultListener;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.person.data.MailFolder;
import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.person.server.persist.ProfileRecord;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link WebUserService}.
 */
public class WebUserServlet extends MsoyServiceServlet
    implements WebUserService
{
    // from interface WebUserService
    public SessionData login (long clientVersion, String username, String password, int expireDays)
        throws ServiceException
    {
        checkClientVersion(clientVersion, username);
        // we are running on a servlet thread at this point and can thus talk to the authenticator
        // directly as it is thread safe (and it blocks) and we are allowed to block
        return startSession(MsoyServer.author.authenticateSession(username, password), expireDays);
    }

    // from interface WebUserService
    public SessionData register (long clientVersion, String username, String password,
                                 final String displayName, int[] bdayvec, AccountInfo info,
                                 int expireDays, String inviteId)
        throws ServiceException
    {
        checkClientVersion(clientVersion, username);

        // check age restriction
        java.sql.Date birthday = ProfileRecord.fromDateVec(bdayvec);
        Calendar thirteenYearsAgo = Calendar.getInstance();
        thirteenYearsAgo.add(Calendar.YEAR, -13);
        if (birthday.compareTo(thirteenYearsAgo.getTime()) > 0) {
            log.log(Level.WARNING, "User submitted invalid birtdate [date=" + birthday + "].");
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        // check invitation validity
        boolean ignoreRestrict = false;
        InvitationRecord invite = null;
        if (inviteId != null) {
            try {
                invite = MsoyServer.memberRepo.inviteAvailable(inviteId);
                if (invite == null) {
                    throw new ServiceException(MsoyAuthCodes.INVITE_ALREADY_REDEEMED);
                }
                ignoreRestrict = true;
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Checking invite availability failed " +
                        "[inviteId=" + inviteId + "]", pe);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }
        }

        // validate display name length (this is enforced on the client)
        if (displayName.length() < Profile.MIN_DISPLAY_NAME_LENGTH ||
            displayName.length() > Profile.MAX_DISPLAY_NAME_LENGTH) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // we are running on a servlet thread at this point and can thus talk to the authenticator
        // directly as it is thread safe (and it blocks) and we are allowed to block
        final MemberRecord newAccount = MsoyServer.author.createAccount(
            username, password, displayName, ignoreRestrict, invite);

        // store the user's birthday and realname in their profile
        ProfileRecord prec = new ProfileRecord();
        prec.memberId = newAccount.memberId;
        prec.birthday = birthday;
        prec.realName = info.realName;
        try {
            MsoyServer.profileRepo.storeProfile(prec);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to create initial profile [prec=" + prec + "]", pe);
            // keep on keepin' on
        }

        // if we are responding to an invitation, wire that all up
        if (invite != null && invite.inviterId != 0) {
            try {
                MsoyServer.memberRepo.linkInvite(inviteId, newAccount);
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Linking invites failed [inviteId=" + inviteId +
                        ", memberId=" + newAccount.memberId + "]", pe);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            MemberNameRecord invname;
            try {
                invname = MsoyServer.memberRepo.loadMemberName(invite.inviterId);
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to lookup inviter name [inviteId=" + inviteId +
                        ", memberId=" + invite.inviterId + "]", pe);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            if (invname != null) {
                // send a notification email to the inviter that the friend has accepted
                final InvitationRecord finvite = invite;
                final MemberName inviter = invname.toMemberName();
                MsoyServer.omgr.postRunnable(new Runnable() {
                    public void run () {
                        // send them a whirled mail informing them of the acceptance
                        String subject = MsoyServer.msgMan.getBundle("server").get(
                            "m.invite_accepted_subject");
                        String body = MsoyServer.msgMan.getBundle("server").get(
                            "m.invite_accepted_body", finvite.inviteeEmail, displayName);
                        MsoyServer.mailMan.deliverMessage(
                            newAccount.memberId, finvite.inviterId, subject, body, null,
                            false, new ResultListener.NOOP<Void>());

                        // note the establishment of this friendship in the appropriate manager
                        // TODO: This is really spammy; in fact, when somebody accepts your invite
                        // TODO: you may get, in practice, four separate notifications:
                        // TODO:  - Foo is now your friend.
                        // TODO:  - Foo accepted your invitation.
                        // TODO:  - You have new mail.
                        // TODO:  - Foo is now online.
                        // TODO: We'd like to bring this down to one or possibly two lines, and
                        // TODO: will tackle this problem when notification has been peerified.
                        MsoyServer.friendMan.friendshipEstablished(
                            new MemberName(displayName, newAccount.memberId), inviter);

                        // and possibly send a runtime notification as well
                        MsoyServer.notifyMan.notifyInvitationAccepted(
                            finvite.inviterId, displayName, finvite.inviteeEmail);
                    }
                });
            }
        }

        return startSession(newAccount, expireDays);
    }

    // from interface WebUserService
    public SessionData validateSession (long clientVersion, String authtok, int expireDays)
        throws ServiceException
    {
        checkClientVersion(clientVersion, authtok);

        // refresh the token associated with their authentication session
        try {
            MemberRecord mrec = MsoyServer.memberRepo.refreshSession(authtok, expireDays);
            if (mrec == null) {
                return null;
            }

            WebCreds creds = mrec.toCreds(authtok);
            mapUser(creds.token, mrec);
            return loadSessionData(mrec, creds);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to refresh session [tok=" + authtok + "].", pe);
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
    public void sendForgotPasswordEmail (String email)
        throws ServiceException
    {
        try {
            String code = MsoyServer.author.generatePasswordResetCode(email);
            if (code == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }

            MemberRecord mrec = MsoyServer.memberRepo.loadMember(email);
            if (mrec == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }

            // create and send a forgot password email
            VelocityContext ctx = new VelocityContext();
            ctx.put("server_url", ServerConfig.getServerURL());
            ctx.put("email", mrec.accountName);
            ctx.put("memberId", mrec.memberId);
            ctx.put("code", code);
            try {
                MailSender.sendEmail(email, ServerConfig.getFromAddress(), "forgotPassword", ctx);
            } catch (Exception e) {
                throw new ServiceException(e.getMessage());
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to lookup account [email=" + email + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    // from interface WebUserService
    public void updateEmail (WebIdent ident, String newEmail)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        if (!MailUtil.isValidAddress(newEmail)) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        }

        try {
            MsoyServer.memberRepo.configureAccountName(mrec.memberId, newEmail);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to set email [who=" + mrec.memberId +
                    ", email=" + newEmail + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // let the authenticator know that we updated our account name
        MsoyServer.author.updateAccount(mrec.accountName, newEmail, null, null);
    }

    // from interface WebUserService
    public void updateEmailPrefs (WebIdent ident, boolean emailOnWhirledMail,
                                  boolean emailAnnouncements)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        // update their mail preferences if appropriate
        int oflags = mrec.flags;
        mrec.setFlag(MemberRecord.FLAG_NO_WHIRLED_MAIL_TO_EMAIL, !emailOnWhirledMail);
        mrec.setFlag(MemberRecord.FLAG_NO_ANNOUNCE_EMAIL, !emailAnnouncements);
        if (mrec.flags != oflags) {
            try {
                MsoyServer.memberRepo.configureFlags(mrec.memberId, mrec.flags);
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to update flags [who=" + mrec.memberId +
                        ", flags=" + mrec.flags + "].", pe);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }
    }

    // from interface WebUserService
    public void updatePassword (WebIdent ident, String newPassword)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);
        MsoyServer.author.updateAccount(mrec.accountName, null, null, newPassword);
    }

    // from interface WebUserService
    public boolean resetPassword (int memberId, String code, String newPassword)
        throws ServiceException
    {
        try {
            MemberRecord mrec = MsoyServer.memberRepo.loadMember(memberId);
            if (mrec == null) {
                log.info("No such member for password reset " + memberId + ".");
                return false;
            }

            if (!MsoyServer.author.validatePasswordResetCode(mrec.accountName, code)) {
                String actual = MsoyServer.author.generatePasswordResetCode(mrec.accountName);
                log.info("Code mismatch for password reset [id=" + memberId + ", code=" + code +
                         ", actual=" + actual + "].");
                return false;
            }

            MsoyServer.author.updateAccount(mrec.accountName, null, null, newPassword);
            return true;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to reset password [who=" + memberId +
                    ", code=" + code + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WebUserService
    public void configurePermaName (WebIdent ident, String permaName)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);
        if (mrec.permaName != null) {
            log.warning("Rejecting attempt to reassing permaname [who=" + mrec.accountName +
                        ", oname=" + mrec.permaName + ", nname=" + permaName + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (permaName.length() < MemberName.MINIMUM_PERMANAME_LENGTH ||
            permaName.length() > MemberName.MAXIMUM_PERMANAME_LENGTH ||
            !permaName.matches(PERMANAME_REGEX)) {
            throw new ServiceException("e.invalid_permaname");
        }

        try {
            MsoyServer.memberRepo.configurePermaName(mrec.memberId, permaName);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_PERMANAME);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to set permaname [who=" + mrec.memberId +
                    ", pname=" + permaName + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // let the authenticator know that we updated our permaname
        MsoyServer.author.updateAccount(mrec.accountName, null, permaName, null);
    }

    // from interface WebUserService
    public AccountInfo getAccountInfo (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            AccountInfo ainfo = new AccountInfo();
            ProfileRecord prec = MsoyServer.profileRepo.loadProfile(mrec.memberId);
            if (prec != null) {
                ainfo.realName = prec.realName;
            }
            ainfo.emailWhirledMail = !mrec.isSet(MemberRecord.FLAG_NO_WHIRLED_MAIL_TO_EMAIL);
            ainfo.emailAnnouncements = !mrec.isSet(MemberRecord.FLAG_NO_ANNOUNCE_EMAIL);
            return ainfo;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to fetch account info [who=" + mrec.memberId +
                "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface WebUserService
    public void updateAccountInfo (WebIdent ident, AccountInfo info)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            ProfileRecord prec = MsoyServer.profileRepo.loadProfile(mrec.memberId);
            prec.realName = info.realName;
            MsoyServer.profileRepo.storeProfile(prec);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update user account info [who=" + mrec.memberId +
                "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected void checkClientVersion (long clientVersion, String who)
        throws ServiceException
    {
        if (clientVersion != DeploymentConfig.version) {
            log.info("Refusing wrong version [who=" + who + ", cvers=" + clientVersion +
                     ", svers=" + DeploymentConfig.version + "].");
            throw new ServiceException(MsoyAuthCodes.VERSION_MISMATCH);
        }
    }

    protected SessionData startSession (MemberRecord mrec, int expireDays)
        throws ServiceException
    {
        try {
            // if they made it through that gauntlet, create or update their session token
            WebCreds creds = mrec.toCreds(
                MsoyServer.memberRepo.startOrJoinSession(mrec.memberId, expireDays));
            mapUser(creds.token, mrec);
            return loadSessionData(mrec, creds);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to start session [for=" + mrec.accountName + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    protected SessionData loadSessionData (MemberRecord mrec, WebCreds creds)
    {
        SessionData data = new SessionData();
        data.creds = creds;

        // fill in their flow, gold and level
        data.flow = mrec.flow;
        // data.gold = TODO
        data.level = mrec.level;

        // load up their new message count
        try {
            data.newMailCount = MsoyServer.mailMan.getRepository().getMessageCount(
                mrec.memberId, MailFolder.INBOX_FOLDER_ID).right;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load new mail count [id=" + mrec.memberId + "].", pe);
        }

        // load up their room count
        try {
            data.roomCount = MsoyServer.sceneRepo.getRoomCount(mrec.memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load room count [id=" + mrec.memberId + "].", pe);
        }

        return data;
    }

    /** The regular expression defining valid permanames. */
    protected static final String PERMANAME_REGEX = "^[A-Za-z][_A-Za-z0-9]*$";
}
