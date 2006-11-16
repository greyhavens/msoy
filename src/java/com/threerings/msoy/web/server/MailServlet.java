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
    public void deleteMessages (WebCreds creds, int folderId, int[] msgIdArr)
        throws ServiceException
    {
        ServletWaiter<Void> waiter = new ServletWaiter<Void>(
                "deleteMail[" + folderId + ", arr[" + msgIdArr.length + "]]");
        MsoyServer.mailMan.deleteMessages(creds.memberId, folderId, msgIdArr, waiter);
        waiter.waitForResult();
    }

    // from MailService
    public void deliverMessage (WebCreds creds, int recipientId, String subject, String text,
                                MailPayload object)
        throws ServiceException
    {
        ServletWaiter<Void> waiter = new ServletWaiter<Void>(
                "deliverMessage[" + recipientId + ", " + subject + ", " + text + "]");
        MsoyServer.mailMan.deliverMessage(
            creds.memberId, recipientId, subject, text, object, waiter);
        waiter.waitForResult();
    }

    // from MailService
    public void updatePayload (WebCreds creds, int folderId, int messageId, MailPayload payload)
        throws ServiceException
    {
        ServletWaiter<Void> waiter = new ServletWaiter<Void>(
                "updatePayload[" + folderId + ", " + messageId + "]");
        MsoyServer.mailMan.updatePayload(creds.memberId, folderId, messageId, payload, waiter);
        waiter.waitForResult();
    }

    // from MailService
    public MailFolder getFolder (WebCreds creds, int folderId) throws ServiceException
    {
        ServletWaiter<MailFolder> waiter = new ServletWaiter<MailFolder>(
                "deliverMessage[" + folderId + "]");
        MsoyServer.mailMan.getFolder(creds.memberId, folderId, waiter);
        return waiter.waitForResult();
    }

    // from MailService
    public List<MailFolder> getFolders (WebCreds creds) throws ServiceException
    {
        ServletWaiter<List<MailFolder>> waiter =
            new ServletWaiter<List<MailFolder>>("getFolders[]");
        MsoyServer.mailMan.getFolders(creds.memberId, waiter);
        return waiter.waitForResult();
    }

    // from MailService
    public List<MailHeaders> getHeaders (WebCreds creds, int folderId) throws ServiceException
    {
        ServletWaiter<List<MailHeaders>> waiter =
            new ServletWaiter<List<MailHeaders>>("getHeaders[]");
        MsoyServer.mailMan.getHeaders(creds.memberId, folderId, waiter);
        return waiter.waitForResult();
    }

    // from MailService
    public MailMessage getMessage (WebCreds creds, int folderId, int messageId)
        throws ServiceException
    {
        ServletWaiter<MailMessage> waiter = new ServletWaiter<MailMessage>(
                "getMessage[" + folderId + ", " + messageId + "]");
        MsoyServer.mailMan.getMessage(creds.memberId, folderId, messageId, true, waiter);
        return waiter.waitForResult();
    }
}
