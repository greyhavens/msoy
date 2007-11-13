//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;
import org.apache.velocity.VelocityContext;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.data.MsoyAuthCodes;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link MemberService}.
 */
public class MemberServlet extends MsoyServiceServlet
    implements MemberService
{
    // from MemberService
    public boolean getFriendStatus (WebIdent ident, final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        try {
            return MsoyServer.memberRepo.getFriendStatus(memrec.memberId, memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getFriendStatus failed [for=" + memberId + "].", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void addFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        try {
            final MemberName friendName =
                MsoyServer.memberRepo.noteFriendship(memrec.memberId, friendId);
            if (friendName == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }
            MsoyServer.feedRepo.publishMemberMessage(
                    memrec.memberId, FeedMessageType.FRIEND_ADDED_FRIEND,
                    friendName.toString() + "\t" + String.valueOf(friendId) );
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.friendMan.friendshipEstablished(memrec.getName(), friendName);
                }
            });

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "addFriend failed [for=" + memrec.memberId +
                    ", friendId=" + friendId + "].", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void removeFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        try {
            MsoyServer.memberRepo.clearFriendship(memrec.memberId, friendId);
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    MsoyServer.friendMan.friendshipCleared(memrec.memberId, friendId);
                }
            });

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "removeFriend failed [for=" + memrec.memberId +
                    ", friendId=" + friendId + "].", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
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
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        ItemRepository<ItemRecord, ?, ?, ?> repo = MsoyServer.itemMan.getRepository(type);
        try {
            ArrayList<Item> items = new ArrayList<Item>();
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
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
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
            ArrayList<Invitation> pending = new ArrayList<Invitation>();
            for (InvitationRecord iRec : MsoyServer.memberRepo.loadPendingInvites(mrec.memberId)) {
                // we issued these invites so we are the inviter
                pending.add(iRec.toInvitation(mrec.getName()));
            }
            result.pendingInvitations = pending;
            result.serverUrl = ServerConfig.getServerURL() + "/#invite-";
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitationsStatus failed [id=" + mrec.memberId +"]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public InvitationResults sendInvites (WebIdent ident, List addresses, String customMessage,
                                          boolean anonymous)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            // make sure this user still has available invites; we already check this value in GWT
            // land, and deal with it sensibly there
            if (MsoyServer.memberRepo.getInvitesGranted(mrec.memberId) < addresses.size()) {
                throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
            }

            // if they're requesting anonymous invites and are not an admin, rejecto!
            if (anonymous && !mrec.isAdmin()) {
                throw new ServiceException(InvocationCodes.E_ACCESS_DENIED);
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitesGranted failed [id=" + mrec.memberId +"]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }

        InvitationResults ir = new InvitationResults();
        ir.results = new String[addresses.size()];
        for (int ii = 0; ii < addresses.size(); ii++) {
            String email = (String)addresses.get(ii);
            ir.results[ii] = sendInvite(anonymous ? null : mrec, email, customMessage);
        }
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
            MemberName inviter = null;
            if (invRec.inviterId > 0) {
                MemberNameRecord mnr = MsoyServer.memberRepo.loadMemberName(invRec.inviterId);
                inviter = mnr.toMemberName();
            }
            return invRec.toInvitation(inviter);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitation failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void optOut (Invitation invite)
        throws ServiceException
    {
        try {
            if (!MsoyServer.memberRepo.inviteAvailable(invite.inviteId)) {
                throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
            }
            MsoyServer.memberRepo.optOutInvite(invite);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "optOut failed [inviteId=" + invite.inviteId + "]", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Helper function for {@link #sendInvites}.
     */
    protected String sendInvite (MemberRecord inviter, String email, String customMessage)
    {
        try {
            // make sure this address is valid
            if (!MailUtil.isValidAddress(email)) {
                return InvitationResults.INVALID_EMAIL;
            }

            // make sure this address isn't already registered
            if (MsoyServer.memberRepo.loadMember(email) != null) {
                return InvitationResults.ALREADY_REGISTERED;
            }

            // make sure this address isn't on the opt-out list
            if (MsoyServer.memberRepo.hasOptedOut(email)) {
                return InvitationResults.OPTED_OUT;
            }

            // make sure this user hasn't already invited this address
            if (inviter != null) {
                if (MsoyServer.memberRepo.loadInvite(email, inviter.memberId) != null) {
                    return InvitationResults.ALREADY_INVITED;
                }
            }

            String inviteId = MsoyServer.memberRepo.generateInviteId();

            // create and send the invitation
            VelocityContext ctx = new VelocityContext();
            if (inviter != null) {
                ctx.put("friend", inviter.name);
                ctx.put("email", inviter.accountName);
            }
            ctx.put("custom_message", customMessage);
            ctx.put("invite_id", inviteId);
            ctx.put("server_url", ServerConfig.getServerURL());

            String from = (inviter == null) ? ServerConfig.getFromAddress() : inviter.accountName;
            try {
                MailSender.sendEmail(email, from, "memberInvite", ctx);
            } catch (Exception e) {
                return e.getMessage();
            }

            int inviterId = (inviter == null) ? 0 : inviter.memberId;
            MsoyServer.memberRepo.addInvite(email, inviterId, inviteId);
            return InvitationResults.SUCCESS;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "sendInvite failed [inviter=" + inviter.who() +
                    ", email=" + email + "].", pe);
            return ServiceException.INTERNAL_ERROR;
        }
    }
}
