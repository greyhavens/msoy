//
// $Id$

package com.threerings.msoy.web.server;

import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.MailService;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailHeaders;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class MailServlet extends MsoyServiceServlet
    implements MailService
{
    // from MailService
    public void deleteMessages (final WebCreds creds, final int folderId, final int[] msgIdArr)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        try {
            MsoyServer.mailMan.getRepository().deleteMessage(memrec.memberId, folderId, msgIdArr);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to delete messages [mid=" + memrec.memberId +
                    ", fid=" + folderId + ", mids=" + StringUtil.toString(msgIdArr) + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MailService
    public void deliverMessage (final WebCreds creds, final int recipientId, final String subject,
                                final String text, final MailPayload object)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(creds);
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
    public void updatePayload (final WebCreds creds, final int folderId, final int messageId,
                               final MailPayload payload)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(creds);
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "updatePayload[" + folderId + ", " + messageId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.updatePayload(
                    memrec.memberId, folderId, messageId, payload, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from MailService
    public MailFolder getFolder (final WebCreds creds, final int folderId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(creds);
        final ServletWaiter<MailFolder> waiter = new ServletWaiter<MailFolder>(
            "deliverMessage[" + folderId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.getFolder(memrec.memberId, folderId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from MailService
    public List<MailFolder> getFolders (final WebCreds creds)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(creds);
        final ServletWaiter<List<MailFolder>> waiter = new ServletWaiter<List<MailFolder>>(
            "getFolders[]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.getFolders(memrec.memberId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from MailService
    public List<MailHeaders> getHeaders (final WebCreds creds, final int folderId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(creds);
        final ServletWaiter<List<MailHeaders>> waiter = new ServletWaiter<List<MailHeaders>>(
            "getHeaders[]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.getHeaders(memrec.memberId, folderId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from MailService
    public MailMessage getMessage (final WebCreds creds, final int folderId, final int messageId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(creds);
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
}
