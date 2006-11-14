//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.ForUpdate;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ValueExp;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.operator.Conditionals.*;

/**
 * Manages the persistent store of mail and mailboxes.
 */
public class MailRepository extends DepotRepository
{
    public MailRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    /**
     * Fetch and return a single folder record for a given member, or null.
     */
    public MailFolderRecord getFolder (int memberId, int folderId)
        throws PersistenceException
    {
        return load(MailFolderRecord.class,
                    new Key(MailFolderRecord.OWNER_ID_C, memberId,
                            MailFolderRecord.FOLDER_ID_C, folderId));
    }
    
    /**
     * Count the number of read/unread messages in a given folder.
     */
    public Map<Boolean, Integer> getMessageCount (final int memberId, final int folderId)
        throws PersistenceException
    {
        Map<Boolean, Integer> map = new HashMap<Boolean, Integer>();
        for (MailCountRecord record :
            findAll(MailCountRecord.class,
                    new Key(MailMessageRecord.OWNER_ID_C, memberId,
                            MailMessageRecord.FOLDER_ID_C, folderId),
                    new FromOverride(MailMessageRecord.class),
                    new FieldOverride(MailCountRecord.UNREAD, MailMessageRecord.UNREAD_C),
                    new FieldOverride(MailCountRecord.COUNT, "count(*)"),
                    new GroupBy(MailMessageRecord.UNREAD_C))) {
            map.put(record.unread, record.count);
        }
        return map;
    }
    
    /**
     * Fetch and return all folder records for a given member.
     */
    public Collection<MailFolderRecord> getFolders (int memberId)
        throws PersistenceException
    {
        return findAll(MailFolderRecord.class, new Key(MailFolderRecord.OWNER_ID, memberId));
    }

    /**
     * Fetch and return a single message record in a given folder of a given member.
     */
    public MailMessageRecord getMessage (int memberId, int folderId, int messageId)
        throws PersistenceException
    {
        return load(MailMessageRecord.class,
                    new Key(MailMessageRecord.OWNER_ID_C, memberId,
                            MailMessageRecord.FOLDER_ID_C, folderId,
                            MailMessageRecord.MESSAGE_ID_C, messageId));
    }
    
    /**
     * Fetch and return all message records in a given folder of a given member.
     * 
     * TODO: If messages end up being non-trivial in size, separate into own table.
     */
     public Collection<MailMessageRecord> getMessages (int memberId, int folderId)
         throws PersistenceException
     {
         return findAll(MailMessageRecord.class,
                        new Key(MailMessageRecord.OWNER_ID_C, memberId,
                                MailMessageRecord.FOLDER_ID_C, folderId));
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
      * Insert a message into the database, for a given member and folder. This method
      * fills in the messageId field with a new value that's unique within the folder.
      */
     public MailMessageRecord fileMessage (MailMessageRecord record)
         throws PersistenceException
     {
         record.messageId = claimMessageId(record.ownerId, record.folderId, 1);
         insert(record);
         return record;
     }
     
     /**
      * Move a message from one folder to another. TODO: Bulk move, see deleteMessage.
      */
     public void moveMessage (int ownerId, int folderId, int newFolderId, int... messageIds)
         throws PersistenceException
     {
         Comparable[] idArr = new Comparable[messageIds.length];
         for (int ii = 0; ii < messageIds.length; ii ++) {
             idArr[ii] = new Integer(messageIds[ii]);
         }
         int newId = claimMessageId(ownerId, newFolderId, 1);
         updatePartial(MailMessageRecord.class,
             new Where(new And(
                 new Equals(MailMessageRecord.OWNER_ID_C, new ValueExp(ownerId)),
                 new Equals(MailMessageRecord.FOLDER_ID_C, new ValueExp(folderId)),
                 new In(MailMessageRecord.MESSAGE_ID_C, idArr))),
                 MailMessageRecord.FOLDER_ID, newFolderId,
                 MailMessageRecord.MESSAGE_ID, newId);
     }

     /**
      * Delete a message record.
      */
     public void deleteMessage (int ownerId, int folderId, int... messageIds)
         throws PersistenceException
     {
         if (messageIds.length == 0) {
             return;
         }
         Comparable[] idArr = new Comparable[messageIds.length];
         for (int ii = 0; ii < messageIds.length; ii ++) {
             idArr[ii] = new Integer(messageIds[ii]);
         }
         deleteAll(MailMessageRecord.class,
             new Where(new And(
                 new Equals(MailMessageRecord.OWNER_ID_C, new ValueExp(ownerId)),
                 new Equals(MailMessageRecord.FOLDER_ID_C, new ValueExp(folderId)),
                 new In(MailMessageRecord.MESSAGE_ID_C, idArr))));
     }

     public void setBodyObjectState (int ownerId, int folderId, int messageId, byte[] state)
         throws PersistenceException
     {
         updatePartial(MailMessageRecord.class,
             new Key(MailMessageRecord.OWNER_ID_C, ownerId,
                     MailMessageRecord.FOLDER_ID_C, folderId,
                     MailMessageRecord.MESSAGE_ID_C, messageId),
             MailMessageRecord.BODY_OBJECT_STATE, state);
     }
     
    /**
      * Flag a message as being unread (or not).
      */
     public void setUnread (int ownerId, int folderId, int messageId, boolean unread)
         throws PersistenceException
     {
         updatePartial(MailMessageRecord.class,
                       new Key(MailMessageRecord.OWNER_ID_C, ownerId,
                               MailMessageRecord.FOLDER_ID_C, folderId,
                               MailMessageRecord.MESSAGE_ID_C, messageId),
                       MailMessageRecord.UNREAD, unread);
     }


     // claim space in a folder to deliver idCount messages; returns the first usable id
     protected int claimMessageId (int memberId, int folderId, int idCount)
         throws PersistenceException
     {
         MailFolderRecord record = load(MailFolderRecord.class,
                                        new Key(MailFolderRecord.OWNER_ID_C, memberId,
                                                MailFolderRecord.FOLDER_ID_C, folderId),
                                        new ForUpdate());
         int firstId = record.nextMessageId;
         record.nextMessageId += idCount;
         update(record, MailFolderRecord.NEXT_MESSAGE_ID);
         return firstId;
     }

}
