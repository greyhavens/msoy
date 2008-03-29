//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.StringUtil;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.MultiKey;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.util.JSONMarshaller;

import com.threerings.msoy.person.data.Conversation;
import com.threerings.msoy.person.data.GameAwardPayload;
import com.threerings.msoy.person.data.MailPayload;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of mail and mailboxes.
 */
public class MailRepository extends DepotRepository
{
    public MailRepository (PersistenceContext ctx, MsoyEventLogger eventLog)
    {
        super(ctx);

        _eventLog = eventLog;

        // TEMP (3-27-08): if we have no conversation records, migrate our mail message records
        try {
            if (load(CountRecord.class, new FromOverride(ConversationRecord.class)).count == 0) {
                migrateToConversations();
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Conversation migration failed.", pe);
        }
        // END TEMP
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
     * Loads the messages in the supplied conversation.
     */
    public List<ConvMessageRecord> loadMessages (int conversationId)
        throws PersistenceException
    {
        return findAll(ConvMessageRecord.class,
                       new Where(ConvMessageRecord.CONVERSATION_ID_C, conversationId),
                       OrderBy.descending(ConvMessageRecord.SENT_C));
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
    public ConvMessageRecord addMessage (int conversationId, int authorId, String body,
                                         int payloadType, byte[] payloadState)
        throws PersistenceException
    {
        ConvMessageRecord record = new ConvMessageRecord();
        record.conversationId = conversationId;
        record.sent = new Timestamp(System.currentTimeMillis());
        record.authorId = authorId;
        record.body = body;
        record.payloadType = payloadType;
        record.payloadState = payloadState;
        insert(record);

        // update the conversation record
        updatePartial(ConversationRecord.class, conversationId,
                      ConversationRecord.LAST_SENT, record.sent,
                      ConversationRecord.LAST_AUTHOR_ID, authorId,
                      ConversationRecord.LAST_SNIPPET,
                      StringUtil.truncate(body, Conversation.SNIPPET_LENGTH));

        // note that we added a message to a conversation
        _eventLog.mailSent(conversationId, authorId, payloadType);

        return record;
    }

    /**
     * Returns the number of unread messages in all conversations for the specified member.
     */
    public int getUnreadMessages (int memberId)
        throws PersistenceException
    {
        return 1; // TODO
    }

    // TEMP
    protected void migrateToConversations ()
        throws PersistenceException
    {
        // there are about 50k messages on production right now, so we can handle loading
        // everything into memory and being more sophisticated about our converstion

        // we have to load these less than Short.MAX_VALUE at a time to avoid triggering a Postgres
        // JDBC driver bug
        List<MailMessageRecord> msgrecs = Lists.newArrayList(), batch;
        do {
            batch = findAll(MailMessageRecord.class, new Limit(msgrecs.size(), Short.MAX_VALUE));
            msgrecs.addAll(batch);
        } while (batch.size() == Short.MAX_VALUE);

        int migrated = 0, duplicates = 0;
        Map<String,MigratedConvo> convos = Maps.newHashMap();
      SCAN:
        for (MailMessageRecord msg : msgrecs) {
            String subject = msg.subject;
            if (subject.toLowerCase().startsWith("re: ")) {
                subject = subject.substring(4);
            }

            int lesser = Math.min(msg.senderId, msg.recipientId);
            int greater = Math.max(msg.senderId, msg.recipientId);
            String key = lesser + ":" + greater + ":" + subject;
            MigratedConvo convo = convos.get(key);
            if (convo == null) {
                convos.put(key, convo = new MigratedConvo());
                convo.subject = subject;
            }

            // make sure this message is not already added to the conversation (the sender and
            // recipient might both have a copy)
            for (MigratedMessage mmsg : convo.messages) {
                if (mmsg.authorId == msg.senderId && ObjectUtil.equals(mmsg.body, msg.bodyText)) {
                    // log.info("Skipping " + msg.senderId + " " + msg.sent + " " + subject + ".");
                    duplicates++;
                    continue SCAN;
                }
            }

            migrated++;
            MigratedMessage mmsg = new MigratedMessage();
            mmsg.sent = msg.sent.getTime();
            mmsg.authorId = msg.senderId;
            mmsg.body = (msg.bodyText == null) ? "" : msg.bodyText;
            mmsg.payloadType = msg.payloadType;
            mmsg.payloadState = msg.payloadState;
            convo.messages.add(mmsg);
        }

        // now create the appropriate conversation and friends records
        for (MigratedConvo convo : convos.values()) {
            try {
                MigratedMessage latest = null;
                IntSet participantIds = new ArrayIntSet();
                for (MigratedMessage msg : convo.messages) {
                    participantIds.add(msg.authorId);
                    if (latest == null || latest.sent < msg.sent) {
                        latest = msg;
                    }
                }

                ConversationRecord conrec = new ConversationRecord();
                conrec.subject = convo.subject;
                conrec.lastSent = new Timestamp(latest.sent);
                conrec.lastSnippet = StringUtil.truncate(latest.body, Conversation.SNIPPET_LENGTH);
                conrec.lastAuthorId = latest.authorId;
                insert(conrec);

                for (MigratedMessage msg : convo.messages) {
                    ConvMessageRecord record = new ConvMessageRecord();
                    record.conversationId = conrec.conversationId;
                    record.sent = new Timestamp(msg.sent);
                    record.authorId = msg.authorId;
                    record.body = msg.body;
                    record.payloadType = msg.payloadType;
                    record.payloadState = msg.payloadState;
                    insert(record);
                }

                for (int participantId : participantIds) {
                    ParticipantRecord prec = new ParticipantRecord();
                    prec.conversationId = conrec.conversationId;
                    prec.participantId = participantId;
                    prec.lastRead = new Timestamp(0);
                    insert(prec);
                }

            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to migrate conversation " + convo + ".", pe);
            }
        }

        log.info("Converted " + migrated + " messages into " + convos.size() + " conversations. " +
                 "Skipped " + duplicates + " duplicate messages.");
    }
    // END TEMP

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
//         classes.add(MailMessageRecord.class);
//         classes.add(MailFolderRecord.class);
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
        public String subject;
        public List<MigratedMessage> messages = Lists.newArrayList();
        public String toString () { return StringUtil.fieldsToString(this); }
    }
    // END TEMP

    protected MsoyEventLogger _eventLog;

    static {
        // register a migration for TrophyAwardPayload -> GameAwardPayload
        Map<String, String> migmap = Maps.newHashMap();
        migmap.put("trophyName", "awardName");
        migmap.put("trophyMedia", "awardMediaHash");
        migmap.put("trophyMimeType", "awardMimeType");
        JSONMarshaller.registerMigration(GameAwardPayload.class, migmap);
    }
}
