//
// $Id$

package com.threerings.msoy.mail.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.mail.gwt.Conversation;

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
     * Returns the number of conversations in which the specified member is a participant and which
     * have messages not read by that member.
     */
    public int loadUnreadConvoCount (int memberId, Iterable<Integer> muted)
    {
        SQLExpression<?> isMe = ParticipantRecord.PARTICIPANT_ID.eq(memberId);
        SQLExpression<?> isNew =
            ConversationRecord.LAST_SENT.greaterThan(ParticipantRecord.LAST_READ);
        return load(CountRecord.class,
                    new FromOverride(ParticipantRecord.class),
                    ParticipantRecord.CONVERSATION_ID.join(ConversationRecord.CONVERSATION_ID),
                    new Where(Ops.and(isMe, isNew, nonMuted(muted)))).count;
    }

    /**
     * Loads conversations in which the specified member is a participant, sorted by most recently
     * active to least.
     */
    public List<ConversationRecord> loadConversations (
        int participantId, Iterable<Integer> muted, int offset, int count)
    {
        SQLExpression<?> isMe = ParticipantRecord.PARTICIPANT_ID.eq(participantId);
        return findAll(ConversationRecord.class, CacheStrategy.RECORDS, Lists.newArrayList(
            ConversationRecord.CONVERSATION_ID.join(ParticipantRecord.CONVERSATION_ID),
            new Where(Ops.and(isMe, nonMuted(muted))),
            new Limit(offset, count),
            OrderBy.descending(ConversationRecord.LAST_SENT)));
    }

    /**
     * Loads a particular conversation record.
     */
    public ConversationRecord loadConversation (int conversationId)
    {
        return load(ConversationRecord.getKey(conversationId));
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

    public ConvMessageRecord loadMessage (int conversationId, long sent)
    {
        return load(ConvMessageRecord._R,
            ConvMessageRecord.getKey(conversationId, new Timestamp(sent)));
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
    public ConversationRecord startConversation (
        int recipientId, int authorId, String subject, String body, ConvMessageRecord cmrec,
        boolean authorParticipation, boolean recipParticipation)
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

        if (recipParticipation) {
            ParticipantRecord rprec = new ParticipantRecord();
            rprec.conversationId = conrec.conversationId;
            rprec.participantId = recipientId;
            rprec.lastRead = new Timestamp(0);
            insert(rprec);
        }

        return conrec;
    }

    /**
     * Adds a message to a conversation.
     */
    public void addMessage (ConversationRecord conrec, int authorId, String body,
                            ConvMessageRecord record, boolean notifyRecip)
    {
        record.conversationId = conrec.conversationId;
        record.sent = new Timestamp(System.currentTimeMillis());
        record.authorId = authorId;
        record.body = body;
        insert(record);

        // update the conversation record
        updatePartial(ConversationRecord.getKey(conrec.conversationId),
                      ConversationRecord.LAST_SENT, record.sent,
                      ConversationRecord.LAST_AUTHOR_ID, authorId,
                      ConversationRecord.LAST_SNIPPET,
                      StringUtil.truncate(body, Conversation.SNIPPET_LENGTH));

        if (notifyRecip) {
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
        }
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
        ConversationRecord conrec = load(ConversationRecord.getKey(conversationId));
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
    public List<ConversationComplaintRecord> loadComplaints (int convoId)
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

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        // note: this may leave some stale conversations around if these members were the only one
        // who had participant records for their convos; since we only purge permaguests (who can't
        // converse) this is a non-issue for now
        deleteAll(ParticipantRecord.class,
                  new Where(ParticipantRecord.PARTICIPANT_ID.in(memberIds)));
        deleteAll(ConversationComplaintRecord.class,
                  new Where(ConversationComplaintRecord.COMPLAINER_ID.in(memberIds)));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ConversationRecord.class);
        classes.add(ConvMessageRecord.class);
        classes.add(ParticipantRecord.class);
        classes.add(ConversationComplaintRecord.class);
    }

    protected static SQLExpression<?> nonMuted (Iterable<Integer> muted)
    {
        return Ops.not(Ops.or(
            ConversationRecord.TARGET_ID.in(muted),
            ConversationRecord.INITIATOR_ID.in(muted)));
    }
}
