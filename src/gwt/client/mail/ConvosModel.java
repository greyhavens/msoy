//
// $Id$

package client.mail;

import java.util.List;

import client.util.ServiceBackedDataModel;
import client.util.events.FlashEvents;
import client.util.events.StatusChangeEvent;

import com.threerings.msoy.person.gwt.ConvMessage;
import com.threerings.msoy.person.gwt.Conversation;
import com.threerings.msoy.person.gwt.MailService;

/**
 * A data model that provides a member's conversations.
 */
public class ConvosModel extends ServiceBackedDataModel<Conversation, MailService.ConvosResult>
{
    /**
     * Notes that we've read the specified conversation.
     */
    public void markConversationRead (int convoId)
    {
        int unread = 0;
        for (Conversation convo : _pageItems) {
            if (convo.conversationId == convoId) {
                convo.hasUnread = false;
            } else if (convo.hasUnread) {
                unread++;
            }
        }

        // now dispatch an event indicating our new unread mail count
        FlashEvents.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.MAIL, unread, unread+1));
    }

    /**
     * Notes that we've added a new message to the specified conversation.
     */
    public void noteMessageAdded (int convoId, ConvMessage message)
    {
        Conversation convo = findConversation(convoId);
        if (convo != null) {
            convo.lastSent = message.sent;
            convo.lastSnippet = (message.body.length() > Conversation.SNIPPET_LENGTH) ?
                message.body.substring(0, Conversation.SNIPPET_LENGTH) : message.body;
        }
    }

    /**
     * Notes that a conversation has been deleted.
     */
    public void conversationDeleted (int convoId)
    {
        Conversation convo = findConversation(convoId);
        if (convo != null) {
            removeItem(convo);
        }
    }

    protected Conversation findConversation (int convoId)
    {
        for (Conversation convo : _pageItems) {
            if (convo.conversationId == convoId) {
                return convo;
            }
        }
        return null;
    }

    @Override // from ServiceBackedDataModel
    protected void callFetchService (int start, int count, boolean needCount) {
        CMail.mailsvc.loadConversations(CMail.ident, start, count, needCount, this);
    }

    @Override // from ServiceBackedDataModel
    protected int getCount (MailService.ConvosResult result) {
        return result.totalConvoCount;
    }

    @Override // from ServiceBackedDataModel
    protected List<Conversation> getRows (MailService.ConvosResult result) {
        return result.convos;
    }
}
