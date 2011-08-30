//
// $Id$

package com.threerings.msoy.mail.server;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;

import com.samskivert.depot.DuplicateKeyException;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.mail.gwt.ConvMessage;
import com.threerings.msoy.mail.gwt.Conversation;
import com.threerings.msoy.mail.gwt.MailPayload;
import com.threerings.msoy.mail.gwt.MailService;
import com.threerings.msoy.mail.server.persist.ConvMessageRecord;
import com.threerings.msoy.mail.server.persist.ConversationRecord;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.JSONMarshaller;
import com.threerings.msoy.underwire.server.SupportLogic;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link MailService}.
 */
public class MailServlet extends MsoyServiceServlet
    implements MailService
{
    // from interface MailService
    public ConvosResult loadConversations (int offset, int count, boolean needCount)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();

        // find who we're muting
        List<Integer> muted = Ints.asList(_memberRepo.loadMutelist(memrec.memberId));

        // load up the unmuted conversations in question
        List<Conversation> convos = Lists.newArrayList();
        List<ConversationRecord> conrecs =
            _mailRepo.loadConversations(memrec.memberId, muted, offset, count);

        // figure out who the other party is in each conversation
        Set<Integer> otherIds = Sets.newHashSet();
        for (ConversationRecord conrec : conrecs) {
            convos.add(conrec.toConversation());
            otherIds.add(conrec.getOtherId(memrec.memberId));
        }

        // load up the last read info for these conversations (TODO: do this in a join when
        // loading the conversation records)
        for (Conversation convo : convos) {
            Long lastRead = _mailRepo.loadLastRead(convo.conversationId, memrec.memberId);
            convo.hasUnread = (lastRead == null) || (lastRead < convo.lastSent.getTime());
        }

        // resolve the member cards for the other parties
        Map<Integer, MemberCard> others = MemberCardRecord.toMap(_memberRepo.loadMemberCards(otherIds));
        for (int ii = 0, ll = convos.size(); ii < ll; ii++) {
            convos.get(ii).other = others.get(conrecs.get(ii).getOtherId(memrec.memberId));
        }

        ConvosResult result = new ConvosResult();
        if (needCount) {
            result.unreadConvoCount = _mailRepo.loadUnreadConvoCount(memrec.memberId, muted);
        }
        result.convos = convos;
        return result;
    }

    // from interface MailService
    public ConvoResult loadConversation (int convoId)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();
        ConvoResult convo = new ConvoResult();

        // make sure this member is a conversation participant or a member of support staff
        Long lastRead = _mailRepo.loadLastRead(convoId, memrec.memberId);
        boolean support = false;
        if (lastRead == null) {
            if (memrec.isSupport()) {
                // don't let support see uncomplained conversations
                if (_mailRepo.loadComplaints(convoId).size() == 0) {
                    return null;
                }
                lastRead = 0L;
                support = true;
            } else {
                return null;
            }
        }
        convo.lastRead = lastRead;

        // load up the conversation
        ConversationRecord conrec = _mailRepo.loadConversation(convoId);
        if (conrec == null) {
            return null;
        }

        // leave the other name blank if this is a support review
        if (!support) {
            convo.other = _memberRepo.loadMemberName(conrec.getOtherId(memrec.memberId));
        }
        convo.subject = conrec.subject;

        // load up the messages in this conversation
        List<ConvMessage> msgs = Lists.newArrayList();
        List<ConvMessageRecord> cmrecs = _mailRepo.loadMessages(convoId);
        Set<Integer> authorIds = Sets.newHashSet();

        long newLastRead = lastRead;
        for (ConvMessageRecord cmrec : cmrecs) {
            msgs.add(cmrec.toConvMessage());
            authorIds.add(cmrec.authorId);
            newLastRead = Math.max(newLastRead, cmrec.sent.getTime());
        }

        // resolve the member cards for the participants
        Map<Integer, MemberCard> authors = MemberCardRecord.toMap(_memberRepo.loadMemberCards(authorIds));
        for (int ii = 0, ll = msgs.size(); ii < ll; ii++) {
            msgs.get(ii).author = authors.get(cmrecs.get(ii).authorId);
        }
        convo.messages = msgs;

        // maybe mark this member as having read the conversation
        if (!support && newLastRead > lastRead) {
            _mailRepo.updateLastRead(convoId, memrec.memberId, newLastRead);
            // decrement their unread mail count if they are online
            MemberNodeActions.reportUnreadMail(memrec.memberId, -1);
        }

        return convo;
    }

    // from interface MailService
    public void startConversation (int recipId, String subject, String body, MailPayload attachment)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();

        // make sure the recipient exists
        MemberRecord recip = _memberRepo.loadMember(recipId);
        if (recip == null) {
            log.warning("Requested to send mail to non-existent recipient " +
                        "[from=" + memrec.who() + ", to=" + recipId + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // let the mail manager handle the rest
        _mailLogic.startConversation(memrec, recip, subject, body, attachment);
    }

    // from interface MailService
    public ConvMessage continueConversation (int convoId, String body, MailPayload attachment)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();

        // pass the buck to the mail manager
        ConvMessageRecord cmr = _mailLogic.continueConversation(memrec, convoId, body, attachment);

        // convert the added message to a runtime record and return it to the caller
        ConvMessage result = cmr.toConvMessage();
        result.author = _memberRepo.loadMemberCard(memrec.memberId, false);
        return result;
    }

    // from interface MailService
    public boolean deleteConversation (int convoId, boolean ignoreUnread)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();
        // the repository handles all the juicy goodness
        return _mailRepo.deleteConversation(convoId, memrec.memberId, ignoreUnread);
    }

    // from interface MailService
    public void deleteConversations (List<Integer> convoIds)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();
        // blow them away, if they have unread messages, fuck 'em
        for (int convoId : convoIds) {
            _mailRepo.deleteConversation(convoId, memrec.memberId, true);
        }
    }

    // from interface MailService
    public void updatePayload (int convoId, long sent, MailPayload payload)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();
        try {
            // note: we don't validate memberId because this method is legacy and going away
            byte[] state = JSONMarshaller.getMarshaller(payload.getClass()).getStateBytes(payload);
            _mailRepo.updatePayloadState(convoId, sent, state);

        } catch (Exception e) {
            log.warning("Failed update payload", "mid", memrec.memberId, "cid", convoId,
                        "sent", sent, "pay", payload, e);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MailService
    public void complainConversation (int convoId, String reason)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();

        // make sure this member is a conversation participant
        Long lastRead = _mailRepo.loadLastRead(convoId, memrec.memberId);
        if (lastRead == null) {
            return;
        }

        // load the conversation
        ConversationRecord conrec = _mailRepo.loadConversation(convoId);
        if (conrec == null) {
            return;
        }

        // load up the messages in this conversation
        List<ConvMessageRecord> cmrecs = _mailRepo.loadMessages(convoId);
        Set<Integer> authorIds = Sets.newHashSet();

        for (ConvMessageRecord cmrec : cmrecs) {
            authorIds.add(cmrec.authorId);
        }

        // resolve the member cards for the participants
        Map<Integer, MemberCard> authors = MemberCardRecord.toMap(_memberRepo.loadMemberCards(authorIds));

        StringBuilder bodyText = new StringBuilder();
        for (ConvMessageRecord cmrec : cmrecs) {
            bodyText.append(
                new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss").format(cmrec.sent))
                .append(" ");

            MemberCard author = authors.get(cmrec.authorId);
            bodyText.append((author != null) ? author.name.toString() : "<unknown>")
                .append("(").append(cmrec.authorId).append("):\n");
            if (cmrec.payloadType != 0) {
                bodyText.append("Special mail type: ")
                    .append(describePayloadType(cmrec.payloadType))
                    .append("\n");
            }
            bodyText.append(cmrec.body).append("\n----\n");
        }

        // flag the complaint, with an error if it was already flagged
        try {
            _mailRepo.addComplaint(convoId, memrec.memberId);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MailCodes.COMPLAINT_ALREADY_REGISTERED);
        }

        // queue up the event
        _supportLogic.addMessageComplaint(
            memrec.getName(), conrec.getOtherId(memrec.memberId),
            bodyText.toString(), "Conversation: " + reason, Pages.MAIL.makeLink("c", convoId));
    }

    /**
     * Describe the given payload type in human-readable language.
     */
    protected static String describePayloadType (int type)
    {
        switch(type) {
        case MailPayload.TYPE_GROUP_INVITE:
            return "Group Invitation";
        case MailPayload.TYPE_FRIEND_INVITE:
            return "Friend Invitation";
        case MailPayload.TYPE_PRESENT:
            return "Item Gift";
        case MailPayload.TYPE_GAME_INVITE:
            return "Game Invitation";
        case MailPayload.TYPE_ROOM_GIFT:
            return "Room Gift";
        default:
            return "Unknown Payload Type: " + type;
        }
    }

    @Inject protected MailLogic _mailLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected SupportLogic _supportLogic;
}
