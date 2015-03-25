//
// $Id$

package com.threerings.msoy.person.server;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

// import octazen.addressbook.AddressBookAuthenticationException;
// import octazen.addressbook.AddressBookException;
// import octazen.addressbook.Contact;
// import octazen.addressbook.SimpleAddressBookImporter;
// import octazen.addressbook.UnexpectedFormatException;
// import octazen.http.UserInputRequiredException;

import com.samskivert.net.MailUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.avrg.server.persist.AVRGameRepository;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.server.GameUtil;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameCookieRepository;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.mail.gwt.GameInvitePayload;
import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.person.gwt.InvitationResults;
import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.gwt.ProfileCodes;
import com.threerings.msoy.person.server.persist.GameInvitationRecord;
import com.threerings.msoy.person.server.persist.InviteRepository;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.spam.server.persist.SpamRepository;
import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link InviteService}.
 */
public class InviteServlet extends MsoyServiceServlet
    implements InviteService
{
    // from InviteService
    public List<EmailContact> getWebMailAddresses (String email, String password)
        throws ServiceException
    {
        throw new ServiceException(ProfileCodes.E_INTERNAL_ERROR);
        // MemberRecord memrec = requireAuthedUser();

        // // don't let someone attempt more than 5 imports in a 5 minute period
        // long now = System.currentTimeMillis();
        // if (now > _webmailCleared + WEB_ACCESS_CLEAR_INTERVAL) {
        //     _webmailAccess.clear();
        //     _webmailCleared = now;
        // }
        // if (_webmailAccess.increment(memrec.memberId, 1) > MAX_WEB_ACCESS_ATTEMPTS) {
        //     throw new ServiceException(ProfileCodes.E_MAX_WEBMAIL_ATTEMPTS);
        // }

        // try {
        //     List<Contact> contacts = SimpleAddressBookImporter.fetchContacts(email, password);
        //     List<EmailContact> results = Lists.newArrayList();

        //     for (Contact contact : contacts) {
        //         // don't invite the account owner
        //         if (email.equals(contact.getEmail())) {
        //             continue;
        //         }
        //         EmailContact ec = new EmailContact();
        //         ec.name = contact.getName();
        //         ec.email = contact.getEmail();
        //         MemberRecord member = _memberRepo.loadMember(ec.email);
        //         if (member != null) {
        //             if (member.memberId == memrec.memberId) {
        //                 // skip self invites
        //                 continue;
        //             }
        //             ec.friendship = _memberRepo.getFriendship(memrec.memberId, member.memberId);
        //             ec.mname = member.getName();
        //         }
        //         results.add(ec);
        //     }
        //     return results;

        // } catch (AddressBookAuthenticationException e) {
        //     throw new ServiceException(ProfileCodes.E_BAD_USERNAME_PASS);
        // } catch (UnexpectedFormatException e) {
        //     log.warning("getWebMailAddresses failed", "email", email, e);
        //     throw new ServiceException(ProfileCodes.E_INTERNAL_ERROR);
        // } catch (AddressBookException e) {
        //     throw new ServiceException(ProfileCodes.E_UNSUPPORTED_WEBMAIL);
        // } catch (UserInputRequiredException e) {
        //     throw new ServiceException(ProfileCodes.E_USER_INPUT_REQUIRED);
        // } catch (Exception e) {
        //     log.warning("getWebMailAddresses failed", "who", memrec.who(), "email", email, e);
        //     throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        // }
    }

    // from InviteService
    public InvitationResults sendInvites (
        List<EmailContact> addresses, String fromName, String subject, String customMessage,
        boolean anonymous)
        throws ServiceException
    {
        MemberRecord mrec = anonymous ? requireAdminUser() : requireAuthedUser();

        log.info("Member inviting", "addresses", addresses.size(), "subject", subject);

        InvitationResults ir = new InvitationResults();
        ir.results = new String[addresses.size()];
        ir.names = new MemberName[addresses.size()];
        List<Invitation> penders = Lists.newArrayList();
        for (int ii = 0; ii < addresses.size(); ii++) {
            EmailContact contact = addresses.get(ii);
            if (contact.name.equals(contact.email)) {
                contact.name = null;
            }
            try {
                penders.add(sendInvite(anonymous ? null : mrec, contact.email, contact.name,
                            fromName, subject, customMessage));
            } catch (NameServiceException nse) {
                ir.results[ii] = nse.getMessage();
                ir.names[ii] = nse.name;
            } catch (ServiceException se) {
                ir.results[ii] = se.getMessage();
            }
        }
        ir.pendingInvitations = penders;
        return ir;
    }

    // from InviteService
    public InvitationResults sendGameInvites (
        List<EmailContact> addresses, int gameId, String from, String url, String customMessage)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        if (GameUtil.isDevelopmentVersion(gameId)) {
            // TODO: this is not really an end-user exception and could arguably be done in the
            // client. At least let the creator know something is wrong for now
            throw new ServiceException("e.game_not_listed");
        }

        GameInfoRecord gdr = _mgameRepo.loadGame(gameId);
        if (gdr == null) {
            throw new ServiceException(MsoyCodes.INTERNAL_ERROR);
        }
        if (gdr.genre == GameGenre.HIDDEN) {
            throw new ServiceException("e.game_not_published"); // TODO
        }

        InvitationResults ir = new InvitationResults();
        ir.results = new String[addresses.size()];
        ir.names = new MemberName[addresses.size()];
        for (int ii = 0; ii < addresses.size(); ii++) {
            EmailContact contact = addresses.get(ii);
            if (contact.name.equals(contact.email)) {
                contact.name = null;
            }
            try {
                sendGameInvite(gdr.name, gameId, mrec, contact.email, contact.name, from,
                               url, customMessage);
            } catch (ServiceException se) {
                ir.results[ii] = se.getMessage();
            }
        }
        return ir;
    }

    // from InviteService
    public void sendWhirledMailGameInvites (Set<Integer> recipientIds, int gameId, String subject,
        String body, String args)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        // send the invitation, respecting mute lists
        GameInvitePayload payload = new GameInvitePayload(args);
        _mailLogic.startBulkConversation(mrec, recipientIds, subject, body, payload, true);
        // TODO: this is really not useful, is it?
        // _eventLog.gameInviteSent(gameId, mrec.memberId, recip.memberId + "", "whirled");
    }

    public int getHomeSceneId ()
        throws ServiceException
    {
        return requireAuthedUser().homeSceneId;
    }

    // from InviteService
    public List<MemberCard> getFriends (int gameId, int count)
        throws ServiceException
    {
        // load friends
        MemberRecord memrec = requireAuthedUser();
        Set<Integer> friendIds = _memberRepo.loadFriendIds(memrec.memberId);

        // if requested, remove friends that have already saved some progress in the game
        GameInfoRecord grec;
        if (gameId != 0 && (grec = _mgameRepo.loadGame(gameId)) != null) {
            if (grec.isAVRG) {
                friendIds.removeAll(_avrGameRepo.getPropertiedMembers(gameId, friendIds));
            } else {
                friendIds.removeAll(_gameCookieRepo.getCookiedPlayers(gameId, friendIds));
            }
        }

        // resolve to member cards
        return MemberCardRecord.toMemberCards(
            _memberRepo.loadMemberCards(friendIds, 0, count, true));
    }

    /**
     * Helper function for {@link #sendInvites}.
     */
    protected Invitation sendInvite (
        MemberRecord inviter, String email, String toName, String fromName, String subject,
        String customMessage)
        throws ServiceException
    {
        // make sure this address is valid
        if (!MailUtil.isValidAddress(email)) {
            throw new ServiceException(InvitationResults.INVALID_EMAIL);
        }

        // make sure this address isn't already registered
        MemberRecord invitee = _memberRepo.loadMember(email);
        if (invitee != null) {
            Friendship fr = _memberRepo.getFriendship(inviter.memberId, invitee.memberId);
            if (fr == Friendship.FRIENDS) {
                throw new ServiceException(InvitationResults.ALREADY_FRIEND);
            } else if (fr == Friendship.INVITED) {
                throw new ServiceException(InvitationResults.ALREADY_FRIEND_INV);
            }
            throw new NameServiceException(
                InvitationResults.ALREADY_REGISTERED, invitee.getName());
        }

        // make sure this address isn't on the opt-out list
        if (_spamRepo.hasOptedOut(email)) {
            throw new ServiceException(InvitationResults.OPTED_OUT);
        }

        // make sure this user hasn't already invited this address
        int inviterId = (inviter == null) ? 0 : inviter.memberId;
        if (_inviteRepo.loadInviteByEmail(email, inviterId) != null) {
            throw new ServiceException(InvitationResults.ALREADY_INVITED);
        }

        String inviteId = _inviteRepo.generateInviteId();

        // create and send the invitation email
        MailSender.Parameters params = new MailSender.Parameters();
        if (inviter != null) {
            params.set("friend", fromName);
            params.set("email", inviter.accountName);
        }
        if (!StringUtil.isBlank(toName)) {
            params.set("name", toName);
        }
        if (!StringUtil.isBlank(customMessage)) {
            params.set("custom_message", customMessage);
        }
        if (!StringUtil.isBlank(subject)) {
            params.set("custom_subject", subject);
        }
        params.set("invite_id", inviteId);
        params.set("server_url", ServerConfig.getServerURL());

        String from = (inviter == null) ? ServerConfig.getFromAddress() : inviter.accountName;
        _mailer.sendTemplateEmail(MailSender.By.HUMAN, email, from, "memberInvite", params);

        // record the invite and that we sent it
        _inviteRepo.addInvite(email, inviterId, inviteId);
        _eventLog.inviteSent(inviteId, inviterId, email);

        Invitation invite = new Invitation();
        invite.inviteId = inviteId;
        invite.inviteeEmail = email;
        // invite.inviter left blank on purpose
        return invite;
    }

    /**
     * Helper function for {@link #sendGameInvites}.
     */
    protected void sendGameInvite (String gameName, int gameId, MemberRecord inviter, String email,
        String toName, String fromName, String url, String customMessage)
        throws ServiceException
    {
        // make sure this address is valid
        if (!MailUtil.isValidAddress(email)) {
            throw new ServiceException(InvitationResults.INVALID_EMAIL);
        }

        // TODO: if a user is trying to invite another registered user, we should just send a
        // whirled mail message instead
        MemberRecord invitee = _memberRepo.loadMember(email);
        if (_memberRepo.loadMember(email) != null) {
            throw new NameServiceException(
                InvitationResults.ALREADY_REGISTERED, invitee.getName());
        }

        // make sure this address isn't on the opt-out list
        if (_spamRepo.hasOptedOut(email)) {
            throw new ServiceException(InvitationResults.OPTED_OUT);
        }

        // we are fine to send multiple game invites, but we must provide an invite id so the
        // recipient can opt out securely
        String inviteId;
        GameInvitationRecord invite = _inviteRepo.loadGameInviteByEmail(email);
        if (invite != null) {
            inviteId = invite.inviteId;
        } else {
            inviteId = _inviteRepo.generateGameInviteId();
        }

        // create and send the invitation email
        MailSender.Parameters params = new MailSender.Parameters();
        if (inviter != null) {
            params.set("friend", fromName);
            params.set("email", inviter.accountName);
        }
        if (!StringUtil.isBlank(toName)) {
            params.set("name", toName);
        }
        if (!StringUtil.isBlank(customMessage)) {
            params.set("custom_message", customMessage);
        }
        params.set("server_url", ServerConfig.getServerURL());
        params.set("url", url);
        params.set("game", gameName);
        params.set("invite_id", inviteId);

        String from = inviter.accountName;
        _mailer.sendTemplateEmail(MailSender.By.HUMAN, email, from, "gameInvite", params);

        // record the invite and that we sent it
        _inviteRepo.addGameInvite(email, inviteId);
        _eventLog.gameInviteSent(gameId, inviter.memberId, email, "email");
    }

    protected class NameServiceException extends ServiceException
    {
        public MemberName name;

        public NameServiceException (String message, MemberName name)
        {
            super(message);
            this.name = name;
        }
    }

    protected IntIntMap _webmailAccess = new IntIntMap();
    protected long _webmailCleared = System.currentTimeMillis();

    @Inject protected AVRGameRepository _avrGameRepo;
    @Inject protected InviteRepository _inviteRepo;
    @Inject protected MailLogic _mailLogic;
    @Inject protected MailSender _mailer;
    @Inject protected MsoyGameCookieRepository _gameCookieRepo;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected SpamRepository _spamRepo;

    protected static final int MAX_WEB_ACCESS_ATTEMPTS = 5;
    protected static final long WEB_ACCESS_CLEAR_INTERVAL = 5L * 60 * 1000;
}
