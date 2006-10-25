//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Defines mail services available to the GWT/AJAX web client.
 */
public interface MailService extends RemoteService
{
    public MailFolder getFolder (WebCreds creds, int folderId)
        throws ServiceException;
    
    public List getFolders (WebCreds creds)
        throws ServiceException;

    public MailMessage getMessage (WebCreds creds, int folderId, int messageId)
        throws ServiceException;
    
    public List getHeaders (WebCreds creds, int folderId)
        throws ServiceException;
    
    public void deliverMessage (WebCreds creds, int recipientId, String subject, String text)
        throws ServiceException;

    public void deleteMessages (WebCreds creds, int folderId, int[] msgIdArr)
        throws ServiceException;
}
