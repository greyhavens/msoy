//
// $Id$

package client.mail;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.mail.gwt.ConvMessage;
import com.threerings.msoy.mail.gwt.Conversation;
import com.threerings.msoy.mail.gwt.MailService;
import com.threerings.msoy.mail.gwt.MailServiceAsync;

import client.shell.CShell;
import client.util.MsoyServiceBackedDataModel;
import client.util.events.FlashEvents;
import client.util.events.StatusChangeEvent;
import client.util.events.StatusChangeListener;

/**
 * A data model that provides a member's conversations.
 */
public class ConvosModel extends MsoyServiceBackedDataModel<Conversation, MailService.ConvosResult>
{
    public ConvosModel ()
    {
        FlashEvents.addListener(new StatusChangeListener() {
            public void statusChanged (StatusChangeEvent event) {
                if (event.getType() == StatusChangeEvent.MAIL) {
                    int newUnread = event.getValue();
                    if (_unreadCount != newUnread) {
                        // do a partial reset, so that we request new mail next change
                        _count = -1;
                        _unreadCount = newUnread;
                    }
                }
            }
        });
    }

    /**
     * Notes that we've read the specified conversation.
     */
    public void markConversationRead (int convoId)
    {
        Conversation convo = findConversation(convoId);
        if (convo != null && convo.hasUnread) {
            convo.hasUnread = false;
            _unreadCount--;
            dispatchUnread();
        }
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
            if (convo.hasUnread) {
                _unreadCount--;
                dispatchUnread();
            }
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
    protected void callFetchService (int start, int count, boolean needCount,
                                     AsyncCallback<MailService.ConvosResult> callback)
    {
        _mailsvc.loadConversations(start, count, needCount, callback);
    }

    @Override // from ServiceBackedDataModel
    protected int getCount (MailService.ConvosResult result)
    {
        _unreadCount = result.unreadConvoCount;
        dispatchUnread();
        return result.totalConvoCount;
    }

    @Override // from ServiceBackedDataModel
    protected List<Conversation> getRows (MailService.ConvosResult result)
    {
        return result.convos;
    }

    protected void dispatchUnread ()
    {
        CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.MAIL, _unreadCount, 0));
    }

    /** The total number of unread conversations, on all pages. */
    protected int _unreadCount;

    protected static final MailServiceAsync _mailsvc = (MailServiceAsync)
        ServiceUtil.bind(GWT.create(MailService.class), MailService.ENTRY_POINT);
}
