//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.Collection;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.clause.ForUpdateClause;

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
                    new Key(MailFolderRecord.MEMBER_ID, memberId,
                            MailFolderRecord.FOLDER_ID, folderId));
    }
    
    /**
     * Fetch and return all folder records for a given member.
     */
    public Collection<MailFolderRecord> getFolders (int memberId)
        throws PersistenceException
    {
        return findAll(MailFolderRecord.class, new Key(MailFolderRecord.MEMBER_ID, memberId));
    }

    /**
     * Fetch and return a single message record in a given folder of a given member.
     */
    public MailMessageRecord getMessage (int memberId, int folderId, int messageId)
        throws PersistenceException
    {
        return load(MailMessageRecord.class,
                    new Key(MailMessageRecord.OWNER_ID, memberId,
                            MailMessageRecord.FOLDER_ID, folderId,
                            MailMessageRecord.MESSAGE_ID, messageId));
    }
    
    /**
     * Fetch and return all message records in a given folder of a given member.
     */
     public Collection<MailMessageRecord> getMessages (int memberId, int folderId)
         throws PersistenceException
     {
         return findAll(MailMessageRecord.class,
                        new Key(MailMessageRecord.OWNER_ID, memberId,
                                MailMessageRecord.FOLDER_ID, folderId));
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
     public void moveMessage (int ownerId, int folderId, int messageId, int newFolderId)
         throws PersistenceException
     {
         int newId = claimMessageId(ownerId, newFolderId, 1);
         updatePartial(MailMessageRecord.class,
                       new Key(MailMessageRecord.OWNER_ID, ownerId,
                               MailMessageRecord.FOLDER_ID, folderId,
                               MailMessageRecord.MESSAGE_ID, messageId),
                       MailMessageRecord.FOLDER_ID, newFolderId,
                       MailMessageRecord.MESSAGE_ID, newId);
     }

     /**
      * Delete a message record.
      * 
      * TODO: For bulk deletion support, add 'IN (1, 3, 7)' support to Depot.
      */
     public void deleteMessage (int memberId, int folderId, int messageId)
         throws PersistenceException
     {
         deleteAll(MailMessageRecord.class,
                   new Key(MailMessageRecord.OWNER_ID, memberId,
                           MailMessageRecord.FOLDER_ID, folderId,
                           MailMessageRecord.MESSAGE_ID, messageId));
     }

     // claim space in a folder to deliver idCount messages; returns the first usable id
     protected int claimMessageId (int memberId, int folderId, int idCount)
         throws PersistenceException
     {
         MailFolderRecord record = load(MailFolderRecord.class,
                                        new Key(MailFolderRecord.MEMBER_ID, memberId,
                                                MailFolderRecord.FOLDER_ID, folderId),
                                        new ForUpdateClause());
         int firstId = record.nextMessageId;
         record.nextMessageId += idCount;
         update(record, MailFolderRecord.NEXT_MESSAGE_ID);
         return firstId;
     }

}
