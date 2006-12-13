//
// $Id$

package com.threerings.msoy.web.server;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.MailService;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailHeaders;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class MailServlet extends RemoteServiceServlet
    implements MailService
{
    // from MailService
    public void deleteMessages (final WebCreds creds, final int folderId, final int[] msgIdArr)
        throws ServiceException
    {
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "deleteMail[" + folderId + ", arr[" + msgIdArr.length + "]]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.deleteMessages(creds.memberId, folderId, msgIdArr, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from MailService
    public void deliverMessage (final WebCreds creds, final int recipientId, final String subject,
                                final String text, final MailPayload object)
        throws ServiceException
    {
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "deliverMessage[" + recipientId + ", " + subject + ", " + text + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.deliverMessage(
                    creds.memberId, recipientId, subject, text, object, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from MailService
    public void updatePayload (final WebCreds creds, final int folderId, final int messageId,
                               final MailPayload payload)
        throws ServiceException
    {
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "updatePayload[" + folderId + ", " + messageId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.updatePayload(
                    creds.memberId, folderId, messageId, payload, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from MailService
    public MailFolder getFolder (final WebCreds creds, final int folderId)
        throws ServiceException
    {
        final ServletWaiter<MailFolder> waiter = new ServletWaiter<MailFolder>(
            "deliverMessage[" + folderId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.getFolder(creds.memberId, folderId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from MailService
    public List<MailFolder> getFolders (final WebCreds creds)
        throws ServiceException
    {
        final ServletWaiter<List<MailFolder>> waiter = new ServletWaiter<List<MailFolder>>(
            "getFolders[]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.getFolders(creds.memberId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from MailService
    public List<MailHeaders> getHeaders (final WebCreds creds, final int folderId)
        throws ServiceException
    {
        final ServletWaiter<List<MailHeaders>> waiter = new ServletWaiter<List<MailHeaders>>(
            "getHeaders[]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.getHeaders(creds.memberId, folderId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from MailService
    public MailMessage getMessage (final WebCreds creds, final int folderId, final int messageId)
        throws ServiceException
    {
        final ServletWaiter<MailMessage> waiter = new ServletWaiter<MailMessage>(
            "getMessage[" + folderId + ", " + messageId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.mailMan.getMessage(creds.memberId, folderId, messageId, true, waiter);
            }
        });
        return waiter.waitForResult();
    }
}
