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
import com.threerings.msoy.web.data.MailHeaders;
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
                return toMailMessage(_mailRepo.getMessage(memberId, folderId, messageId));
            }
        });
    }

    /**
     * Fetch and return all the messages in a folder from the database. 
     */
    public void getHeaders (final int memberId, final int folderId,
                             ResultListener<List<MailHeaders>> waiter)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<List<MailHeaders>>(waiter) {
            public List<MailHeaders> invokePersistResult () throws PersistenceException {
                List<MailHeaders> result = new ArrayList<MailHeaders>();
                for (MailMessageRecord record : _mailRepo.getMessages(memberId, folderId)) {
                    result.add(toMailHeaders(record));
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
                return toMailFolder(_mailRepo.getFolder(memberId, folderId));
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
                testFolders(memberId);

                List<MailFolder> result = new ArrayList<MailFolder>();
                for (MailFolderRecord record : _mailRepo.getFolders(memberId)) {
                    result.add(toMailFolder(record));
                }
                return result;
            }
        });
    }

    /**
     * Deliver a message, i.e. file one copy of it in the sender's 'Sent' folder,
     * and one copy in the recipient's 'Inbox' folder.
     */    
    public void deliverMessage (final int senderId, final int recipientId, final String subject,
                                final String text, ResultListener<Void> waiter)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Void>(waiter) {
            public Void invokePersistResult () throws PersistenceException {
                testFolders(senderId);
                testFolders(recipientId);
                
                // copy the mail message into record format
                MailMessageRecord record = new MailMessageRecord();
                record.senderId = senderId;
                record.recipientId = recipientId;
                record.subject = subject;
                record.message = text;
                record.sent = new Timestamp(System.currentTimeMillis());

                // file one copy for ourselves
                record.ownerId = senderId;
                record.folderId = MailFolder.SENT_FOLDER_ID;
                _mailRepo.fileMessage(record);
                
                // and one for the recipient (safely reusing the record object)
                record.ownerId = recipientId;
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

    // create a MailHeaders object from a a MailMessageRecord
    protected MailHeaders toMailHeaders (MailMessageRecord record)
        throws PersistenceException
    {
        MailHeaders headers = new MailHeaders();
        headers.messageId = record.messageId;
        headers.folderId = record.folderId;
        headers.ownerId = record.ownerId;
        headers.subject = record.subject;
        headers.sent = new Date(record.sent.getTime());
        
        MemberRecord memRec = _memberRepo.loadMember(record.senderId);
        headers.sender = new MemberGName(memRec.name, memRec.memberId);

        memRec = _memberRepo.loadMember(record.recipientId);
        headers.recipient = new MemberGName(memRec.name, memRec.memberId);
        return headers;
    }
    
    // convert a MailMessageRecord to a MailMessage
    protected MailMessage toMailMessage (MailMessageRecord record)
        throws PersistenceException
    {
        MailMessage message = new MailMessage();
        message.headers = toMailHeaders(record);
        message.message = record.message;
        return message;
    }

    // convert a MailFolderRecord to its MailFolder form
    protected MailFolder toMailFolder (MailFolderRecord record)
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
