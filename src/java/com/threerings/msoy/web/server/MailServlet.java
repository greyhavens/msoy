//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.JSONMarshaller;

import com.threerings.msoy.person.data.ConvMessage;
import com.threerings.msoy.person.data.Conversation;
import com.threerings.msoy.person.data.MailPayload;
import com.threerings.msoy.person.server.persist.ConvMessageRecord;
import com.threerings.msoy.person.server.persist.ConversationRecord;
import com.threerings.msoy.person.server.persist.MailRepository;

import com.threerings.msoy.web.client.MailService;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class MailServlet extends MsoyServiceServlet
    implements MailService
{
    // from interface MailService
    public ConvosResult loadConversations (WebIdent ident, int offset, int count, boolean needCount)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
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
            for (MemberCardRecord mcr : MsoyServer.memberRepo.loadMemberCards(otherIds)) {
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
            log.log(Level.WARNING, "Load conversations failed [for=" + memrec.who() +
                    ", offset=" + offset + ", count=" + count + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MailService
    public ConvoResult loadConversation (WebIdent ident, int convoId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
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
            convo.other = MsoyServer.memberRepo.loadMemberName(conrec.getOtherId(memrec.memberId));
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
            for (MemberCardRecord mcr : MsoyServer.memberRepo.loadMemberCards(authorIds)) {
                authors.put(mcr.memberId, mcr.toMemberCard());
            }
            for (int ii = 0, ll = msgs.size(); ii < ll; ii++) {
                msgs.get(ii).author = authors.get(cmrecs.get(ii).authorId);
            }
            convo.messages = msgs;

            // maybe mark this member as having read the conversation
            if (newLastRead > lastRead) {
                _mailRepo.updateLastRead(convoId, memrec.memberId, newLastRead);
            }

            return convo;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Load conversation failed [for=" + memrec.who() +
                    ", convoId=" + convoId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MailService
    public void startConversation (WebIdent ident, int recipientId, String subject, String body,
                                   MailPayload attachment)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        try {
            // TODO: validate recipient exists?
            _mailRepo.startConversation(recipientId, memrec.memberId, subject, body, attachment);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Start conversation failed [for=" + memrec.who() +
                    ", recipId=" + recipientId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MailService
    public ConvMessage continueConversation (WebIdent ident, int convoId, String text,
                                             MailPayload attachment)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        try {
            ConversationRecord conrec = _mailRepo.loadConversation(convoId);
            if (conrec == null) {
                log.warning("Requested to continue non-existent conversation [by=" + memrec.who() +
                            ", convoId=" + convoId + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            // make sure this member is a conversation participant
            Long lastRead = _mailRepo.loadLastRead(convoId, memrec.memberId);
            if (lastRead == null) {
                log.warning("Request to continue conversation by non-member [who=" + memrec.who() +
                            ", convoId=" + convoId + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            // TODO: make sure text.length() is not too long

            // encode the attachment if we have one
            int payloadType = 0;
            byte[] payloadState = null;
            if (attachment != null) {
                payloadType = attachment.getType();
                try {
                    payloadState = JSONMarshaller.getMarshaller(
                        attachment.getClass()).getStateBytes(attachment);
                } catch (Exception e) {
                    log.warning("Failed to encode message attachment [for=" + memrec.who() +
                                ", attachment=" + attachment + "].");
                    throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
                }
            }

            // store the message in the repository
            ConvMessageRecord cmr =
                _mailRepo.addMessage(convoId, memrec.memberId, text, payloadType, payloadState);

            // update our last read for this conversation to reflect that we've read our message
            _mailRepo.updateLastRead(convoId, memrec.memberId, cmr.sent.getTime());

            // TODO: let other conversation participants know they've got mail

            // convert the added message to a runtime record and return it to the caller
            ConvMessage result = cmr.toConvMessage();
            for (MemberCardRecord mcr : MsoyServer.memberRepo.loadMemberCards(
                     Collections.singleton(memrec.memberId))) {
                result.author = mcr.toMemberCard();
            }
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Continue conversation failed [for=" + memrec.who() +
                    ", convoId=" + convoId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

//     // from MailService
//     public void deleteMessages (final WebIdent ident, final int folderId, final int[] msgIdArr)
//         throws ServiceException
//     {
//         MemberRecord memrec = requireAuthedUser(ident);
//         try {
//             _mailRepo.deleteMessage(memrec.memberId, folderId, msgIdArr);
//             if (folderId == MailFolder.INBOX_FOLDER_ID) {
//                 int count = _mailRepo.getMessageCount(memrec.memberId, folderId).right;
//                 MemberNodeActions.reportUnreadMail(memrec.memberId, count);
//             }

//         } catch (PersistenceException pe) {
//             log.log(Level.WARNING, "Failed to delete messages [mid=" + memrec.memberId +
//                     ", fid=" + folderId + ", mids=" + StringUtil.toString(msgIdArr) + "].", pe);
//             throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//         }
//     }

//     // from MailService
//     public void deliverMessage (final WebIdent ident, final int recipientId, final String subject,
//                                 final String text, final MailPayload object)
//         throws ServiceException
//     {
//         final MemberRecord memrec = requireAuthedUser(ident);
//         final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
//             "deliverMessage[" + recipientId + ", " + subject + ", " + text + "]");
//         MsoyServer.omgr.postRunnable(new Runnable() {
//             public void run () {
//                 MsoyServer.mailMan.deliverMessage(
//                     memrec.memberId, recipientId, subject, text, object, false, waiter);
//             }
//         });
//         waiter.waitForResult();
//     }

    // from MailService
    public void updatePayload (WebIdent ident, int convoId, int authorId, long sent,
                               MailPayload payload)
        throws ServiceException
    {
//         MemberRecord memrec = requireAuthedUser(ident);
//         try {
//             byte[] state = JSONMarshaller.getMarshaller(payload.getClass()).getStateBytes(payload);
//             _mailRepo.setPayloadState(memrec.memberId, folderId, messageId, state);

//         } catch (Exception e) {
//             log.log(Level.WARNING, "Failed update payload [mid=" + memrec.memberId +
//                     ", fid=" + folderId + ", mid=" + messageId + ", pay=" + payload + "].", e);
//             throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//         }
    }

//     // from MailService
//     public MailFolder getFolder (WebIdent ident, final int folderId)
//         throws ServiceException
//     {
//         MemberRecord memrec = requireAuthedUser(ident);
//         try {
//             return buildFolder(_mailRepo.getFolder(memrec.memberId, folderId));

//         } catch (PersistenceException pe) {
//             log.log(Level.WARNING, "getFolder failed [mid=" + memrec.memberId +
//                     ", fid=" + folderId + "].", pe);
//             throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//         }
//     }

//     // from MailService
//     public List<MailFolder> getFolders (WebIdent ident)
//         throws ServiceException
//     {
//         MemberRecord memrec = requireAuthedUser(ident);
//         try {
//             List<MailFolder> result = new ArrayList<MailFolder>();
//             for (MailFolderRecord record : _mailRepo.getFolders(memrec.memberId)) {
//                 result.add(buildFolder(record));
//             }
//             return result;

//         } catch (PersistenceException pe) {
//             log.log(Level.WARNING, "getFolders failed [mid=" + memrec.memberId + "].", pe);
//             throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//         }
//     }

//     // from MailService
//     public List<MailHeaders> getHeaders (final WebIdent ident, final int folderId)
//         throws ServiceException
//     {
//         MemberRecord memrec = requireAuthedUser(ident);
//         try {
//             List<MailHeaders> result = new ArrayList<MailHeaders>();
//             for (MailMessageRecord record : _mailRepo.getMessages(memrec.memberId, folderId)) {
//                 result.add(record.toMailHeaders(MsoyServer.memberRepo));
//             }
//             return result;

//         } catch (PersistenceException pe) {
//             log.log(Level.WARNING, "getHeaders failed [mid=" + memrec.memberId +
//                     ", fid=" + folderId + "].", pe);
//             throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//         }
//     }

//     // from interface MailService
//     public List<MailMessage> getConversation (WebIdent ident, int folderId, int convId)
//         throws ServiceException
//     {
//         MemberRecord memrec = requireAuthedUser(ident);
//         try {
//             List<MailMessage> result = new ArrayList<MailMessage>();
//             IntSet unreadIds = new ArrayIntSet();

//             // currently the convId is the id of the other person in the conversation
//             for (MailMessageRecord record : _mailRepo.getMessages(
//                      memrec.memberId, folderId, convId)) {
//                 result.add(record.toMailMessage(MsoyServer.memberRepo));
//                 if (record.unread) {
//                     unreadIds.add(record.messageId);
//                 }
//             }

//             // if we read any unread messages, update their unread state and update this member's
//             // unread mail count
//             if (!unreadIds.isEmpty()) {
//                 _mailRepo.setUnread(memrec.memberId, folderId, unreadIds, false);
//                 // if we read an unread inbox message, count how many more of those there are
//                 if (folderId == MailFolder.INBOX_FOLDER_ID) {
//                     int count = _mailRepo.getMessageCount(memrec.memberId, folderId).right;
//                     MemberNodeActions.reportUnreadMail(memrec.memberId, count);
//                 }
//             }

//             return result;

//         } catch (PersistenceException pe) {
//             log.log(Level.WARNING, "getConversation failed [mid=" + memrec.memberId +
//                     ", fid=" + folderId + ", convId=" + convId + "].", pe);
//             throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//         }
//     }

//     // from MailService
//     public MailMessage getMessage (final WebIdent ident, final int folderId, final int messageId)
//         throws ServiceException
//     {
//         MemberRecord memrec = requireAuthedUser(ident);
//         MailMessageRecord record;
//         MailMessage message;

//         try {
//             record = _mailRepo.getMessage(memrec.memberId, folderId, messageId);
//             if (record == null) {
//                 return null;
//             }
//             if (record.unread) {
//                 _mailRepo.setUnread(memrec.memberId, folderId, messageId, false);
//                 // if we read an unread inbox message, count how many more of those there are
//                 if (folderId == MailFolder.INBOX_FOLDER_ID) {
//                     int count = _mailRepo.getMessageCount(memrec.memberId, folderId).right;
//                     MemberNodeActions.reportUnreadMail(memrec.memberId, count);
//                 }
//             }
//             message = record.toMailMessage(MsoyServer.memberRepo);

//         } catch (PersistenceException pe) {
//             log.log(Level.WARNING, "getMessage failed [mid=" + memrec.memberId +
//                     ", fid=" + folderId + "].", pe);
//             throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//         }

//         return message;
//     }

//     // build a MailFolder object, including the message counts which require a separate query
//     protected MailFolder buildFolder (MailFolderRecord record)
//         throws PersistenceException
//     {
//         MailFolder folder = toMailFolder(record);
//         Tuple<Integer, Integer> counts =
//             _mailRepo.getMessageCount(record.ownerId, record.folderId);
//         folder.unreadCount = counts.right != null ? counts.right.intValue() : 0;
//         folder.readCount = counts.left != null ? counts.left.intValue() : 0;
//         return folder;
//     }

//     // convert a MailFolderRecord to its MailFolder form
//     protected MailFolder toMailFolder (MailFolderRecord record)
//         throws PersistenceException
//     {
//         MailFolder folder = new MailFolder();
//         folder.folderId = record.folderId;
//         folder.ownerId = record.ownerId;
//         folder.name = record.name;
//         return folder;
//     }

    protected MailRepository _mailRepo = MsoyServer.mailRepo;
}
