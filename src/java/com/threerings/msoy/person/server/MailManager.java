//
// $Id$

package com.threerings.msoy.person.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailHeaders;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.server.JSONMarshaller;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.person.server.persist.MailFolderRecord;
import com.threerings.msoy.person.server.persist.MailMessageRecord;
import com.threerings.msoy.person.server.persist.MailRepository;

import static com.threerings.msoy.Log.log;

/**
 * Manage msoy mail.
 */
public class MailManager
{
    /**
     * Prepares our mail manager for operation.
     */
    public void init (ConnectionProvider conProv, MemberRepository memberRepo)
    {
        _mailRepo = new MailRepository(conProv);
        _memberRepo = memberRepo;
    }

    /**
     * Returns a reference to our repository.
     */
    public MailRepository getRepository ()
    {
        return _mailRepo;
    }

    /**
     * Fetch and return a single message from the database.
     */
    public void getMessage (final int memberId, final int folderId, final int messageId,
                            final boolean flagAsRead, ResultListener<MailMessage> waiter)
    {
        final MemberObject mObj =
                folderId == MailFolder.INBOX_FOLDER_ID ? MsoyServer.lookupMember(memberId) : null;
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<MailMessage>(waiter) {
            public MailMessage invokePersistResult () throws PersistenceException {
                MailMessageRecord record = _mailRepo.getMessage(memberId, folderId, messageId);
                if (record == null) {
                    return null;
                }
                if (record.unread && flagAsRead) {
                    _mailRepo.setUnread(memberId, folderId, messageId, false);
                    // are we logged in, and did we read an unread message in the inbox?
                    if (mObj != null) {
                        // if so, count how many more of those there are
                        _count = _mailRepo.getMessageCount(memberId, folderId);
                    }
                }
                return record.toMailMessage(_memberRepo);
            }
            public void handleSuccess () {
                if (_count != null) {
                    mObj.setHasNewMail(_count.right > 0);
                }
                super.handleSuccess();
            }
            protected Tuple<Integer, Integer> _count;
        });
    }

    /**
     * Deliver a message, i.e. file one copy of it in the sender's 'Sent' folder,
     * and one copy in the recipient's 'Inbox' folder.
     */
    public void deliverMessage (final int senderId, final int recipientId, final String subject,
                                final String text, final MailPayload payload,
                                ResultListener<Void> waiter)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(waiter) {
            public Void invokePersistResult () throws PersistenceException {
                // copy the mail message into record format
                MailMessageRecord record = new MailMessageRecord();
                record.senderId = senderId;
                record.recipientId = recipientId;
                record.subject = subject;
                record.bodyText = text;

                if (payload != null) {
                    record.payloadType = payload.getType();
                    try {
                        record.payloadState =
                            JSONMarshaller.getMarshaller(payload.getClass()).getStateBytes(payload);
                    } catch (Exception e) {
                        throw new PersistenceException(e);
                    }
                }
                _mailRepo.deliverMessage(record);
                return null;
            }

            public void handleSuccess () {
                // if all went well and the recipient is online, notify them they have new mail
                MemberObject mObj = MsoyServer.lookupMember(recipientId);
                if (mObj != null) {
                    mObj.setHasNewMail(true);
                }
                super.handleSuccess();
            }
        });
    }

    /**
     * Move some messages from one folder to another.
     */
    public void moveMessages (final int memberId, final int folderId, final int[] msgIdArr,
                              final int newFolderId, ResultListener<Void> waiter)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(waiter) {
            public Void invokePersistResult () throws PersistenceException {
                _mailRepo.moveMessage(memberId, folderId, newFolderId, msgIdArr);
                return null;
            }
        });
    }

    /** Provides access to persistent mail data. */
    protected MailRepository _mailRepo;

    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;
}
