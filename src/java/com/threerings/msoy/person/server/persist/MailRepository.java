//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;

import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.util.JSONMarshaller;

import com.threerings.msoy.person.gwt.Conversation;
import com.threerings.msoy.person.gwt.GameAwardPayload;
import com.threerings.msoy.person.gwt.MailPayload;

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
        throws PersistenceException
    {
        return load(CountRecord.class, new FromOverride(ParticipantRecord.class),
                    new Where(ParticipantRecord.PARTICIPANT_ID_C, participantId)).count;
    }

    /**
     * Returns the number of conversations in which the specified member is a participant and which
     * have messages not read by that member.
     */
    public int loadUnreadConvoCount (int memberId)
        throws PersistenceException
    {
        SQLExpression isMe = new Conditionals.Equals(ParticipantRecord.PARTICIPANT_ID_C, memberId);
        SQLExpression isNew = new Conditionals.GreaterThan(
            ConversationRecord.LAST_SENT_C, ParticipantRecord.LAST_READ_C);
        return load(CountRecord.class,
                    new FromOverride(ParticipantRecord.class),
                    new Join(ParticipantRecord.CONVERSATION_ID_C,
                             ConversationRecord.CONVERSATION_ID_C),
                    new Where(new Logic.And(isMe, isNew))).count;
    }

    /**
     * Loads conversations in which the specified member is a participant, sorted by most recently
     * active to least.
     */
    public List<ConversationRecord> loadConversations (int participantId, int offset, int count)
        throws PersistenceException
    {
        return findAll(
            ConversationRecord.class,
            new Join(ConversationRecord.CONVERSATION_ID_C, ParticipantRecord.CONVERSATION_ID_C),
            new Where(ParticipantRecord.PARTICIPANT_ID_C, participantId),
            new Limit(offset, count),
            OrderBy.descending(ConversationRecord.LAST_SENT_C));
    }

    /**
     * Loads a particular conversation record.
     */
    public ConversationRecord loadConversation (int conversationId)
        throws PersistenceException
    {
        return load(ConversationRecord.class, conversationId);
    }

    /**
     * Loads the messages in the supplied conversation (in oldest to newest order).
     */
    public List<ConvMessageRecord> loadMessages (int conversationId)
        throws PersistenceException
    {
        return findAll(ConvMessageRecord.class,
                       new Where(ConvMessageRecord.CONVERSATION_ID_C, conversationId),
                       OrderBy.ascending(ConvMessageRecord.SENT_C));
    }

    /**
     * Returns the sent time of the last message read by the specified participant in the specified
     * conversation or null if the specified member is not a participant in that conversation.
     */
    public Long loadLastRead (int conversationId, int participantId)
        throws PersistenceException
    {
        ParticipantRecord prec = load(ParticipantRecord.class,
                                      ParticipantRecord.getKey(conversationId, participantId));
        return (prec == null) ? null : prec.lastRead.getTime();
    }

    /**
     * Updates the last read timestamp for the specified member and conversation.
     */
    public void updateLastRead (int conversationId, int participantId, long lastRead)
        throws PersistenceException
    {
        updatePartial(ParticipantRecord.getKey(conversationId, participantId),
                      ParticipantRecord.LAST_READ, new Timestamp(lastRead));
    }

    /**
     * Starts a conversation with the specified subject.
     */
    public ConversationRecord startConversation (int recipientId, int authorId, String subject,
                                                 String body, MailPayload payload)
        throws PersistenceException
    {
        // TODO: do this in a transaction
        ConversationRecord conrec = new ConversationRecord();
        conrec.subject = subject;
        conrec.initiatorId = authorId;
        conrec.targetId = recipientId;
        conrec.lastSent = new Timestamp(System.currentTimeMillis());
        conrec.lastSnippet = StringUtil.truncate(body, Conversation.SNIPPET_LENGTH);
        conrec.lastAuthorId = authorId;
        insert(conrec);

        ConvMessageRecord cmrec = new ConvMessageRecord();
        cmrec.conversationId = conrec.conversationId;
        cmrec.sent = conrec.lastSent;
        cmrec.authorId = authorId;
        cmrec.body = body;
        // serialize the payload
        if (payload != null) {
            cmrec.payloadType = payload.getType();
            try {
                cmrec.payloadState =
                    JSONMarshaller.getMarshaller(payload.getClass()).getStateBytes(payload);
            } catch (Exception e) {
                throw new PersistenceException(e);
            }
        }
        insert(cmrec);

        ParticipantRecord aprec = new ParticipantRecord();
        aprec.conversationId = conrec.conversationId;
        aprec.participantId = authorId;
        aprec.lastRead = conrec.lastSent;
        insert(aprec);

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
        throws PersistenceException
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
     * having unread messages.
     */
    public boolean deleteConversation (final int conversationId, int participantId)
        throws PersistenceException
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
        if (conrec.lastSent.getTime() > parrec.lastRead.getTime()) {
            return false;
        }

        // delete our participation record
        delete(parrec);

        // if the other guy is still around, then stop here
        if (loadLastRead(conversationId, conrec.getOtherId(participantId)) != null) {
            return true;
        }

        // otherwise actually delete the contents of the conversation
        deleteAll(
            ConvMessageRecord.class,
            new Where(ConvMessageRecord.CONVERSATION_ID_C, conversationId),
            new CacheInvalidator.TraverseWithFilter<ConvMessageRecord>(ConvMessageRecord.class) {
                protected boolean testForEviction (Serializable key, ConvMessageRecord cmr) {
                    return cmr.conversationId == conversationId;
                }
            });
        delete(conrec);

        return true;
    }

    /**
     * Updates the payload state of a message.
     */
    public void updatePayloadState (int conversationId, long sent, byte[] state)
        throws PersistenceException
    {
        updatePartial(ConvMessageRecord.getKey(conversationId, new Timestamp(sent)),
                      ConvMessageRecord.PAYLOAD_STATE, state);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ConversationRecord.class);
        classes.add(ConvMessageRecord.class);
        classes.add(ParticipantRecord.class);
    }

    // TEMP
    protected static class MigratedMessage
    {
        public long sent;
        public int authorId;
        public String body;
        public int payloadType;
        public byte[] payloadState;
        public String toString () { return StringUtil.fieldsToString(this); }
    }

    protected static class MigratedConvo
    {
        public int initiatorId;
        public int targetId;
        public String subject;
        public List<MigratedMessage> messages = Lists.newArrayList();
        public String toString () { return StringUtil.fieldsToString(this); }
    }
    // END TEMP

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
