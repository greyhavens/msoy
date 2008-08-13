//
// $Id$

package com.threerings.msoy.person.server;

import java.io.IOException;
import java.util.List;

import octazen.addressbook.AddressBookAuthenticationException;
import octazen.addressbook.AddressBookException;
import octazen.addressbook.Contact;
import octazen.addressbook.SimpleAddressBookImporter;
import octazen.addressbook.UnexpectedFormatException;
import octazen.http.HttpException;
import octazen.http.UserInputRequiredException;

import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.data.EmailContact;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.person.gwt.InvitationResults;
import com.threerings.msoy.person.gwt.MemberInvites;
import com.threerings.msoy.person.gwt.ProfileCodes;
import com.threerings.msoy.person.gwt.InviteService;

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
        MemberRecord memrec = requireAuthedUser();

        try {
            // don't let someone attempt more than 5 imports in a 5 minute period
            long now = System.currentTimeMillis();
            if (now > _waCleared + WEB_ACCESS_CLEAR_INTERVAL) {
                _webmailAccess.clear();
                _waCleared = now;
            }
            if (_webmailAccess.increment(memrec.memberId, 1) > MAX_WEB_ACCESS_ATTEMPTS) {
                throw new ServiceException(ProfileCodes.E_MAX_WEBMAIL_ATTEMPTS);
            }
            List<Contact> contacts = SimpleAddressBookImporter.fetchContacts(email, password);
            List<EmailContact> results = Lists.newArrayList();

            for (Contact contact : contacts) {
                EmailContact ec = new EmailContact();
                ec.name = contact.getName();
                ec.email = contact.getEmail();
                MemberRecord member = _memberRepo.loadMember(ec.email);
                if (member != null) {
                    if (_memberRepo.getFriendStatus(memrec.memberId, member.memberId)) {
                        // just skip people who are already friends
                        continue;
                    }
                    ec.mname = member.getName();
                }
                results.add(ec);
            }

            return results;

        } catch (AddressBookAuthenticationException e) {
            throw new ServiceException(ProfileCodes.E_BAD_USERNAME_PASS);
        } catch (UnexpectedFormatException e) {
            log.warning("getWebMailAddresses failed [email=" + email + "].", e);
            throw new ServiceException(ProfileCodes.E_INTERNAL_ERROR);
        } catch (AddressBookException e) {
            throw new ServiceException(ProfileCodes.E_UNSUPPORTED_WEBMAIL);
        } catch (UserInputRequiredException e) {
            throw new ServiceException(ProfileCodes.E_USER_INPUT_REQUIRED);
        } catch (IOException e) {
            log.warning("getWebMailAddresses failed [email=" + email + "].", e);
            throw new ServiceException(ProfileCodes.E_INTERNAL_ERROR);
        } catch (HttpException e) {
            log.warning("getWebMailAddresses failed [email=" + email + "].", e);
            throw new ServiceException(ProfileCodes.E_INTERNAL_ERROR);
        } catch (PersistenceException pe) {
            log.warning("getWebMailAddresses failed [who=" + memrec.who() +
                    ", email=" + email + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from InviteService
    public MemberInvites getInvitationsStatus ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        try {
            MemberInvites result = new MemberInvites();
            result.availableInvitations = _memberRepo.getInvitesGranted(mrec.memberId);
            List<Invitation> pending = Lists.newArrayList();
            for (InvitationRecord iRec : _memberRepo.loadPendingInvites(mrec.memberId)) {
                // we issued these invites so we are the inviter
                pending.add(iRec.toInvitation(mrec.getName()));
            }
            result.pendingInvitations = pending;
            result.serverUrl = ServerConfig.getServerURL() + "#invite-";
            return result;

        } catch (PersistenceException pe) {
            log.warning("getInvitationsStatus failed [id=" + mrec.memberId +"]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from InviteService
    public InvitationResults sendInvites (List<EmailContact> addresses, String fromName,
                                          String customMessage, boolean anonymous)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // if they're requesting anonymous invites and are not an admin, rejecto!
        if (anonymous && !mrec.isAdmin()) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

// TODO: nix this when we stop caring about retaining the potential to limit growth
//         try {
//             // make sure this user still has available invites; we already check this value in GWT
//             // land, and deal with it sensibly there
//             int availInvites = _memberRepo.getInvitesGranted(mrec.memberId);
//             if (availInvites < addresses.size()) {
//                 log.warning("Member requested to grant more invites than they have " +
//                             "[who=" + mrec.who() + ", tried=" + addresses.size() +
//                             ", have=" + availInvites + "].");
//                 throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//             }

//         } catch (PersistenceException pe) {
//             log.warning("getInvitesGranted failed [id=" + mrec.memberId +"]", pe);
//             throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//         }

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
                            fromName, customMessage));
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
    public void removeInvitation (String inviteId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        try {
            InvitationRecord invRec = _memberRepo.loadInvite(inviteId, false);
            if (invRec == null) {
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            if (invRec.inviterId != mrec.memberId || invRec.inviteeId != 0) {
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
            _memberRepo.deleteInvite(inviteId);

        } catch (PersistenceException pe) {
            log.warning("removeInvitation failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Helper function for {@link #sendInvites}.
     */
    protected Invitation sendInvite (MemberRecord inviter, String email, String toName,
                                     String fromName, String customMessage)
        throws ServiceException
    {
        try {
            // make sure this address is valid
            if (!MailUtil.isValidAddress(email)) {
                throw new ServiceException(InvitationResults.INVALID_EMAIL);
            }

            // make sure this address isn't already registered
            MemberRecord invitee = _memberRepo.loadMember(email);
            if (invitee != null) {
                if (_memberRepo.getFriendStatus(inviter.memberId, invitee.memberId)) {
                    throw new ServiceException(InvitationResults.ALREADY_FRIEND);
                }
                throw new NameServiceException(
                        InvitationResults.ALREADY_REGISTERED, invitee.getName());
            }

            // make sure this address isn't on the opt-out list
            if (_memberRepo.hasOptedOut(email)) {
                throw new ServiceException(InvitationResults.OPTED_OUT);
            }

            // make sure this user hasn't already invited this address
            int inviterId = (inviter == null) ? 0 : inviter.memberId;
            if (_memberRepo.loadInvite(email, inviterId) != null) {
                throw new ServiceException(InvitationResults.ALREADY_INVITED);
            }

            String inviteId = _memberRepo.generateInviteId();

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
            params.set("invite_id", inviteId);
            params.set("server_url", ServerConfig.getServerURL());

            String from = (inviter == null) ? ServerConfig.getFromAddress() : inviter.accountName;
            String result = MailSender.sendEmail(email, from, "memberInvite", params);
            if (result != null) {
                throw new ServiceException(result);
            }

            // record the invite and that we sent it
            _memberRepo.addInvite(email, inviterId, inviteId);
            _eventLog.inviteSent(inviteId, inviterId, email);

            Invitation invite = new Invitation();
            invite.inviteId = inviteId;
            invite.inviteeEmail = email;
            // invite.inviter left blank on purpose
            return invite;

        } catch (PersistenceException pe) {
            log.warning("sendInvite failed [inviter=" + inviter.who() +
                    ", email=" + email + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
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
    protected long _waCleared = System.currentTimeMillis();

    protected static final int MAX_WEB_ACCESS_ATTEMPTS = 5;
    protected static final long WEB_ACCESS_CLEAR_INTERVAL = 5L * 60 * 1000;
}
