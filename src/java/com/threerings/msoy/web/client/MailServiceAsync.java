//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface MailServiceAsync
{
    /**
     * The asynchronous version of {@link MailService#getFolder}
     */
    public MailFolder getFolder (WebCreds creds, int folderId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getFolders}
     */
    public List getFolders (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getMessage}
     */
    public MailMessage getMessage (WebCreds creds, int folderId, int messageId,
                                   AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getMessages}
     */
    public List getMessages (WebCreds creds, int folderId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#deliverMail}
     */
    public void deliverMail (WebCreds creds, MailMessage msg, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#moveMail}
     */
    public void moveMail (WebCreds creds, MailMessage msg, int newFolderId,
                          AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#deleteMail}
     */
    public void deleteMail (WebCreds creds, int folderId, int[] msgIdArr, AsyncCallback callback);
}
