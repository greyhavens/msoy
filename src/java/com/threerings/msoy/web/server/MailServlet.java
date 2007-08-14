//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.msoy.person.server.persist.MailFolderRecord;
import com.threerings.msoy.person.server.persist.MailMessageRecord;
import com.threerings.msoy.person.server.persist.MailRepository;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.JSONMarshaller;

import com.threerings.msoy.web.client.MailService;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailHeaders;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class MailServlet extends MsoyServiceServlet
    implements MailService
{
    // from MailService
    public void deleteMessages (final WebIdent ident, final int folderId, final int[] msgIdArr)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        try {
            getMailRepo().deleteMessage(memrec.memberId, folderId, msgIdArr);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to delete messages [mid=" + memrec.memberId +
                    ", fid=" + folderId + ", mids=" + StringUtil.toString(msgIdArr) + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MailService
    public void deliverMessage (final WebIdent ident, final int recipientId, final String subject,
                                final String text, final MailPayload object)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "deliverMessage[" + recipientId + ", " + subject + ", " + text + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.deliverMessage(
                    memrec.memberId, recipientId, subject, text, object, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from MailService
    public void updatePayload (WebIdent ident, final int folderId, final int messageId,
                               MailPayload payload)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        try {
            byte[] state = JSONMarshaller.getMarshaller(payload.getClass()).getStateBytes(payload);
            getMailRepo().setPayloadState(memrec.memberId, folderId, messageId, state);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed update payload [mid=" + memrec.memberId +
                    ", fid=" + folderId + ", mid=" + messageId + ", pay=" + payload + "].", e);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MailService
    public MailFolder getFolder (WebIdent ident, final int folderId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        try {
            return buildFolder(getMailRepo().getFolder(memrec.memberId, folderId));
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getFolder failed [mid=" + memrec.memberId +
                    ", fid=" + folderId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MailService
    public List<MailFolder> getFolders (WebIdent ident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        try {
            List<MailFolder> result = new ArrayList<MailFolder>();
            for (MailFolderRecord record : getMailRepo().getFolders(memrec.memberId)) {
                result.add(buildFolder(record));
            }
            return result;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getFolders failed [mid=" + memrec.memberId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MailService
    public List<MailHeaders> getHeaders (final WebIdent ident, final int folderId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        try {
            List<MailHeaders> result = new ArrayList<MailHeaders>();
            for (MailMessageRecord record : getMailRepo().getMessages(memrec.memberId, folderId)) {
                result.add(record.toMailHeaders(MsoyServer.memberRepo));
            }
            return result;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getHeaders failed [mid=" + memrec.memberId +
                    ", fid=" + folderId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MailService
    public MailMessage getMessage (final WebIdent ident, final int folderId, final int messageId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        final ServletWaiter<MailMessage> waiter = new ServletWaiter<MailMessage>(
            "getMessage[" + folderId + ", " + messageId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.getMessage(
                    memrec.memberId, folderId, messageId, true, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // build a MailFolder object, including the message counts which require a separate query
    protected MailFolder buildFolder (MailFolderRecord record)
        throws PersistenceException
    {
        MailFolder folder = toMailFolder(record);
        Tuple<Integer, Integer> counts =
            getMailRepo().getMessageCount(record.ownerId, record.folderId);
        folder.unreadCount = counts.right != null ? counts.right.intValue() : 0;
        folder.readCount = counts.left != null ? counts.left.intValue() : 0;
        return folder;
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

    protected static MailRepository getMailRepo ()
    {
        return MsoyServer.mailMan.getRepository();
    }
}
