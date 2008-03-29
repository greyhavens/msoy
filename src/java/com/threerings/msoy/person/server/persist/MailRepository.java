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
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntSet;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.MultiKey;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.*;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.operator.Conditionals.*;

import com.threerings.msoy.person.data.Conversation;
import com.threerings.msoy.person.data.MailFolder;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.CountRecord;

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
    public ConversationRecord startConversation (String subject, int recipientId, int authorId,
                                                 String body, int payloadType, byte[] payloadState)
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
        cmrec.payloadType = payloadType;
        cmrec.payloadState = payloadState;
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
     * Fetch and return a single folder record for a given member, or null.
     */
    public MailFolderRecord getFolder (int memberId, int folderId)
        throws PersistenceException
    {
        return load(MailFolderRecord.class,
                    MailFolderRecord.OWNER_ID, memberId,
                    MailFolderRecord.FOLDER_ID, folderId);
    }

    /**
     * Count the number of read/unread messages in a given folder and return a Tuple<Integer,
     * Integer> with the number of read messages in the left spot and the unread ones in the right.
     */
    public Tuple<Integer, Integer> getMessageCount (final int memberId, final int folderId)
        throws PersistenceException
    {
        // make sure MailMessageRecord is resolved (TODO: depot should do this automatically based
        // on the @Computed(shadowOf=class) annotation)
        _ctx.getMarshaller(MailMessageRecord.class);

        int read = 0, unread = 0;
        List<MailCountRecord> records = findAll(
            MailCountRecord.class,
            new Where(MailMessageRecord.OWNER_ID_C, memberId,
                      MailMessageRecord.FOLDER_ID_C, folderId),
            new GroupBy(MailMessageRecord.UNREAD_C));
        for (MailCountRecord record : records) {
            if (record.unread) {
                unread = record.count;
            } else {
                read = record.count;
            }
        }
        return new Tuple<Integer, Integer>(read, unread);
    }

    /**
     * Fetch and return all folder records for a given member.
     */
    public List<MailFolderRecord> getFolders (int memberId)
        throws PersistenceException
    {
        return findAll(MailFolderRecord.class, new Where(MailFolderRecord.OWNER_ID_C, memberId));
    }

    /**
     * Fetch and return a single message record in a given folder of a given member.
     */
    public MailMessageRecord getMessage (int memberId, int folderId, int messageId)
        throws PersistenceException
    {
        return load(MailMessageRecord.class,
                    MailMessageRecord.OWNER_ID, memberId,
                    MailMessageRecord.FOLDER_ID, folderId,
                    MailMessageRecord.MESSAGE_ID, messageId);
    }

    /**
     * Fetch and return all message records in a given folder of a given member.
     */
    public List<MailMessageRecord> getMessages (int memberId, int folderId)
        throws PersistenceException
    {
        return findAll(MailMessageRecord.class,
                       new Where(MailMessageRecord.OWNER_ID_C, memberId,
                                 MailMessageRecord.FOLDER_ID_C, folderId),
                       OrderBy.descending(MailMessageRecord.SENT_C));
    }

    /**
     * Fetch and return all message records in a given folder of a given member for whom the other
     * party is the specified id.
     */
    public List<MailMessageRecord> getMessages (int memberId, int folderId, int otherId)
        throws PersistenceException
    {
        return findAll(
            MailMessageRecord.class,
            new Where(new And(new Equals(MailMessageRecord.OWNER_ID_C, memberId),
                              new Equals(MailMessageRecord.FOLDER_ID_C, folderId),
                              new Or(new Equals(MailMessageRecord.SENDER_ID_C, otherId),
                                     new Equals(MailMessageRecord.RECIPIENT_ID_C, otherId)))),
            OrderBy.descending(MailMessageRecord.SENT_C));
    }

    /**
     * Insert a new folder record into the database.
     */
    public MailFolderRecord createFolder (MailFolderRecord record)
        throws PersistenceException
    {
        insert(record);
        return record;
    }

    /**
     * Deliver a message by filing it in the recipient's and the sender's in- and sent boxes
     * respectively, with ownerId set appropriately. This method modifies the record.
     */
    public void deliverMessage (MailMessageRecord record)
        throws PersistenceException
    {
        record.sent = new Timestamp(System.currentTimeMillis());

        // file one copy for ourselves
        if (record.senderId != 0) {
            record.ownerId = record.senderId;
            record.folderId = MailFolder.SENT_FOLDER_ID;
            record.unread = false;
            fileMessage(record);
        }

        // and make a copy for the recipient
        record.ownerId = record.recipientId;
        record.folderId = MailFolder.INBOX_FOLDER_ID;
        record.unread = true;
        fileMessage(record);

        _eventLog.mailSent(record.senderId, record.recipientId, record.payloadType);
    }

    /**
     * Insert a message into the database, for a given member and folder. This method
     * fills in the messageId field with a new value that's unique within the folder.
     */
    public MailMessageRecord fileMessage (MailMessageRecord record)
        throws PersistenceException
    {
        record.messageId = claimMessageId(record.ownerId, record.folderId, 1);
        if (record.messageId < 0) {
            log.warning("Failed to file message, unable to obtain message id " + record + ".");
            return null;
        }
        insert(record);
        return record;
    }

    /**
     * Moves a message from one folder to another.
     */
    public void moveMessage (int ownerId, int folderId, int newFolderId, int[] messageIds)
        throws PersistenceException
    {
        Comparable[] idArr = IntListUtil.box(messageIds);
        int newId = claimMessageId(ownerId, newFolderId, messageIds.length);
        if (newId < 0) {
            log.warning("Failed to move message, unable to obtain message id [oid=" + ownerId +
                        ", fid=" + folderId + ", nfid=" + newFolderId +
                        ", ids=" + StringUtil.toString(messageIds) + "].");
            return;
        }

        MultiKey<MailMessageRecord> key = new MultiKey<MailMessageRecord>(
            MailMessageRecord.class,
            MailMessageRecord.OWNER_ID, ownerId,
            MailMessageRecord.FOLDER_ID, folderId,
            MailMessageRecord.MESSAGE_ID, idArr);
        updatePartial(MailMessageRecord.class, key, key,
                      MailMessageRecord.FOLDER_ID, newFolderId,
                      MailMessageRecord.MESSAGE_ID, newId++);
    }

    /**
     * Delete one or more message records.
     */
    public void deleteMessage (int ownerId, int folderId, int... messageIds)
        throws PersistenceException
    {
        if (messageIds.length == 0) {
            return;
        }
        Comparable[] idArr = IntListUtil.box(messageIds);
        MultiKey<MailMessageRecord> key = new MultiKey<MailMessageRecord>(
            MailMessageRecord.class,
            MailMessageRecord.OWNER_ID, ownerId,
            MailMessageRecord.FOLDER_ID, folderId,
            MailMessageRecord.MESSAGE_ID, idArr);
        deleteAll(MailMessageRecord.class, key, key);
    }

    /**
     * Set the payload state of a message in the persistent store.
     */
    public void setPayloadState (int ownerId, int folderId, int messageId, byte[] state)
        throws PersistenceException
    {
        updatePartial(MailMessageRecord.getKey(messageId, folderId, ownerId),
                      MailMessageRecord.PAYLOAD_STATE, state);
    }

    /**
     * Flags messages as being unread (or not).
     */
    public void setUnread (int ownerId, int folderId, IntSet messageIds, boolean unread)
        throws PersistenceException
    {
        // TODO: do this in one query
        for (int messageId : messageIds) {
            updatePartial(MailMessageRecord.getKey(messageId, folderId, ownerId),
                          MailMessageRecord.UNREAD, unread);
        }
    }

    // claim space in a folder to deliver idCount messages; returns the first usable id
    protected int claimMessageId (int memberId, int folderId, int idCount)
        throws PersistenceException
    {
        int firstId, rows;
        int attempts = 10;
        do {
            // sanity check
            if (--attempts < 0) {
                throw new PersistenceException(
                    "Failed to claim new ID for message delivery in 10 attempts [memberId=" +
                    memberId + ", folderId=" + folderId + ", idCount=" + idCount + "]");
            }
            Key<MailFolderRecord> key = MailFolderRecord.getKey(folderId, memberId);

            // find the next available message ID
            MailFolderRecord record = load(MailFolderRecord.class, key);
            if (record == null) {
                return -1;
            }
            firstId = record.nextMessageId;

            // attempt to claim our message id block with a lock-less database update
            rows = updatePartial(
                MailFolderRecord.class,
                new Where(new And(key.condition,
                                  new Equals(MailFolderRecord.NEXT_MESSAGE_ID_C, firstId))),
                key,
                MailFolderRecord.NEXT_MESSAGE_ID, firstId + idCount);
        } while (rows == 0);

        return firstId;
    }

    // TEMP
    protected void migrateToConversations ()
        throws PersistenceException
    {
        // there are about 50k messages on production right now, so we can handle loading
        // everything into memory and being more sophisticated about our converstion
        int migrated = 0, duplicates = 0;
        Map<String,MigratedConvo> convos = Maps.newHashMap();
      SCAN:
        for (MailMessageRecord msg : findAll(MailMessageRecord.class)) {
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
        classes.add(MailMessageRecord.class);
        classes.add(MailFolderRecord.class);
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
}
