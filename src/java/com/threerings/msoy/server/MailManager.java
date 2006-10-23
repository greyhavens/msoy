//
// $Id$

package com.threerings.msoy.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.util.ResultListener;
import com.threerings.msoy.server.persist.MailFolderRecord;
import com.threerings.msoy.server.persist.MailMessageRecord;
import com.threerings.msoy.server.persist.MailRepository;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MemberGName;

/**
 * Manage msoy mail.
 */
public class MailManager
{
    /**
     * Prepares our mail manager for operation.
     */
    public void init (MailRepository mailRepo, MemberRepository memberRepo)
    {
        _mailRepo = mailRepo;
        _memberRepo = memberRepo;
    }

    /**
     * Fetch and return a single message from the database. 
     */
    public void getMessage (final int memberId, final int folderId, final int messageId,
                            ResultListener<MailMessage> waiter)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<MailMessage>(waiter) {
            public MailMessage invokePersistResult () throws PersistenceException {
                return toWebObject(_mailRepo.getMessage(memberId, folderId, messageId));
            }
        });
    }

    /**
     * Fetch and return all the messages in a folder from the database. 
     */
    public void getMessages (final int memberId, final int folderId,
                             ResultListener<List<MailMessage>> waiter)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<List<MailMessage>>(waiter) {
            public List<MailMessage> invokePersistResult () throws PersistenceException {
                List<MailMessage> result = new ArrayList<MailMessage>();
                for (MailMessageRecord record : _mailRepo.getMessages(memberId, folderId)) {
                    result.add(toWebObject(record));
                }
                return result;
            }
        });
    }

    /**
     * Fetch and return a single folder from the database. 
     */
    public void getFolder (final int memberId, final int folderId,
                           ResultListener<MailFolder> waiter)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<MailFolder>(waiter) {
            public MailFolder invokePersistResult () throws PersistenceException {
                return toWebObject(_mailRepo.getFolder(memberId, folderId));
            }
        });
    }

    /**
     * Fetch and return all of a given member's folders from the database.
     */
    public void getFolders (final int memberId, ResultListener<List<MailFolder>> waiter)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<List<MailFolder>>(waiter) {
            public List<MailFolder> invokePersistResult () throws PersistenceException {
                List<MailFolder> result = new ArrayList<MailFolder>();
                for (MailFolderRecord record : _mailRepo.getFolders(memberId)) {
                    result.add(toWebObject(record));
                }
                return result;
            }
        });
    }

    /**
     * Deliver a message, i.e. file one copy of it in the sender's 'Sent' folder,
     * and one copy in the recipient's 'Inbox' folder.
     */    
    public void deliverMessage (final MailMessage msg, ResultListener<Void> waiter)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Void>(waiter) {
            public Void invokePersistResult () throws PersistenceException {
                testFolders(msg.recipient.memberId);
                testFolders(msg.sender.memberId);
                
                // copy the mail message into record format
                MailMessageRecord record = new MailMessageRecord();
                record.senderId = msg.sender.memberId;
                record.recipientId = msg.recipient.memberId;
                record.subject = msg.subject;
                record.message = msg.message;
                record.sent = new Timestamp(System.currentTimeMillis());

                // file one copy for ourselves
                record.ownerId = msg.sender.memberId;
                record.folderId = MailFolder.SENT_FOLDER_ID;
                _mailRepo.fileMessage(record);
                
                // and one for the recipient (safely reusing the record object)
                record.ownerId = msg.recipient.memberId;
                record.folderId = MailFolder.INBOX_FOLDER_ID;
                _mailRepo.fileMessage(record);
                return null;
            }
        });
    }

    /**
     * Move some messages from one folder to another.
     * 
     * TODO: We should not need to iterate here.
     */
    public void moveMessages (final int memberId, final int folderId, final int[] msgIdArr,
                              final int newFolderId, ResultListener<Void> waiter)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(waiter) {
            public Void invokePersistResult () throws PersistenceException {
                for (int msgId : msgIdArr) {
                    _mailRepo.moveMessage(memberId, folderId, msgId, newFolderId);
                }
                return null;
            }
        });

    }
    
    /**
     * Bulk delete a number of messages from the database. Note: This actually
     * DELETES the messages, it doesn't move them to the Trash folder.
     * 
     * TODO: We should not need to iterate here.
     */
    public void deleteMessages (final int memberId, final int folderId, final int[] msgIdArr,
                                ResultListener<Void> waiter)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(waiter) {
            public Void invokePersistResult () throws PersistenceException {
                for (int msgId : msgIdArr) {
                    _mailRepo.deleteMessage(memberId, folderId, msgId);
                }
                return null;
            }
        });
    }

    // initialize a member's folder structure, if necessary
    protected void testFolders (int memberId)
        throws PersistenceException
    {
        if (_mailRepo.getFolders(memberId).size() == 0) {
            MailFolderRecord record = new MailFolderRecord();
            record.ownerId = memberId;

            record.folderId = MailFolder.INBOX_FOLDER_ID;
            record.name = "Inbox";
            _mailRepo.createFolder(record);
            
            record.folderId = MailFolder.TRASH_FOLDER_ID;
            record.name = "Trash";
            _mailRepo.createFolder(record);

            record.folderId = MailFolder.SENT_FOLDER_ID;
            record.name = "Sent";
            _mailRepo.createFolder(record);
        }
    }

    // convert a MailMessageRecord to its MailMessage form
    protected MailMessage toWebObject (MailMessageRecord record)
        throws PersistenceException
    {
        MailMessage message = new MailMessage();
        message.messageId = record.messageId;
        message.folderId = record.folderId;
        message.ownerId = record.ownerId;
        message.subject = record.subject;
        message.sent = new Date(record.sent.getTime());
        message.message = record.message;
        
        MemberRecord memRec = _memberRepo.loadMember(record.senderId);
        message.sender = new MemberGName(memRec.name, memRec.memberId);

        memRec = _memberRepo.loadMember(record.recipientId);
        message.recipient = new MemberGName(memRec.name, memRec.memberId);
        return message;
    }

    // convert a MailFolderRecord to its MailFolder form
    protected MailFolder toWebObject (MailFolderRecord record)
        throws PersistenceException
    {
        MailFolder folder = new MailFolder();
        folder.folderId = record.folderId;
        folder.ownerId = record.ownerId;
        folder.name = record.name;
        return folder;
    }

    /** Provides access to persistent mail data. */
    protected MailRepository _mailRepo;
    
    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;

}
