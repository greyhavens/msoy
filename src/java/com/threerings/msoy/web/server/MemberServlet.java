//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;
import com.samskivert.net.MailUtil;
import org.apache.velocity.VelocityContext;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.FriendManager;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.person.util.FeedMessageType;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.data.EmailContact;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link MemberService}.
 */
public class MemberServlet extends MsoyServiceServlet
    implements MemberService
{
    // from interface MemberService
    public MemberCard getMemberCard (int memberId)
        throws ServiceException
    {
        try {
            for (MemberCardRecord mcr : MsoyServer.memberRepo.loadMemberCards(
                     Collections.singleton(memberId))) {
                return mcr.toMemberCard();
            }
            return null;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getMemberCard failed [id=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public boolean getFriendStatus (WebIdent ident, final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        try {
            return MsoyServer.memberRepo.getFriendStatus(memrec.memberId, memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getFriendStatus failed [for=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void addFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        try {
            MemberName friendName = MsoyServer.memberRepo.noteFriendship(memrec.memberId, friendId);
            if (friendName == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }
            MsoyServer.feedRepo.publishMemberMessage(
                memrec.memberId, FeedMessageType.FRIEND_ADDED_FRIEND,
                friendName.toString() + "\t" + String.valueOf(friendId) );
            FriendManager.friendshipEstablished(memrec.getName(), friendName);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "addFriend failed [for=" + memrec.memberId +
                    ", friendId=" + friendId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void removeFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        try {
            MsoyServer.memberRepo.clearFriendship(memrec.memberId, friendId);
            FriendManager.friendshipCleared(memrec.memberId, friendId);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "removeFriend failed [for=" + memrec.memberId +
                    ", friendId=" + friendId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MemberService
    public List loadInventory (WebIdent ident, byte type, int suiteId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        // convert the string they supplied to an item enumeration
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + ident + ", type=" + type + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
        try {
            List<Item> items = Lists.newArrayList();
            for (ItemRecord record : repo.loadOriginalItems(memrec.memberId, suiteId)) {
                items.add(record.toItem());
            }
            for (ItemRecord record : repo.loadClonedItems(memrec.memberId, suiteId)) {
                items.add(record.toItem());
            }

            // when Item becomes a type-safe Comparable this Comparator can go away
            Collections.sort(items, new Comparator<Item>() {
                public int compare (Item one, Item two) {
                    return one.compareTo(two);
                }
            });
            return items;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "loadInventory failed [for=" + memrec.memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public MemberInvites getInvitationsStatus (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            MemberInvites result = new MemberInvites();
            result.availableInvitations = MsoyServer.memberRepo.getInvitesGranted(mrec.memberId);
            List<Invitation> pending = Lists.newArrayList();
            for (InvitationRecord iRec : MsoyServer.memberRepo.loadPendingInvites(mrec.memberId)) {
                // we issued these invites so we are the inviter
                pending.add(iRec.toInvitation(mrec.getName()));
            }
            result.pendingInvitations = pending;
            result.serverUrl = ServerConfig.getServerURL() + "/#invite-";
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitationsStatus failed [id=" + mrec.memberId +"]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public InvitationResults sendInvites (WebIdent ident, List addresses, String fromName,
                                          String customMessage, boolean anonymous)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        // if they're requesting anonymous invites and are not an admin, rejecto!
        if (anonymous && !mrec.isAdmin()) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

// TODO: nix this when we stop caring about retaining the potential to limit growth
//         try {
//             // make sure this user still has available invites; we already check this value in GWT
//             // land, and deal with it sensibly there
//             int availInvites = MsoyServer.memberRepo.getInvitesGranted(mrec.memberId);
//             if (availInvites < addresses.size()) {
//                 log.warning("Member requested to grant more invites than they have " +
//                             "[who=" + mrec.who() + ", tried=" + addresses.size() +
//                             ", have=" + availInvites + "].");
//                 throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//             }

//         } catch (PersistenceException pe) {
//             log.log(Level.WARNING, "getInvitesGranted failed [id=" + mrec.memberId +"]", pe);
//             throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//         }

        InvitationResults ir = new InvitationResults();
        ir.results = new String[addresses.size()];
        ir.names = new MemberName[addresses.size()];
        List<Invitation> penders = Lists.newArrayList();
        for (int ii = 0; ii < addresses.size(); ii++) {
            EmailContact contact = (EmailContact)addresses.get(ii);
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

    // from MemberService
    public Invitation getInvitation (String inviteId, boolean viewing)
        throws ServiceException
    {
        try {
            InvitationRecord invRec = MsoyServer.memberRepo.loadInvite(inviteId, viewing);
            if (invRec == null) {
                return null;
            }

            // if we're viewing this invite, log that it was viewed
            if (viewing) {
                _eventLog.inviteViewed(inviteId);
            }

            MemberName inviter = null;
            if (invRec.inviterId > 0) {
                inviter = MsoyServer.memberRepo.loadMemberName(invRec.inviterId);
            }
            return invRec.toInvitation(inviter);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitation failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void removeInvitation (WebIdent ident, String inviteId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            InvitationRecord invRec = MsoyServer.memberRepo.loadInvite(inviteId, false);
            if (invRec == null) {
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            if (invRec.inviterId != mrec.memberId || invRec.inviteeId != 0) {
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
            MsoyServer.memberRepo.deleteInvite(inviteId);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "removeInvitation failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void optOut (String inviteId)
        throws ServiceException
    {
        try {
            if (MsoyServer.memberRepo.inviteAvailable(inviteId) != null) {
                MsoyServer.memberRepo.optOutInvite(inviteId);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "optOut failed [inviteId=" + inviteId + "]", pe);
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
            MemberRecord invitee = MsoyServer.memberRepo.loadMember(email);
            if (invitee != null) {
                if (MsoyServer.memberRepo.getFriendStatus(inviter.memberId, invitee.memberId)) {
                    throw new ServiceException(InvitationResults.ALREADY_FRIEND);
                }
                throw new NameServiceException(
                        InvitationResults.ALREADY_REGISTERED, invitee.getName());
            }

            // make sure this address isn't on the opt-out list
            if (MsoyServer.memberRepo.hasOptedOut(email)) {
                throw new ServiceException(InvitationResults.OPTED_OUT);
            }

            // make sure this user hasn't already invited this address
            int inviterId = (inviter == null) ? 0 : inviter.memberId;
            if (MsoyServer.memberRepo.loadInvite(email, inviterId) != null) {
                throw new ServiceException(InvitationResults.ALREADY_INVITED);
            }

            String inviteId = MsoyServer.memberRepo.generateInviteId();

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
            MsoyServer.memberRepo.addInvite(email, inviterId, inviteId);
            _eventLog.inviteSent(inviteId, inviterId, email);

            Invitation invite = new Invitation();
            invite.inviteId = inviteId;
            invite.inviteeEmail = email;
            // invite.inviter left blank on purpose
            return invite;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "sendInvite failed [inviter=" + inviter.who() +
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
}
