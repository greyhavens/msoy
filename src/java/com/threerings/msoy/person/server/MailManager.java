//
// $Id$

package com.threerings.msoy.person.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.apache.velocity.VelocityContext;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailHeaders;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.JSONMarshaller;
import com.threerings.msoy.server.util.MailSender;

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
    public void init (PersistenceContext ctx, MemberRepository memberRepo)
    {
        _mailRepo = new MailRepository(ctx);
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
     * Called when a new member is created. Configures their mail folders and sends them a welcome
     * message.
     */
    public void memberCreated (int memberId)
    {
        final ArrayList<MailFolderRecord> folders = new ArrayList<MailFolderRecord>();
        for (int folderId : MailFolder.STOCK_FOLDERS) {
            MailFolderRecord record = new MailFolderRecord();
            record.ownerId = memberId;
            record.nextMessageId = 1;
            record.folderId = folderId;
            record.name = MsoyServer.msgMan.getBundle("server").get("m.mail_folder_" + folderId);
            folders.add(record);
        }

        final MailMessageRecord welcome = new MailMessageRecord();
        welcome.recipientId = memberId;
        // TODO: We need to be able to send system messages somehow.
        welcome.senderId = memberId;
        welcome.subject = MsoyServer.msgMan.getBundle("server").get("m.welcome_mail_subject");
        welcome.bodyText = MsoyServer.msgMan.getBundle("server").get("m.welcome_mail_body");

        // now actually save this stuff to the database
        MsoyServer.invoker.postUnit(new Invoker.Unit("MailManager.memberCreated") {
            public boolean invoke () {
                try {
                    for (MailFolderRecord record : folders) {
                        _mailRepo.createFolder(record);
                    }
                    _mailRepo.deliverMessage(welcome);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Member mailbox initialization failure.", pe);
                }
                return false;
            }
        });
    }

    /**
     * Fetch and return a single message from the database.
     */
    public void getMessage (final int memberId, final int folderId, final int messageId,
                            final boolean flagAsRead, ResultListener<MailMessage> waiter)
    {
        final MemberObject mObj = folderId == MailFolder.INBOX_FOLDER_ID ?
            MsoyServer.lookupMember(memberId) : null;
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
     * Deliver a message, i.e. file one copy of it in the sender's 'Sent' folder, and one copy in
     * the recipient's 'Inbox' folder. <em>Note:</em> because this method immediately posts an
     * invoker unit, it may be called from any thread.
     */
    public void deliverMessage (final int senderId, final int recipientId, final String subject,
                                final String text, final MailPayload payload,
                                ResultListener<Void> waiter)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(waiter) {
            public Void invokePersistResult () throws Exception {
                // look up the sender and recipient for later emailery
                _sendrec = MsoyServer.memberRepo.loadMember(senderId);
                _reciprec = MsoyServer.memberRepo.loadMember(recipientId);
                if (_sendrec == null || _reciprec == null) {
                    log.info("Dropping message with missing sender or recipient " +
                             "[sender=" + senderId + ", recip=" + recipientId + "].");
                    throw new InvocationException("m.no_such_user");
                }

                // copy the mail message into record format
                _record = new MailMessageRecord();
                _record.senderId = senderId;
                _record.recipientId = recipientId;
                _record.subject = subject;
                _record.bodyText = text;

                // serialize the payload
                if (payload != null) {
                    _record.payloadType = payload.getType();
                    try {
                        _record.payloadState = JSONMarshaller.getMarshaller(
                            payload.getClass()).getStateBytes(payload);
                    } catch (Exception e) {
                        throw new PersistenceException(e);
                    }
                }

                // record the message to the repository
                _mailRepo.deliverMessage(_record);
                return null;
            }

            public void handleSuccess () {
                // if all went well and the recipient is online, notify them they have new mail
                MemberObject mObj = MsoyServer.lookupMember(recipientId);
                if (mObj != null) {
                    mObj.setHasNewMail(true);
                }
                super.handleSuccess();

                // finally send a real email to the recipient
                MsoyServer.mailInvoker.postUnit(
                    new MailSender.Unit(_reciprec.accountName, "gotMail") {
                    protected void populateContext (VelocityContext ctx) {
                        ctx.put("subject", _record.subject);
                        ctx.put("sender", _sendrec.name);
                        ctx.put("body", (_record.bodyText == null) ? "" : _record.bodyText);
                        ctx.put("server_url", ServerConfig.getServerURL());
                    }
                });
            }

            protected MemberRecord _sendrec, _reciprec;
            protected MailMessageRecord _record;
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
