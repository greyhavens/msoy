//
// $Id$

package com.threerings.msoy.web.client;

import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface MailServiceAsync
{
    /**
     * The asynchronous version of {@link MailService#getFolder}
     */
    public void getFolder (WebCreds creds, int folderId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getFolders}
     */
    public void getFolders (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getMessage}
     */
    public void getMessage (WebCreds creds, int folderId, int messageId,
                                   AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#getHeaders}
     */
    public void getHeaders (WebCreds creds, int folderId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#deliverMessage}
     */
    public void deliverMessage (WebCreds creds, int recipientId, String subject, String text,
                                MailPayload object, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#updatePayload}
     */
    public void updatePayload (WebCreds creds, int folderId, int messageId,
                                  MailPayload obj, AsyncCallback callback);

    /**
     * The asynchronous version of {@link MailService#deleteMessages}
     */
    public void deleteMessages (WebCreds creds, int folderId, int[] msgIdArr,
                                AsyncCallback callback);
}
