//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.MultiKey;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FieldDefinition;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.msoy.web.data.MailFolder;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of mail and mailboxes.
 */
public class MailRepository extends DepotRepository
{
    public MailRepository (PersistenceContext ctx)
    {
        super(ctx);
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
     * Count the number of read/unread messages in a given folder and return
     * a Tuple<Integer, Integer> with the number of read messages in the left
     * spot and the unread ones in the right.
     */
    public Tuple<Integer, Integer> getMessageCount (final int memberId, final int folderId)
        throws PersistenceException
    {
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
     *
     * TODO: If messages end up being non-trivial in size, separate into own table.
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
        Key<MailMessageRecord> key = MailMessageRecord.getKey(messageId, folderId, ownerId);
        updatePartial(MailMessageRecord.class, key, key, MailMessageRecord.PAYLOAD_STATE, state);
    }

    /**
     * Flag a message as being unread (or not).
     */
    public void setUnread (int ownerId, int folderId, int messageId, boolean unread)
        throws PersistenceException
    {
        Key<MailMessageRecord> key = MailMessageRecord.getKey(messageId, folderId, ownerId);
        updatePartial(MailMessageRecord.class, key, key, MailMessageRecord.UNREAD, unread);
    }

    // claim space in a folder to deliver idCount messages; returns the first usable id
    protected int claimMessageId (int memberId, int folderId, int idCount)
        throws PersistenceException
    {
        // TODO: When we have just a little more time, do this with fancy lockless magic
        // TODO: like UPDATE set NEXT=NEXT+1 where NEXT=122 and iterate until rows modified.
        MailFolderRecord record = load(MailFolderRecord.class,
                                       MailFolderRecord.OWNER_ID, memberId,
                                       MailFolderRecord.FOLDER_ID, folderId);
        if (record == null) {
            return -1;
        }

        int firstId = record.nextMessageId;
        record.nextMessageId += idCount;
        update(record, MailFolderRecord.NEXT_MESSAGE_ID);
        return firstId;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MailMessageRecord.class);
        classes.add(MailFolderRecord.class);
    }
}
