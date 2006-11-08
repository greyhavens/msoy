//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.Query;
import com.samskivert.jdbc.depot.clause.ForUpdate;

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
     * Count the number of read/unread messages in a given folder.
     */
    public Map<Boolean, Integer> getMessageCount (final int memberId, final int folderId)
        throws PersistenceException
    {
        final String tableName = _ctx.getMarshaller(MailMessageRecord.class).getTableName();
        return _ctx.invoke(new Query<Map<Boolean, Integer>>(_ctx, null, null) {
            public Map<Boolean, Integer> invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = conn.prepareStatement(
                    "   select count(*), " + MailMessageRecord.UNREAD +
                    "     from " + tableName +
                    "    where " + MailMessageRecord.OWNER_ID + " = ? " +
                    "      and " + MailMessageRecord.FOLDER_ID + " = ? " +
                    " group by " + MailMessageRecord.UNREAD);
                Map<Boolean, Integer> map = new HashMap<Boolean, Integer>();
                try {
                    stmt.setInt(1, memberId);
                    stmt.setInt(2, folderId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        Integer count = rs.getInt(1);
                        Boolean unread = rs.getBoolean(2);
                        map.put(unread, count);
                    }
                    rs.close();
                    return map;

                } finally {
                    stmt.close();
                }
            }
        });
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
     * 
     * TODO: If messages end up being non-trivial in size, separate into own table.
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

     /**
      * Flag a message as being unread (or not).
      */
     public void setUnread (int ownerId, int folderId, int messageId, boolean unread)
         throws PersistenceException
     {
         updatePartial(MailMessageRecord.class,
                       new Key(MailMessageRecord.OWNER_ID, ownerId,
                               MailMessageRecord.FOLDER_ID, folderId,
                               MailMessageRecord.MESSAGE_ID, messageId),
                       MailMessageRecord.UNREAD, unread);
     }


     // claim space in a folder to deliver idCount messages; returns the first usable id
     protected int claimMessageId (int memberId, int folderId, int idCount)
         throws PersistenceException
     {
         MailFolderRecord record = load(MailFolderRecord.class,
                                        new Key(MailFolderRecord.MEMBER_ID, memberId,
                                                MailFolderRecord.FOLDER_ID, folderId),
                                        new ForUpdate());
         int firstId = record.nextMessageId;
         record.nextMessageId += idCount;
         update(record, MailFolderRecord.NEXT_MESSAGE_ID);
         return firstId;
     }

}
