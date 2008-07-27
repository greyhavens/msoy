//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines mail services available to the GWT/AJAX web client.
 */
public interface MailService extends RemoteService
{
    /** Communicates results for {@link #loadConversation}. */
    public static class ConvosResult implements IsSerializable
    {
        /** The total conversation count for this player (if requested). */
        public int totalConvoCount;

        /** The conversations. */
        public List<Conversation> convos;
    }

    /** Communicates results for {@link #loadConversation}. */
    public static class ConvoResult implements IsSerializable
    {
        /** The name of the other party to this conversation. */
        public MemberName other;

        /** The subject of the conversation. */
        public String subject;

        /** The sent time of the last message in this conversation the caller has read. */
        public long lastRead;

        /** The messages in this conversation. */
        public List<ConvMessage> messages;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/mailsvc";

    /**
     * Loads the specified range of conversations in which the caller is a participant.
     */
    public ConvosResult loadConversations (WebIdent ident, int offset, int count, boolean needCount)
        throws ServiceException;

    /**
     * Loads the specified conversation.
     */
    public ConvoResult loadConversation (WebIdent ident, int convoId)
        throws ServiceException;

    /**
     * Starts a conversation with another member.
     */
    public void startConversation (WebIdent ident, int recipientId, String subject, String body,
                                   MailPayload attachment)
        throws ServiceException;

    /**
     * Posts a message to an existing conversation.
     */
    public ConvMessage continueConversation (WebIdent ident, int convoId, String text,
                                             MailPayload attachment)
        throws ServiceException;

    /**
     * Deletes a conversation (for the calling user).
     *
     * @return true if the conversation was deleted, false if it could not be deleted because it
     * has unread messages.
     */
    public boolean deleteConversation (WebIdent ident, int convoId)
        throws ServiceException;

    /**
     * Updates the payload on the specified message.
     */
    public void updatePayload (WebIdent ident, int convoId, long sent, MailPayload payload)
        throws ServiceException;
}
