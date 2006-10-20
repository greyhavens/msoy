//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.Collection;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;

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
                    new Key(MailMessageRecord.MEMBER_ID, memberId,
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
                        new Key(MailMessageRecord.MEMBER_ID, memberId,
                                MailMessageRecord.FOLDER_ID, folderId));
     }

     /**
      * Insert a message into the database, for a given member and folder. This method
      * fills in the messageId field with a new value that's unique within the folder.
      */
     public MailMessageRecord deliverMail(MailMessageRecord record)
         throws PersistenceException
     {
         record.folderId = claimMessageId(record.memberId, record.folderId, 1);
         insert(record);
         return record;
     }
     
     /**
      * Move a message from one folder to another. TODO: Bulk move?
      */
     public void moveMessage (MailMessageRecord record, int newFolderId)
         throws PersistenceException
     {
         int newId = claimMessageId(record.memberId, newFolderId, 1);
         record.folderId = newFolderId;
         record.messageId = newId;
         update(record, MailMessageRecord.FOLDER_ID, MailMessageRecord.MESSAGE_ID);
     }

     /**
      * Delete a message record.
      */
     public void deleteMessage (MailMessageRecord record)
         throws PersistenceException
     {
         delete(record);
     }

     // claim space in a folder to deliver idCount messages; returns the first usable id
     protected int claimMessageId (int memberId, int folderId, int idCount)
         throws PersistenceException
     {
         // TODO: We need SELECT ... FOR UPDATE support.
         MailFolderRecord record = load(MailFolderRecord.class,
                                        new Key(MailFolderRecord.MEMBER_ID, memberId,
                                                MailFolderRecord.FOLDER_ID, folderId));
         int firstId = record.nextMessageId;
         record.nextMessageId += idCount;
         update(record, MailFolderRecord.NEXT_MESSAGE_ID);
         return firstId;
     }
}
