//
// $Id$

package client.msgs;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;

import client.shell.Args;
import client.shell.MessagePanel;
import client.shell.Page;
import client.util.BorderedDialog;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.SearchBox;

/**
 * Displays a thread header and either its messages or a post creation or editing panel.
 */
public class ThreadPanel extends TitledListPanel
    implements SearchBox.Listener
{
    public ThreadPanel ()
    {
        _theader = new SmartTable(0, 0);
        _theader.setWidth("100%");
        _theader.setWidget(0, 0, MsoyUI.createBackArrow(), 1, "Back");
        _theader.setText(0, 1, "", 1, "Whirled");
        _theader.setText(0, 2, "...", 1, "Title");
        _theader.setWidget(0, 3, _search = new SearchBox(this), 1, "Search");
    }

    public void showThread (ForumModels fmodels, int threadId, int page, int scrollToId)
    {
        _threadId = threadId;
        _mpanel = new MessagesPanel(
            this, new ForumModels.ThreadMessages(threadId, fmodels.findThread(threadId)),
            page, scrollToId);
        showMessages();
    }

    public void showMessages ()
    {
        showMessages(false);
    }

    public void showMessages (boolean refresh)
    {
        setContents(_theader, _mpanel);
        if (refresh) {
            _mpanel.refreshDisplay();
        }
    }

    public int getThreadId ()
    {
        return _threadId;
    }

    public void gotThread (ForumThread thread)
    {
        _thread = thread;
        _theader.setText(0, 2, _thread.subject);
        _theader.setWidget(0, 1, Link.create(
                               _thread.group + ":", Page.WHIRLEDS,
                               Args.compose("f", _thread.group.getGroupId())));
    }

    public void editFlags ()
    {
        new ThreadFlagsEditorPanel().show();
    }

    public void postReply (ForumMessage inReplyTo, boolean quote)
    {
        setContents(CMsgs.mmsgs.threadReplyHeader(_thread.subject),
                    new ReplyPanel(inReplyTo, quote));
    }

    public void editPost (ForumMessage message, AsyncCallback<ForumMessage> callback)
    {
        setContents(_thread.subject, new PostEditorPanel(message, callback));
    }

    public void newIssue (ForumMessage message)
    {
        setContents(CMsgs.mmsgs.newIssue(), new EditIssuePanel(this, message));
    }

    // from interface SearchBox.Listener
    public void search (String query)
    {
        CMsgs.forumsvc.findMessages(
            CMsgs.ident, _threadId, query, MAX_RESULTS, new MsoyCallback<List<ForumMessage>>() {
            public void onSuccess (List<ForumMessage> messages) {
                _mpanel.setModel(new SimpleDataModel<ForumMessage>(messages), 0);
            }
        });
    }

    // from interface SearchBox.Listener
    public void clearSearch ()
    {
        _mpanel.restoreThread();
    }

    protected void replyPosted (ForumMessage message)
    {
        _mpanel.replyPosted(message);
        showMessages();
    }

    protected static boolean checkMessageText (String text)
    {
        if (text.length() == 0) {
            MsoyUI.error(CMsgs.mmsgs.errMissingReply());
            return false;
        }
        return true;
    }

    protected class ReplyPanel extends TableFooterPanel
    {
        public ReplyPanel (ForumMessage inReplyTo, boolean quote)
        {
            _content.setWidget(0, 0, _editor = new MessageEditor());

            if (inReplyTo != null) {
                // set the quote text if available
                if (quote) {
                    _editor.setHTML(
                        CMsgs.mmsgs.replyQuote(inReplyTo.poster.name.toString(), inReplyTo.message));
                    DeferredCommand.addCommand(new Command() {
                        public void execute () {
                            _editor.getTextArea().getBasicFormatter().selectAll();
                        }
                    });
                }

                // display the message to which we are replying below everything
                MessagePanel reply = new MessagePanel() {
                    public boolean textIsHTML () {
                        return true;
                    }
                };
                reply.setMessage(inReplyTo.poster, inReplyTo.created, inReplyTo.message);

                int row = getRowCount();
                setWidget(row++, 0, WidgetUtil.makeShim(10, 10));
                getFlexCellFormatter().setStyleName(row, 0, "Header");
                setWidget(row++, 0, MsoyUI.createLabel(CMsgs.mmsgs.replyInReplyTo(), "Title"));
                setWidget(row++, 0, reply);
            }

            addFooterButton(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    showMessages();
                }
            }));
            Button submit = new Button(CMsgs.cmsgs.send());
            final int replyId = (inReplyTo == null) ? 0 : inReplyTo.messageId;
            new ForumCallback<ForumMessage>(submit) {
                public boolean callService () {
                    String text = _editor.getHTML();
                    if (!checkMessageText(text)) {
                        return false;
                    }
                    CMsgs.forumsvc.postMessage(CMsgs.ident, _threadId, replyId, text, this);
                    return true;
                }
                public boolean gotResult (ForumMessage result) {
                    replyPosted(result);
                    return false;
                }
            };
            addFooterButton(submit);
        }

        @Override // from Widget
        protected void onAttach ()
        {
            super.onAttach();
            _editor.setFocus(true);
        }

        protected MessageEditor _editor;
    }

    protected class PostEditorPanel extends TableFooterPanel
    {
        public PostEditorPanel (ForumMessage message, AsyncCallback<ForumMessage> callback)
        {
            _message = message;
            _callback = callback;

            _content.setWidget(0, 0, _editor = new MessageEditor());
            _editor.setHTML(message.message);

            addFooterButton(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    showMessages();
                }
            }));

            Button submit = new Button(CMsgs.cmsgs.change());
            new ForumCallback<Void>(submit) {
                public boolean callService () {
                    _text = _editor.getHTML();
                    if (!checkMessageText(_text)) {
                        return false;
                    }
                    CMsgs.forumsvc.editMessage(CMsgs.ident, _message.messageId, _text, this);
                    return true;
                }
                public boolean gotResult (Void result) {
                    MsoyUI.info(CMsgs.mmsgs.msgPostUpdated());
                    _message.message = _text;
                    _message.lastEdited = new Date();
                    _callback.onSuccess(_message);
                    showMessages();
                    return false;
                }
                protected String _text;
            };
            addFooterButton(submit);
        }

        @Override // from Widget
        protected void onAttach ()
        {
            super.onAttach();
            _editor.setFocus(true);
        }

        protected ForumMessage _message;
        protected AsyncCallback<ForumMessage> _callback;
        protected MessageEditor _editor;
    }

    protected class ThreadFlagsEditorPanel extends BorderedDialog
    {
        public ThreadFlagsEditorPanel ()
        {
            setHeaderTitle(CMsgs.mmsgs.tfepTitle());

            FlexTable main = new FlexTable();
            main.setCellSpacing(10);

            int row = 0;
            main.setText(row, 0, CMsgs.mmsgs.tfepIntro());
            main.getFlexCellFormatter().setColSpan(row++, 0, 2);

            main.setText(row, 0, CMsgs.mmsgs.tfepAnnouncement());
            main.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            main.setWidget(row++, 1, _announce = new CheckBox());
            _announce.setChecked(_thread.isAnnouncement());

            main.setText(row, 0, CMsgs.mmsgs.tfepSticky());
            main.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            main.setWidget(row++, 1, _sticky = new CheckBox());
            _sticky.setChecked(_thread.isSticky());

            main.setText(row, 0, CMsgs.mmsgs.tfepLocked());
            main.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
            main.setWidget(row++, 1, _locked = new CheckBox());
            _locked.setChecked(_thread.isLocked());
            setContents(main);

            addButton(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget widget) {
                    ThreadFlagsEditorPanel.this.hide();
                }
            }));

            Button update = new Button(CMsgs.cmsgs.update());
            new ClickCallback<Void>(update) {
                public boolean callService () {
                    _flags |= (_announce.isChecked() ? ForumThread.FLAG_ANNOUNCEMENT : 0);
                    _flags |= (_sticky.isChecked() ? ForumThread.FLAG_STICKY : 0);
                    _flags |= (_locked.isChecked() ? ForumThread.FLAG_LOCKED : 0);
                    CMsgs.forumsvc.updateThreadFlags(CMsgs.ident, _thread.threadId, _flags, this);
                    return true;
                }
                public boolean gotResult (Void result) {
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
            addButton(update);
        }

        protected CheckBox _announce, _sticky, _locked;
    }

    protected int _threadId;
    protected ForumThread _thread;
    protected SmartTable _theader;
    protected SearchBox _search;
    protected MessagesPanel _mpanel;

    protected static final int MAX_RESULTS = 20;
}
