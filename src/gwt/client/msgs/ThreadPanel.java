//
// $Id$

package client.msgs;

import java.util.Date;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;

import client.util.ClickCallback;
import client.util.MsoyUI;

/**
 * Displays a thread header and either its messages or a post creation or editing panel.
 */
public class ThreadPanel extends TitledListPanel
{
    public ThreadPanel (int threadId)
    {
        _threadId = threadId;
        _mpanel = new MessagesPanel(this);
        _mpanel.setModel(new ForumModels.ThreadMessages(threadId), 0);
        showMessages();
    }

    public void showMessages ()
    {
        setContents(getThreadTitle(), _mpanel, true);
    }

    public void gotThread (ForumThread thread)
    {
        _thread = thread;
        updateTitle(getThreadTitle());
    }

    public void postReply (ForumMessage inReplyTo)
    {
        setContents(CMsgs.mmsgs.threadReplyHeader(_thread.subject), new ReplyPanel(inReplyTo), true);
    }

    public void clearReply ()
    {
        showMessages();
    }

    public void editPost (ForumMessage message, AsyncCallback callback)
    {
        setContents(getThreadTitle(), new PostEditorPanel(message, callback), true);
    }

    protected void replyPosted (ForumMessage message)
    {
        _mpanel.replyPosted(message);
        showMessages();
    }

    protected String getThreadTitle ()
    {
        if (_thread == null) {
            return "...";
        } else if (_thread.isAnnouncement()) {
            return CMsgs.mmsgs.threadAnnouncementHeader(_thread.subject);
        } else {
            return CMsgs.mmsgs.threadNormalHeader(_thread.subject);
        }
    }

    protected class ReplyPanel extends FlexTable
    {
        public ReplyPanel (ForumMessage inReplyTo)
        {
            setStyleName("replyPanel");

            setWidget(0, 0, _editor = new MessageEditor());

            // set the quote text if available
            if (inReplyTo != null) {
                _editor.setHTML(
                    CMsgs.mmsgs.replyQuote(inReplyTo.poster.name.toString(), inReplyTo.message));
                DeferredCommand.addCommand(new Command() {
                    public void execute () {
                        _editor.getTextArea().getBasicFormatter().selectAll();
                    }
                });
            }

            HorizontalPanel buttons = new HorizontalPanel();
            buttons.add(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    clearReply();
                }
            }));
            buttons.add(WidgetUtil.makeShim(5, 5));
            Button submit = new Button(CMsgs.cmsgs.submit());
            final int replyId = (inReplyTo == null) ? 0 : inReplyTo.messageId;
            new ClickCallback(submit) {
                public boolean callService () {
                    String text = _editor.getHTML();
                    if (text.length() == 0) {
                        MsoyUI.error(CMsgs.mmsgs.errMissingReply());
                        return false;
                    }
                    // TODO: when we support quoting, make sure there's more than the quoted text
                    CMsgs.forumsvc.postMessage(CMsgs.ident, _threadId, replyId, text, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    replyPosted((ForumMessage)result);
                    return false;
                }
            };
            buttons.add(submit);
            setWidget(2, 0, buttons);
            getFlexCellFormatter().setHorizontalAlignment(2, 0, HasAlignment.ALIGN_RIGHT);
        }

        // @Override // from Widget
        protected void onAttach ()
        {
            super.onAttach();
            _editor.setFocus(true);
            DOM.scrollIntoView(getElement());
        }

        protected MessageEditor _editor;
    }

    protected class PostEditorPanel extends VerticalPanel
    {
        public PostEditorPanel (ForumMessage message, AsyncCallback callback)
        {
            _message = message;
            _callback = callback;

            add(_editor = new MessageEditor());
            _editor.setHTML(message.message);
            setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
            HorizontalPanel buttons = new HorizontalPanel();
            buttons.add(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    showMessages();
                }
            }));
            Button submit = new Button(CMsgs.cmsgs.submit());
            new ClickCallback(submit) {
                public boolean callService () {
                    _text = _editor.getHTML();
                    if (_text.length() == 0) {
                        MsoyUI.error(CMsgs.mmsgs.errMissingReply());
                        return false;
                    }
                    CMsgs.forumsvc.editMessage(CMsgs.ident, _message.messageId, _text, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    MsoyUI.info(CMsgs.mmsgs.msgPostUpdated());
                    _message.message = _text;
                    _message.lastEdited = new Date();
                    _callback.onSuccess(_message);
                    showMessages();
                    return false;
                }
                protected String _text;
            };
            buttons.add(submit);
            add(buttons);
        }

        // @Override // from Widget
        protected void onAttach ()
        {
            super.onAttach();
            _editor.setFocus(true);
        }

        protected ForumMessage _message;
        protected AsyncCallback _callback;
        protected MessageEditor _editor;
    }

    protected MessagesPanel _mpanel;

    protected int _threadId;
    protected ForumThread _thread;
}
