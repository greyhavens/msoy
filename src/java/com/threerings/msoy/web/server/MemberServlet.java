//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;
import com.samskivert.net.MailUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.server.FriendManager;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.person.server.persist.ProfileRepository;

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
            for (MemberCardRecord mcr : _memberRepo.loadMemberCards(
                     Collections.singleton(memberId))) {
                return mcr.toMemberCard();
            }
            return null;

        } catch (PersistenceException pe) {
            log.warning("getMemberCard failed [id=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public boolean getFriendStatus (WebIdent ident, final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        try {
            return _memberRepo.getFriendStatus(memrec.memberId, memberId);
        } catch (PersistenceException pe) {
            log.warning("getFriendStatus failed [for=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void addFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        _memberLogic.establishFriendship(memrec, friendId);
    }

    // from MemberService
    public void removeFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        _memberLogic.clearFriendship(memrec.memberId, friendId);
    }

    // from interface MemberService
    public List<Item> loadInventory (WebIdent ident, byte type, int suiteId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);

        // convert the string they supplied to an item enumeration
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + ident + ", type=" + type + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRepository<ItemRecord, ?, ?, ?> repo = _itemMan.getRepository(type);
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
            log.warning("loadInventory failed [for=" + memrec.memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public MemberInvites getInvitationsStatus (WebIdent ident)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            MemberInvites result = new MemberInvites();
            result.availableInvitations = _memberRepo.getInvitesGranted(mrec.memberId);
            List<Invitation> pending = Lists.newArrayList();
            for (InvitationRecord iRec : _memberRepo.loadPendingInvites(mrec.memberId)) {
                // we issued these invites so we are the inviter
                pending.add(iRec.toInvitation(mrec.getName()));
            }
            result.pendingInvitations = pending;
            result.serverUrl = ServerConfig.getServerURL() + "/#invite-";
            return result;

        } catch (PersistenceException pe) {
            log.warning("getInvitationsStatus failed [id=" + mrec.memberId +"]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public InvitationResults sendInvites (WebIdent ident, List addresses, String fromName,
                                          String customMessage, boolean anonymous)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

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
            InvitationRecord invRec = _memberRepo.loadInvite(inviteId, viewing);
            if (invRec == null) {
                return null;
            }

            // if we're viewing this invite, log that it was viewed
            if (viewing) {
                _eventLog.inviteViewed(inviteId);
            }

            MemberName inviter = null;
            if (invRec.inviterId > 0) {
                inviter = _memberRepo.loadMemberName(invRec.inviterId);
            }
            return invRec.toInvitation(inviter);

        } catch (PersistenceException pe) {
            log.warning("getInvitation failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void removeInvitation (WebIdent ident, String inviteId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

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

    // from MemberService
    public void optOut (String inviteId)
        throws ServiceException
    {
        try {
            if (_memberRepo.inviteAvailable(inviteId) != null) {
                _memberRepo.optOutInvite(inviteId);
            }
        } catch (PersistenceException pe) {
            log.warning("optOut failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public List<MemberCard> getLeaderList ()
        throws ServiceException
    {
        try {
            // locate the members that match the supplied search
            IntSet mids = new ArrayIntSet();
            mids.addAll(_memberRepo.getLeadingMembers(MAX_LEADER_MATCHES));

            // resolve cards for these members
            List<MemberCard> results = _mhelper.resolveMemberCards(mids, false, null);
            Collections.sort(results, MemberHelper.SORT_BY_LEVEL);
            return results;

        } catch (PersistenceException pe) {
            log.warning("Failure fetching leader list", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public int getABTestGroup (ReferralInfo info, String testName, boolean logEvent)
    {
        return _memberLogic.getABTestGroup(testName, info, logEvent);
    }

    // from MemberService
    public void trackClientAction (ReferralInfo info, String actionName, String details)
    {
        _eventLog.clientAction(info.tracker, actionName, details);
    }

    // from MemberService
    public void trackTestAction (ReferralInfo info, String actionName, String testName)
    {
        int abTestGroup = -1;
        if (testName != null) {
            // grab the group without logging a tracking event about it
            abTestGroup = _memberLogic.getABTestGroup(testName, info, false);
        } else {
            testName = "";
        }
        _eventLog.testAction(info.tracker, actionName, testName, abTestGroup);
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
            
            params.set("referral_params", 
                makeReferralParams(inviter.memberId+"", EMAIL_VECTOR, null));

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

    protected static String makeReferralParams (String affiliate, String vector, String creative) 
    {
        return "aid_" + (affiliate != null ? affiliate : "") + 
            "_" + (vector != null ? vector : "") + 
            "_" + (creative != null && creative.length() > 0 ? creative : "_");
    }

    @Inject protected ProfileRepository _profileRepo;
    @Inject protected FriendManager _friendMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected MemberLogic _memberLogic;

    /** Maximum number of members to return for the leader board */
    protected static final int MAX_LEADER_MATCHES = 100;
    
    /** This vector string represents an email invite */
    public static final String EMAIL_VECTOR = "emailInvite";
}
