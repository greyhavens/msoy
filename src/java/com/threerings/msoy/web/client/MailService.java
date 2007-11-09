//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.person.data.MailFolder;
import com.threerings.msoy.person.data.MailMessage;
import com.threerings.msoy.person.data.MailPayload;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines mail services available to the GWT/AJAX web client.
 */
public interface MailService extends RemoteService
{
    /**
     * Loads and returns the metadata for the specified folder.
     */
    public MailFolder getFolder (WebIdent ident, int folderId)
        throws ServiceException;

    /**
     * Returns all folders for the specified member.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.MailFolder>
     */
    public List getFolders (WebIdent ident)
        throws ServiceException;

    /**
     * Loads and returns a specific mail message.
     */
    public MailMessage getMessage (WebIdent ident, int folderId, int messageId)
        throws ServiceException;

    /**
     * Returns all message headers in the specified folder.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.MailHeaders>
     */
    public List getHeaders (WebIdent ident, int folderId)
        throws ServiceException;

    /**
     * Delivers the supplied message to the specified recipient.
     */
    public void deliverMessage (WebIdent ident, int recipientId, String subject, String text,
                                MailPayload object)
        throws ServiceException;

    /**
     * Updates the payload on the specified message.
     */
    public void updatePayload (WebIdent ident, int folderId, int messageId, MailPayload payload)
        throws ServiceException;

    /**
     * Deletes the specified messages.
     */
    public void deleteMessages (WebIdent ident, int folderId, int[] msgIdArr)
        throws ServiceException;
}
