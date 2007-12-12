//
// $Id$

package client.msgs;

import java.util.Date;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;

import client.shell.Application;
import client.util.BorderedDialog;
import client.util.ClickCallback;
import client.util.MsoyUI;

/**
 * Displays a thread header and either its messages or a post creation or editing panel.
 */
public class ThreadPanel extends TitledListPanel
{
    public ThreadPanel (int threadId, int page, int scrollToId, ForumModels fmodels)
    {
        _threadId = threadId;
        _mpanel = new MessagesPanel(this, scrollToId);

        // look for our thread in the resolved group thread models
        ForumThread thread = fmodels.findThread(threadId);

        // if we found our thread, use that to avoid making the server do extra work and so that we
        // keep this ThreadRecord properly up to date
        if (thread != null) {
            _mpanel.setModel(new ForumModels.ThreadMessages(thread), page);
        } else {
            _mpanel.setModel(new ForumModels.ThreadMessages(threadId), page);
        }
        showMessages();
    }

    public void showMessages ()
    {
        setContents(getThreadTitle(), _mpanel, true);
        if (_thread != null) { // if we already have our group, restore the group link
            setRightBits(Application.groupViewLink(_thread.group.toString(),
                                                   _thread.group.getGroupId()));
        }
    }

    public void gotThread (ForumThread thread)
    {
        _thread = thread;
        updateTitle(getThreadTitle());
        setRightBits(Application.groupViewLink(thread.group.toString(), thread.group.getGroupId()));
    }

    public void editFlags ()
    {
        new ThreadFlagsEditorPanel().show();
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

    protected class ReplyPanel extends TableFooterPanel
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
                    int extra = text.length() - ForumMessage.MAX_MESSAGE_LENGTH;
                    if (extra > 0) {
                        MsoyUI.error(CMsgs.mmsgs.errMessageTooLong(""+extra));
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
        }

        protected MessageEditor _editor;
    }

    protected class PostEditorPanel extends TableFooterPanel
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

    protected class ThreadFlagsEditorPanel extends BorderedDialog
    {
        public ThreadFlagsEditorPanel ()
        {
            _header.add(createTitleLabel(CMsgs.mmsgs.tfepTitle(), null));

            FlexTable contents = new FlexTable();
            contents.setCellSpacing(10);

            int row = 0;
            contents.setText(row, 0, CMsgs.mmsgs.tfepIntro());
            contents.getFlexCellFormatter().setColSpan(row++, 0, 2);

            contents.setText(row, 0, CMsgs.mmsgs.tfepAnnouncement());
            contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            contents.setWidget(row++, 1, _announce = new CheckBox());
            _announce.setChecked(_thread.isAnnouncement());

            contents.setText(row, 0, CMsgs.mmsgs.tfepSticky());
            contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            contents.setWidget(row++, 1, _sticky = new CheckBox());
            _sticky.setChecked(_thread.isSticky());

            contents.setText(row, 0, CMsgs.mmsgs.tfepLocked());
            contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            contents.setWidget(row++, 1, _locked = new CheckBox());
            _locked.setChecked(_thread.isLocked());

            ((VerticalPanel)_contents).setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
            ((VerticalPanel)_contents).add(contents);

            _footer.add(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget widget) {
                    ThreadFlagsEditorPanel.this.hide();
                }
            }));

            Button update = new Button(CMsgs.cmsgs.update());
            new ClickCallback(update) {
                public boolean callService () {
                    _flags |= (_announce.isChecked() ? ForumThread.FLAG_ANNOUNCEMENT : 0);
                    _flags |= (_sticky.isChecked() ? ForumThread.FLAG_STICKY : 0);
                    _flags |= (_locked.isChecked() ? ForumThread.FLAG_LOCKED : 0);
                    CMsgs.forumsvc.updateThreadFlags(CMsgs.ident, _thread.threadId, _flags, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    _thread.flags = _flags;
                    // TODO: have ForumModels update all instances of this thread
                    MsoyUI.info(CMsgs.mmsgs.tfepUpdated());
                    ThreadFlagsEditorPanel.this.hide();
                    // update the thread panel title
                    gotThread(_thread);
                    return true;
                }
                protected int _flags;
            };
            _footer.add(update);
        }

        // @Override // from BorderedDialog
        protected Widget createContents ()
        {
            return new VerticalPanel();
        }

        protected CheckBox _announce, _sticky, _locked;
    }

    protected int _threadId;
    protected ForumThread _thread;
    protected MessagesPanel _mpanel;
}
