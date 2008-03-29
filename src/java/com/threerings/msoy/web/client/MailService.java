//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.person.data.ConvMessage;
import com.threerings.msoy.person.data.Conversation;
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
    /** Communicates results for {@link #loadConversation}. */
    public static class ConvResult implements IsSerializable
    {
        /** The subject of the conversation. */
        public String subject;

        /** The sent time of the last message in this conversation the caller has read. */
        public long lastRead;

        /** The messages in this conversation.
         * @gwt.typeArgs <com.threerings.msoy.person.data.ConvMessage> */
        public List messages;
    }

    /**
     * Loads the specified range of conversations in which the caller is a participant.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.Conversation>
     */
    public List loadConversations (WebIdent ident, int offset, int count)
        throws ServiceException;

    /**
     * Loads the specified conversation.
     */
    public ConvResult loadConversation (WebIdent ident, int convoId)
        throws ServiceException;

    /**
     * Starts a conversation with another member.
     */
    public void startConversation (WebIdent ident, int recipientId, String subject, String text,
                                   MailPayload attachment)
        throws ServiceException;

    /**
     * Posts a message to an existing conversation.
     */
    public ConvMessage continueConversation (WebIdent ident, int convoId, String text,
                                             MailPayload attachment)
        throws ServiceException;

    /**
     * Loads and returns the metadata for the specified folder.
     */
    public MailFolder getFolder (WebIdent ident, int folderId)
        throws ServiceException;

    /**
     * Loads all messages in the specified conversation.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.MailMessage>
     */
    public List getConversation (WebIdent ident, int folderId, int convId)
        throws ServiceException;

    /**
     * Returns all folders for the specified member.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.MailFolder>
     */
    public List getFolders (WebIdent ident)
        throws ServiceException;

    /**
     * Returns all message headers in the specified folder.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.MailHeaders>
     */
    public List getHeaders (WebIdent ident, int folderId)
        throws ServiceException;

    /**
     * Loads and returns a specific mail message.
     */
    public MailMessage getMessage (WebIdent ident, int folderId, int messageId)
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
