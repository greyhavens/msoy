//
// $Id$

package com.threerings.msoy.mail.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.operator.Logic;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.util.JSONMarshaller;

import com.threerings.msoy.mail.gwt.Conversation;
import com.threerings.msoy.mail.gwt.GameAwardPayload;
import com.threerings.msoy.mail.gwt.MailPayload;

/**
 * Manages the persistent store of mail and mailboxes.
 */
@Singleton @BlockingThread
public class MailRepository extends DepotRepository
{
    @Inject public MailRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Returns the number of conversations in which the specified member is a participant.
     */
    public int loadConversationCount (int participantId)
    {
        return load(CountRecord.class, new FromOverride(ParticipantRecord.class),
                    new Where(ParticipantRecord.PARTICIPANT_ID, participantId)).count;
    }

    /**
     * Returns the number of conversations in which the specified member is a participant and which
     * have messages not read by that member.
     */
    public int loadUnreadConvoCount (int memberId)
    {
        SQLExpression isMe = new Conditionals.Equals(ParticipantRecord.PARTICIPANT_ID, memberId);
        SQLExpression isNew = new Conditionals.GreaterThan(
            ConversationRecord.LAST_SENT, ParticipantRecord.LAST_READ);
        return load(CountRecord.class,
                    new FromOverride(ParticipantRecord.class),
                    new Join(ParticipantRecord.CONVERSATION_ID,
                             ConversationRecord.CONVERSATION_ID),
                    new Where(new Logic.And(isMe, isNew))).count;
    }

    /**
     * Loads conversations in which the specified member is a participant, sorted by most recently
     * active to least.
     */
    public List<ConversationRecord> loadConversations (int participantId, int offset, int count)
    {
        return findAll(ConversationRecord.class, CacheStrategy.RECORDS, Lists.newArrayList(
            new Join(ConversationRecord.CONVERSATION_ID, ParticipantRecord.CONVERSATION_ID),
            new Where(ParticipantRecord.PARTICIPANT_ID, participantId),
            new Limit(offset, count),
            OrderBy.descending(ConversationRecord.LAST_SENT)));
    }

    /**
     * Loads a particular conversation record.
     */
    public ConversationRecord loadConversation (int conversationId)
    {
        return load(ConversationRecord.class, conversationId);
    }

    /**
     * Loads the messages in the supplied conversation (in oldest to newest order).
     */
    public List<ConvMessageRecord> loadMessages (int conversationId)
    {
        return findAll(ConvMessageRecord.class, CacheStrategy.RECORDS, Lists.newArrayList(
                       new Where(ConvMessageRecord.CONVERSATION_ID, conversationId),
                       OrderBy.ascending(ConvMessageRecord.SENT)));
    }

    /**
     * Returns the sent time of the last message read by the specified participant in the specified
     * conversation or null if the specified member is not a participant in that conversation.
     */
    public Long loadLastRead (int conversationId, int participantId)
    {
        ParticipantRecord prec = load(ParticipantRecord.class,
                                      ParticipantRecord.getKey(conversationId, participantId));
        return (prec == null) ? null : prec.lastRead.getTime();
    }

    /**
     * Updates the last read timestamp for the specified member and conversation.
     */
    public void updateLastRead (int conversationId, int participantId, long lastRead)
    {
        updatePartial(ParticipantRecord.getKey(conversationId, participantId),
                      ParticipantRecord.LAST_READ, new Timestamp(lastRead));
    }

    /**
     * Starts a conversation with the specified subject.
     */
    public ConversationRecord startConversation (int recipientId, int authorId, String subject,
                                                 String body, MailPayload payload,
                                                 boolean authorParticipation)
    {
        // first serialize the payload so that we can fail early if that fails
        ConvMessageRecord cmrec = new ConvMessageRecord();
        if (payload != null) {
            cmrec.payloadType = payload.getType();
            try {
                cmrec.payloadState =
                    JSONMarshaller.getMarshaller(payload.getClass()).getStateBytes(payload);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // TODO: do this in a transaction
        ConversationRecord conrec = new ConversationRecord();
        conrec.subject = subject;
        conrec.initiatorId = authorId;
        conrec.targetId = recipientId;
        conrec.lastSent = new Timestamp(System.currentTimeMillis());
        conrec.lastSnippet = StringUtil.truncate(body, Conversation.SNIPPET_LENGTH);
        conrec.lastAuthorId = authorId;
        insert(conrec);

        cmrec.conversationId = conrec.conversationId;
        cmrec.sent = conrec.lastSent;
        cmrec.authorId = authorId;
        cmrec.body = body;
        insert(cmrec);

        if (authorParticipation) {
            ParticipantRecord aprec = new ParticipantRecord();
            aprec.conversationId = conrec.conversationId;
            aprec.participantId = authorId;
            aprec.lastRead = conrec.lastSent;
            insert(aprec);
        }

        ParticipantRecord rprec = new ParticipantRecord();
        rprec.conversationId = conrec.conversationId;
        rprec.participantId = recipientId;
        rprec.lastRead = new Timestamp(0);
        insert(rprec);

        return conrec;
    }

    /**
     * Adds a message to a conversation.
     */
    public ConvMessageRecord addMessage (ConversationRecord conrec, int authorId, String body,
                                         int payloadType, byte[] payloadState)
    {
        ConvMessageRecord record = new ConvMessageRecord();
        record.conversationId = conrec.conversationId;
        record.sent = new Timestamp(System.currentTimeMillis());
        record.authorId = authorId;
        record.body = body;
        record.payloadType = payloadType;
        record.payloadState = payloadState;
        insert(record);

        // update the conversation record
        updatePartial(ConversationRecord.class, conrec.conversationId,
                      ConversationRecord.LAST_SENT, record.sent,
                      ConversationRecord.LAST_AUTHOR_ID, authorId,
                      ConversationRecord.LAST_SNIPPET,
                      StringUtil.truncate(body, Conversation.SNIPPET_LENGTH));

        // ressurect the other conversation participant if they have deleted this convo
        int otherId = conrec.getOtherId(authorId);
        if (loadLastRead(conrec.conversationId, otherId) == null) {
            ParticipantRecord parrec = new ParticipantRecord();
            parrec.conversationId = conrec.conversationId;
            parrec.participantId = otherId;
            // set their last read time to a couple of seconds before this message's sent time
            parrec.lastRead = new Timestamp(record.sent.getTime()-2000);
            insert(parrec);
        }

        // note that we added a message to a conversation
        _eventLog.mailSent(conrec.conversationId, authorId, payloadType);

        return record;
    }

    /**
     * Deletes the specified participant from the specified conversation.
     *
     * @return true if the participant was deleted, false if they were not due to the conversation
     * having unread messages (and ignoreUnread was false).
     */
    public boolean deleteConversation (final int conversationId, int participantId,
                                       boolean ignoreUnread)
    {
        // TODO: do this in a transaction
        Key<ParticipantRecord> key = ParticipantRecord.getKey(conversationId, participantId);
        ParticipantRecord parrec = load(ParticipantRecord.class, key);
        if (parrec == null) {
            return true; // oh yeah, we're deleted
        }

        // if the conversation is already gone somehow, just delete our participant record
        ConversationRecord conrec = load(ConversationRecord.class, conversationId);
        if (conrec == null) {
            delete(parrec);
            return true;
        }

        // make sure the conversation is fully read by the participant
        if (!ignoreUnread && conrec.lastSent.getTime() > parrec.lastRead.getTime()) {
            return false;
        }

        // delete our participation record
        delete(parrec);

        // if the other guy is still around, then stop here
        if (loadLastRead(conversationId, conrec.getOtherId(participantId)) != null) {
            return true;
        }

        // otherwise actually delete the contents of the conversation
        deleteAll(ConvMessageRecord.class,
                  new Where(ConvMessageRecord.CONVERSATION_ID, conversationId));
        delete(conrec);

        return true;
    }

    /**
     * Updates the payload state of a message.
     */
    public void updatePayloadState (int conversationId, long sent, byte[] state)
    {
        updatePartial(ConvMessageRecord.getKey(conversationId, new Timestamp(sent)),
                      ConvMessageRecord.PAYLOAD_STATE, state);
    }

    /**
     * Loads all complaints about a conversation.
     */
    public List<ConversationComplaintRecord> loadComplaints(int convoId)
    {
        return findAll(ConversationComplaintRecord.class,
            new Where(ConversationComplaintRecord.CONVERSATION_ID, convoId));
    }

    /**
     * Flags a complaint on a message.
     */
    public void addComplaint(int convoId, int complainerId)
    {
        ConversationComplaintRecord complaint = new ConversationComplaintRecord();
        complaint.complainerId = complainerId;
        complaint.conversationId = convoId;
        insert(complaint);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ConversationRecord.class);
        classes.add(ConvMessageRecord.class);
        classes.add(ParticipantRecord.class);
        classes.add(ConversationComplaintRecord.class);
    }

    @Inject protected MsoyEventLogger _eventLog;

    static {
        // register a migration for TrophyAwardPayload -> GameAwardPayload
        Map<String, String> migmap = Maps.newHashMap();
        migmap.put("trophyName", "awardName");
        migmap.put("trophyMedia", "awardMediaHash");
        migmap.put("trophyMimeType", "awardMimeType");
        JSONMarshaller.registerMigration(GameAwardPayload.class, migmap);
    }
}
