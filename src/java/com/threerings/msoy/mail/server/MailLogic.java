//
// $Id$

package com.threerings.msoy.mail.server;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.mail.gwt.FriendInvitePayload;
import com.threerings.msoy.mail.gwt.MailPayload;
import com.threerings.msoy.mail.gwt.PresentPayload;
import com.threerings.msoy.mail.gwt.RoomGiftPayload;
import com.threerings.msoy.mail.server.persist.ConvMessageRecord;
import com.threerings.msoy.mail.server.persist.ConversationRecord;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.person.gwt.InvitationResults;
import com.threerings.msoy.room.server.RoomLogic;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.JSONMarshaller;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.spam.server.SpamUtil;
import com.threerings.msoy.web.gwt.ServiceCodes;

import static com.threerings.msoy.Log.log;

/**
 * Provides mail related services to servlets and other blocking thread entities.
 */
@Singleton @BlockingThread
public class MailLogic
{
    /**
     * Sends a friend invitation email from the supplied inviter to the specified member.
     */
    public void sendFriendInvite (int inviterId, int friendId)
        throws ServiceException
    {
        MemberRecord sender = _memberRepo.loadMember(inviterId);
        MemberRecord recip = _memberRepo.loadMember(friendId);
        if (sender == null || recip == null) {
            log.warning("Missing records for friend invite [iid=" + inviterId +
                        ", tid=" + friendId + ", irec=" + sender + ", trec=" + recip + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        String subj = _serverMsgs.getBundle("server").get("m.friend_invite_subject");
        String body = _serverMsgs.getBundle("server").get("m.friend_invite_body");
        startConversation(sender, recip, subj, body, new FriendInvitePayload());
    }

    /**
     * Starts a mail conversation between the specified two parties without filling up the author's
     * inbox.
     */
    public void startBulkConversation (MemberRecord sender, Set<Integer> recipientIds,
        String subject, String body, MailPayload payload, boolean checkMuteList)
        throws ServiceException
    {

        // check mute list if requested (some bulk conversations are for automated message, some
        // are for player to player spam)
        if (checkMuteList) {
            _memberRepo.filterMuterRecipients(sender.memberId, recipientIds);
        }

        // now start the conversation (and deliver the message)
        ConvMessageRecord cmr = serializePayload(payload);

        for (int recipientId : recipientIds) {
            MemberRecord recip = _memberRepo.loadMember(recipientId);
            if (recip == null) {
                log.warning("Null recipient in bulk convo",
                    "recipient", recipientId, "sender", sender.memberId);
                continue;
            }

            _mailRepo.startConversation(
                recip.memberId, sender.memberId, subject, body, cmr, false, true);

            // potentially send a real email to the recipient
            sendMailEmail(sender, recip, subject, body);

            // let recipient know they've got mail
            MemberNodeActions.reportUnreadMail(recip.memberId, getUnreadConvoCount(recip.memberId));
        }
    }

    /**
     * Count the given member's unread mail, taking into account their mute list.
     */
    public int getUnreadConvoCount (int memberId)
    {
        return _mailRepo.loadUnreadConvoCount(
            memberId, Ints.asList(_memberRepo.loadMutelist(memberId)));
    }

    /**
     * Starts a mail conversation between the specified two parties.
     */
    public void startConversation (MemberRecord sender, MemberRecord recip,
                                   String subject, String body, MailPayload attachment)
        throws ServiceException
    {
        if (attachment instanceof FriendInvitePayload) {
            Friendship fr = _memberRepo.getTwoWayFriendship(sender.memberId, recip.memberId);
            switch (fr) {
            default:
                break; // it's all good, proceed

            case INVITEE: // hey, they already sent us an invite!
                _memberLogic.establishFriendship(sender.memberId, recip.memberId);
                return; // do not send the mail

            case INVITED:
                throw new ServiceException(InvitationResults.ALREADY_FRIEND_INV);

            case FRIENDS:
                throw new ServiceException(InvitationResults.ALREADY_FRIEND);
            }
        }

        boolean isMuted = checkMuting(sender, recip);

        // now start the conversation (and deliver the message)
        ConvMessageRecord cmr = serializePayload(attachment);

        if (!isMuted) {
            processPayload(sender.memberId, recip.memberId, attachment);
        }

        _mailRepo.startConversation(
            recip.memberId, sender.memberId, subject, body, cmr, true, !isMuted);
        if (attachment instanceof FriendInvitePayload) {
            _memberRepo.noteFriendInvitationSent(sender.memberId, recip.memberId);
        }

        if (!isMuted) {
            // potentially send a real email to the recipient
            sendMailEmail(sender, recip, subject, body);

            // let recipient know they've got mail
            MemberNodeActions.reportUnreadMail(recip.memberId, getUnreadConvoCount(recip.memberId));
        }
    }

    /**
     * Continues the specified mail conversation.
     */
    public ConvMessageRecord continueConversation (MemberRecord poster, int convoId, String body,
                                                   MailPayload attachment)
        throws ServiceException
    {
        ConversationRecord conrec = _mailRepo.loadConversation(convoId);
        if (conrec == null) {
            log.warning("Requested to continue non-existent conversation [by=" + poster.who() +
                        ", convoId=" + convoId + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // make sure this member is a conversation participant
        Long lastRead = _mailRepo.loadLastRead(convoId, poster.memberId);
        if (lastRead == null) {
            log.warning("Request to continue conversation by non-member [who=" + poster.who() +
                        ", convoId=" + convoId + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // find out some things about the other participant
        int otherId = conrec.getOtherId(poster.memberId);
        MemberRecord recip = _memberRepo.loadMember(otherId);
        boolean isMuted = (recip != null) && checkMuting(poster, recip);

        ConvMessageRecord cmr = serializePayload(attachment);

        // TODO: make sure body.length() is not too long

        if (!isMuted) {
            processPayload(poster.memberId, conrec.getOtherId(poster.memberId), attachment);
        }

        // store the message in the repository
        _mailRepo.addMessage(conrec, poster.memberId, body, cmr, !isMuted);

        // update our last read for this conversation to reflect that we've read our message
        _mailRepo.updateLastRead(convoId, poster.memberId, cmr.sent.getTime());

        // note that we added a message to a conversation
        _eventLog.mailSent(conrec.conversationId, poster.memberId, cmr.payloadType);

        if (!isMuted) {
            // let other conversation participant know they've got mail
            MemberNodeActions.reportUnreadMail(otherId, getUnreadConvoCount(otherId));

            // potentially send a real email to the recipient
            if (recip != null) {
                String subject = _serverMsgs.getBundle("server").get(
                    "m.reply_subject", conrec.subject);
                sendMailEmail(poster, recip, subject, body);
            }
        }
        return cmr;
    }

    /**
     * Sends email to all players who have not opted out of Whirled announcements.
     */
    public void spamPlayers (String subject, String body)
    {
        // TODO: if we want to continue to use this mechanism to send mass emails to our members,
        // we will need to farm out the mail deliver task to all nodes in the network so that we
        // don't task one node with sending out a million email messages

        // convert the body into proper-ish HTML
        body = SpamUtil.formatSpam(body);
        if (body == null) {
            return;
        }

        // load up the emails of everyone we want to spam
        List<Tuple<Integer, String>> emails = _memberRepo.loadMemberEmailsForAnnouncement();

        // add the Return Path probe addresses (as long as we're not on dev)
        if (!DeploymentConfig.devDeployment) {
            for (String rpaddr : SpamUtil.getReturnPathAddrs()) {
                emails.add(Tuple.newTuple(0, rpaddr));
            }
        }

        // ship our giant list off to the mail sender
        _mailer.sendSpam(MailSender.By.COMPUTER, emails, ServerConfig.getFromAddress(),
                         SpamUtil.makeSpamHeaders(subject), subject, body);

        log.info("Queued up announcement email", "subject", subject, "count", emails.size());
    }

    /**
     * Sends a spam preview mailing to the specified address.
     */
    public void previewSpam (int recipId, String recip, String subject, String body,
                             boolean includeProbeList)
    {
        // convert the body into proper-ish HTML
        body = SpamUtil.formatSpam(body);
        if (body == null) {
            return;
        }

        // add the caller to the recipient list
        List<Tuple<Integer, String>> emails = Lists.newArrayList();
        emails.add(Tuple.newTuple(recipId, recip));

        // add the Return Path probe addresses if they were requested
        if (includeProbeList) {
            for (String rpaddr : SpamUtil.getReturnPathAddrs()) {
                emails.add(Tuple.newTuple(0, rpaddr));
            }
        }

        // ship all this off to the mail sender
        _mailer.sendSpam(MailSender.By.HUMAN, emails, ServerConfig.getFromAddress(),
                         SpamUtil.makeSpamHeaders(subject), subject, body);
    }

    /**
     * Serialize the specified payload into a newly-initialized ConvMessageRecord.
     */
    protected ConvMessageRecord serializePayload (MailPayload payload)
    {
        ConvMessageRecord record = new ConvMessageRecord();
        if (payload != null) {
            record.payloadType = payload.getType();
            try {
                record.payloadState =
                    JSONMarshaller.getMarshaller(payload.getClass()).getStateBytes(payload);
            } catch (Exception e) {
                log.warning("Failed to encode message attachment.", e);
                throw new RuntimeException(e);
            }
        }
        return record;
    }

    /**
     * Return whether the recipient has muted the sender, but throw
     * a ServiceException if the sender has muted the recipient.
     */
    protected boolean checkMuting (MemberRecord sender, MemberRecord recip)
        throws ServiceException
    {
        if (_memberRepo.isMuted(sender.memberId, recip.memberId)) {
            throw new ServiceException("e.cant_mail_muted");
        }
        return _memberRepo.isMuted(recip.memberId, sender.memberId);
    }

    /**
     * Handles any side-effects of mail payload delivery.
     */
    protected void processPayload (int senderId, int recipId, MailPayload payload)
        throws ServiceException
    {
        if (payload instanceof PresentPayload) {
            processPresentPayload(senderId, recipId, (PresentPayload) payload);

        } else if (payload instanceof RoomGiftPayload) {
            processRoomGiftPayload(senderId, recipId, (RoomGiftPayload) payload);
        }
    }

    /**
     * Process gifting an item.
     */
    protected void processPresentPayload (int senderId, int recipId, PresentPayload payload)
        throws ServiceException
    {
        ItemIdent ident = payload.ident;
        ItemRepository<?> repo = _itemLogic.getRepository(ident.type);
        ItemRecord item = repo.loadItem(ident.itemId);

        // validate that they're allowed to gift this item (these are all also checked on the
        // client so we don't need useful error messages)
        String errmsg = null;
        if (item == null) {
            errmsg = "Trying to gift non-existent item";
        } else if (item.ownerId != senderId) {
            errmsg = "Trying to gift un-owned item";
        } else if (item.used.forAnything()) {
            errmsg = "Trying to gift in-use item";
        }
        if (errmsg != null) {
            log.warning(errmsg, "sender", senderId, "recip", recipId, "ident", ident);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRecord oitem = (ItemRecord)item.clone();
        repo.updateOwnerId(item, recipId);

        // notify the item system that the item has moved
        _itemLogic.itemUpdated(oitem, item);
    }

    /**
     * Process gifting a room.
     */
    protected void processRoomGiftPayload (int senderId, int recipId, RoomGiftPayload payload)
        throws ServiceException
    {
        _roomLogic.processRoomGift(senderId, recipId, payload.sceneId);
        _itemLogic.transferRoomItems(senderId, recipId, payload.sceneId);
    }

    /**
     * Send an email to a Whirled mail recipient to report that they received a Whirled mail. Does
     * nothing if the recipient has requested not to receive such mails.
     */
    protected void sendMailEmail (MemberRecord sender, MemberRecord recip,
                                  String subject, String body)
    {
        // // if they don't want to hear about it or are spanked or have not validated, stop now
        // if (recip.isSet(MemberRecord.Flag.NO_WHIRLED_MAIL_TO_EMAIL) ||
        //     recip.isSet(MemberRecord.Flag.SPANKED) ||
        //     !recip.isSet(MemberRecord.Flag.VALIDATED)) {
        //     return;
        // }
        // _mailer.sendTemplateEmail(
        //     MailSender.By.HUMAN, recip.accountName, ServerConfig.getFromAddress(), "gotMail",
        //     "subject", subject,"sender", sender.name, "senderId", sender.memberId,
        //     "body", body, "server_url", ServerConfig.getServerURL());
    }

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected MailSender _mailer;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected RoomLogic _roomLogic;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected ServerMessages _serverMsgs;
}
