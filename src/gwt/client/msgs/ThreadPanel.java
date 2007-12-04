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
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;

import client.shell.Application;
import client.util.ClickCallback;
import client.util.MsoyUI;

/**
 * Displays a thread header and either its messages or a post creation or editing panel.
 */
public class ThreadPanel extends TitledListPanel
{
    public ThreadPanel (int threadId, int offset, ForumModels fmodels)
    {
        _threadId = threadId;
        _mpanel = new MessagesPanel(this);

        // look for our thread in the resolved group thread models
        ForumThread thread = fmodels.findThread(threadId);

        // if we found our thread, use that to avoid making the server do extra work and so that we
        // keep this ThreadRecord properly up to date
        if (thread != null) {
            _mpanel.setModel(new ForumModels.ThreadMessages(thread), 0);
        } else {
            _mpanel.setModel(new ForumModels.ThreadMessages(threadId), 0);
        }
        showMessages();
    }

    public void showMessages ()
    {
        setContents(getThreadTitle(), _mpanel, true);
        if (_group != null) { // if we already have our group, restore the group link
            setRightBits(Application.groupViewLink(_group.toString(), _group.getGroupId()));
        }
    }

    public void gotThread (ForumThread thread, GroupName group)
    {
        _thread = thread;
        _group = group;
        updateTitle(getThreadTitle());
        setRightBits(Application.groupViewLink(group.toString(), group.getGroupId()));
    }

    public void postReply (ForumMessage inReplyTo)
    {
        String title = CMsgs.mmsgs.threadReplyHeader(_thread.subject);
        setContents(title, new ReplyPanel(inReplyTo));
    }

    public void clearReply ()
    {
        showMessages();
    }

    public void editPost (ForumMessage message, AsyncCallback callback)
    {
        setContents(getThreadTitle(), new PostEditorPanel(message, callback));
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

    protected class ReplyPanel extends ContentFooterPanel
    {
        public ReplyPanel (ForumMessage inReplyTo)
        {
            _content.setWidget(0, 0, _editor = new MessageEditor());

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

            _footer.add(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    clearReply();
                }
            }));
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
            _footer.add(submit);
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

    protected class PostEditorPanel extends ContentFooterPanel
    {
        public PostEditorPanel (ForumMessage message, AsyncCallback callback)
        {
            _message = message;
            _callback = callback;

            _content.setWidget(0, 0, _editor = new MessageEditor());
            _editor.setHTML(message.message);

            _footer.add(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
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
            _footer.add(submit);
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
    protected GroupName _group;
}
