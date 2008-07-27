//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.JSONMarshaller;

import com.threerings.msoy.person.gwt.ConvMessage;
import com.threerings.msoy.person.gwt.Conversation;
import com.threerings.msoy.person.gwt.MailPayload;
import com.threerings.msoy.person.gwt.MailService;
import com.threerings.msoy.person.server.MailLogic;
import com.threerings.msoy.person.server.persist.ConvMessageRecord;
import com.threerings.msoy.person.server.persist.ConversationRecord;
import com.threerings.msoy.person.server.persist.MailRepository;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link MailService}.
 */
public class MailServlet extends MsoyServiceServlet
    implements MailService
{
    // from interface MailService
    public ConvosResult loadConversations (WebIdent ident, int offset, int count, boolean needCount)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        try {
            // load up the conversations in question
            List<Conversation> convos = Lists.newArrayList();
            List<ConversationRecord> conrecs =
                _mailRepo.loadConversations(memrec.memberId, offset, count);
            IntSet otherIds = new ArrayIntSet();
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
            IntMap<MemberCard> others = IntMaps.newHashIntMap();
            for (MemberCardRecord mcr : _memberRepo.loadMemberCards(otherIds)) {
                others.put(mcr.memberId, mcr.toMemberCard());
            }
            for (int ii = 0, ll = convos.size(); ii < ll; ii++) {
                convos.get(ii).other = others.get(conrecs.get(ii).getOtherId(memrec.memberId));
            }

            ConvosResult result = new ConvosResult();
            if (needCount) {
                result.totalConvoCount = _mailRepo.loadConversationCount(memrec.memberId);
            }
            result.convos = convos;
            return result;

        } catch (PersistenceException pe) {
            log.warning("Load conversations failed [for=" + memrec.who() +
                    ", offset=" + offset + ", count=" + count + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MailService
    public ConvoResult loadConversation (WebIdent ident, int convoId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        try {
            ConvoResult convo = new ConvoResult();

            // make sure this member is a conversation participant
            Long lastRead = _mailRepo.loadLastRead(convoId, memrec.memberId);
            if (lastRead == null) {
                return null;
            }
            convo.lastRead = lastRead;

            // load up the conversation
            ConversationRecord conrec = _mailRepo.loadConversation(convoId);
            if (conrec == null) {
                return null;
            }
            convo.other = _memberRepo.loadMemberName(conrec.getOtherId(memrec.memberId));
            convo.subject = conrec.subject;

            // load up the messages in this conversation
            List<ConvMessage> msgs = Lists.newArrayList();
            List<ConvMessageRecord> cmrecs = _mailRepo.loadMessages(convoId);
            IntSet authorIds = new ArrayIntSet();
            long newLastRead = lastRead;
            for (ConvMessageRecord cmrec : cmrecs) {
                msgs.add(cmrec.toConvMessage());
                authorIds.add(cmrec.authorId);
                newLastRead = Math.max(newLastRead, cmrec.sent.getTime());
            }

            // resolve the member cards for the participants
            IntMap<MemberCard> authors = IntMaps.newHashIntMap();
            for (MemberCardRecord mcr : _memberRepo.loadMemberCards(authorIds)) {
                authors.put(mcr.memberId, mcr.toMemberCard());
            }
            for (int ii = 0, ll = msgs.size(); ii < ll; ii++) {
                msgs.get(ii).author = authors.get(cmrecs.get(ii).authorId);
            }
            convo.messages = msgs;

            // maybe mark this member as having read the conversation
            if (newLastRead > lastRead) {
                _mailRepo.updateLastRead(convoId, memrec.memberId, newLastRead);
                // decrement their unread mail count if they are online
                MemberNodeActions.reportUnreadMail(memrec.memberId, -1);
            }

            return convo;

        } catch (PersistenceException pe) {
            log.warning("Load conversation failed [for=" + memrec.who() +
                    ", convoId=" + convoId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MailService
    public void startConversation (WebIdent ident, int recipId, String subject, String body,
                                   MailPayload attachment)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        try {
            // make sure the recipient exists
            MemberRecord recip = _memberRepo.loadMember(recipId);
            if (recip == null) {
                log.warning("Requested to send mail to non-existent recipient " +
                            "[from=" + memrec.who() + ", to=" + recipId + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            // let the mail manager handle the rest
            _mailLogic.startConversation(memrec, recip, subject, body, attachment);

        } catch (PersistenceException pe) {
            log.warning("Start conversation failed [for=" + memrec.who() +
                    ", recipId=" + recipId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MailService
    public ConvMessage continueConversation (WebIdent ident, int convoId, String body,
                                             MailPayload attachment)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        try {
            // pass the buck to the mail manager
            ConvMessageRecord cmr = _mailLogic.continueConversation(
                memrec, convoId, body, attachment);

            // convert the added message to a runtime record and return it to the caller
            ConvMessage result = cmr.toConvMessage();
            for (MemberCardRecord mcr : _memberRepo.loadMemberCards(
                     Collections.singleton(memrec.memberId))) {
                result.author = mcr.toMemberCard();
            }
            return result;

        } catch (PersistenceException pe) {
            log.warning("Continue conversation failed [for=" + memrec.who() +
                    ", convoId=" + convoId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MailService
    public boolean deleteConversation (WebIdent ident, int convoId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        try {
            // the repository handles all the juicy goodness
            return _mailRepo.deleteConversation(convoId, memrec.memberId);

        } catch (Exception e) {
            log.warning("Failed to delete convo [for=" + memrec.who() +
                    ", convoId=" + convoId + "].", e);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MailService
    public void updatePayload (WebIdent ident, int convoId, long sent, MailPayload payload)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        try {
            // note: we don't validate memberId because this method is legacy and going away
            byte[] state = JSONMarshaller.getMarshaller(payload.getClass()).getStateBytes(payload);
            _mailRepo.updatePayloadState(convoId, sent, state);

        } catch (Exception e) {
            log.warning("Failed update payload [mid=" + memrec.memberId +
                    ", cid=" + convoId + ", sent=" + sent + ", pay=" + payload + "].", e);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    @Inject protected MailRepository _mailRepo;
    @Inject protected MailLogic _mailLogic;
}
