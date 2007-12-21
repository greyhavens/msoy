//
// $Id$

package com.threerings.msoy.person.server;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;

import com.google.common.collect.Maps;
import org.apache.velocity.VelocityContext;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.JSONMarshaller;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.person.data.GameAwardPayload;
import com.threerings.msoy.person.data.MailFolder;
import com.threerings.msoy.person.data.MailMessage;
import com.threerings.msoy.person.data.MailPayload;
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
    public void init (
        PersistenceContext ctx, MemberRepository memberRepo, MsoyEventLogger eventLog)
    {
        _mailRepo = new MailRepository(ctx, eventLog);
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
     * Called when a new member is created. Configures their mail folders.
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

        MsoyServer.invoker.postUnit(new Invoker.Unit("MailManager.memberCreated") {
            public boolean invoke () {
                try {
                    for (MailFolderRecord record : folders) {
                        _mailRepo.createFolder(record);
                    }
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Member mailbox initialization failure.", pe);
                }
                return false;
            }
        });
    }

    /**
     * Deliver a message, i.e. file one copy of it in the sender's 'Sent' folder, and one copy in
     * the recipient's 'Inbox' folder. <em>Note:</em> because this method immediately posts an
     * invoker unit, it may be called from any thread.
     */
    public void deliverMessage (final int senderId, final int recipientId, final String subject,
                                final String text, final MailPayload payload,
                                final boolean internalOnly, ResultListener<Void> waiter)
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

                // and count how many messages the recipient has now
                _count = _mailRepo.getMessageCount(recipientId, MailFolder.INBOX_FOLDER_ID).right;

                return null;
            }

            public void handleSuccess () {
                // if all went well, attempt to notify the recipient they have new mail
                MemberNodeActions.reportUnreadMail(recipientId, _count);

                super.handleSuccess();

                // finally send a real email to the recipient
                if (!internalOnly && !_reciprec.isSet(MemberRecord.FLAG_NO_WHIRLED_MAIL_TO_EMAIL)) {
                    MsoyServer.mailInvoker.postUnit(
                        new MailSender.Unit(_reciprec.accountName, "gotMail") {
                            protected void populateContext (VelocityContext ctx) {
                                ctx.put("subject", _record.subject);
                                ctx.put("sender", _sendrec.name);
                                ctx.put("senderId", _sendrec.memberId);
                                ctx.put("body", (_record.bodyText == null) ? "" : _record.bodyText);
                                ctx.put("server_url", ServerConfig.getServerURL());
                            }
                        });
                }
            }

            protected MemberRecord _sendrec, _reciprec;
            protected MailMessageRecord _record;
            protected int _count;
        });
    }

    /** Provides access to persistent mail data. */
    protected MailRepository _mailRepo;

    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;

    static {
        // register a migration for TrophyAwardPayload -> GameAwardPayload
        Map<String, String> migmap = Maps.newHashMap();
        migmap.put("trophyName", "awardName");
        migmap.put("trophyMedia", "awardMediaHash");
        migmap.put("trophyMimeType", "awardMimeType");
        JSONMarshaller.registerMigration(GameAwardPayload.class, migmap);
    }
}
