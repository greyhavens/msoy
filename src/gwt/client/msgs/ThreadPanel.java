//
// $Id$

package client.msgs;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.fora.data.ForumMessage;

import client.shell.MessagePanel;
import client.util.ClickCallback;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;

/**
 * Displays the messages in a particular thread.
 */
public class ThreadPanel extends PagedGrid
{
    public ThreadPanel (ForumPanel parent, int threadId)
    {
        super(MESSAGES_PER_PAGE, 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
        setWidth("100%");

        _parent = parent;
        _threadId = threadId;
        setModel(new ForumModels.ThreadMessages(threadId), 0);
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new ThreadMessagePanel((ForumMessage)item);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CMsgs.mmsgs.noMessages();
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true; // we always show our navigation for consistency
    }

    // @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for starting a new message that will optionally be enabled later
        _postReply = new Button(CMsgs.mmsgs.postReply(), new ClickListener() {
            public void onClick (Widget sender) {
                postReplyMessage(null);
            }
        });
        _postReply.setEnabled(false);
        controls.setWidget(0, 0, _postReply);
    }

    // @Override // from PagedGrid
    protected void displayResults (int start, int count, List list)
    {
        _postReply.setEnabled(((ForumModels.ThreadMessages)_model).canPostReply() && _reply == null);
        super.displayResults(start, count, list);
    }

    protected void postReplyMessage (final ForumMessage message)
    {
        if (_reply == null) {
            _parent.add(_reply = new ReplyPanel(message));
            _postReply.setEnabled(false);
        }
    }

    protected void clearReplyPanel ()
    {
        if (_reply != null) {
            _parent.remove(_reply);
            _reply = null;
            _postReply.setEnabled(true);
        }
    }

    protected void replyPosted (ForumMessage message)
    {
        MsoyUI.info(CMsgs.mmsgs.msgReplyPosted());
        ((ForumModels.ThreadMessages)_model).appendItem(message);
        displayPage(_page, true);
    }

    protected void deletePost (final ForumMessage message, boolean confirmed)
    {
        if (!confirmed) {
            // TODO: if forum admin, make them send a mail to the poster explaining why their post
            // was deleted?
            new PromptPopup(CMsgs.mmsgs.confirmDelete()) {
                public void onAffirmative () {
                    deletePost(message, true);
                }
            }.prompt();
            return;
        }

        CMsgs.forumsvc.deleteMessage(CMsgs.ident, message.messageId, new MsoyCallback() {
            public void onSuccess (Object result) {
                removeItem(message);
                MsoyUI.info(CMsgs.mmsgs.msgPostDeleted());
            }
        });
    }

    protected class ThreadMessagePanel extends MessagePanel
    {
        public ThreadMessagePanel (ForumMessage message)
        {
            setMessage(message);
        }

        public void setMessage (ForumMessage message)
        {
            _message = message;
            setMessage(message.poster, message.created, message.message);

            if (!message.lastEdited.equals(message.created)) {
                int row = getRowCount();
                getFlexCellFormatter().setRowSpan(0, 0, row+1); // extend the photo cell
                setText(row, 0, "Edited on " + _pfmt.format(message.lastEdited));
                getFlexCellFormatter().setStyleName(row, 0, "Posted");
                getFlexCellFormatter().addStyleName(row, 0, "LeftPad");
            }
        }

        // @Override // from MessagePanel
        protected boolean textIsHTML ()
        {
            return true;
        }

        // @Override // from MessagePanel
        protected void addInfo (FlowPanel info)
        {
            super.addInfo(info);

            if (_postReply.isEnabled()) {
                InlineLabel reply = new InlineLabel(CMsgs.mmsgs.inlineReply(), false, true, false);
                reply.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        postReplyMessage(_message);
                    }
                });
                reply.addStyleName("Posted");
                reply.addStyleName("actionLabel");
                info.add(reply);
            }

            if (CMsgs.getMemberId() == _message.poster.name.getMemberId()) {
                InlineLabel edit = new InlineLabel(CMsgs.mmsgs.inlineEdit(), false, true, false);
                edit.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        setWidget(1, 0, new PostEditorPanel(ThreadMessagePanel.this, _message));
                    }
                });
                edit.addStyleName("Posted");
                edit.addStyleName("actionLabel");
                info.add(edit);
            }

            // TODO: also if forum admin
            if (CMsgs.getMemberId() == _message.poster.name.getMemberId()) {
                InlineLabel delete = new InlineLabel(CMsgs.mmsgs.inlineDelete(), false, true, false);
                delete.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        deletePost(_message, false);
                    }
                });
                delete.addStyleName("Posted");
                delete.addStyleName("actionLabel");
                info.add(delete);
            }
        }

        protected ForumMessage _message;
    }

    protected class ReplyPanel extends FlexTable
    {
        public ReplyPanel (ForumMessage inReplyTo)
        {
            setStyleName("replyPanel");
            setText(0, 0, CMsgs.mmsgs.postReplyTitle());
            getFlexCellFormatter().setStyleName(0, 0, "Title");

            setWidget(1, 0, _editor = new MessageEditor());

            // TODO: set quote text

            HorizontalPanel buttons = new HorizontalPanel();
            buttons.add(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    clearReplyPanel();
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
                    clearReplyPanel();
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
        public PostEditorPanel (final ThreadMessagePanel parent, final ForumMessage message)
        {
            _parent = parent;
            _message = message;

            add(_editor = new MessageEditor());
            _editor.setHTML(message.message);
            setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
            HorizontalPanel buttons = new HorizontalPanel();
            buttons.add(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    close();
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
                    close();
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

        protected void close ()
        {
            _parent.setMessage(_message);
        }

        protected ThreadMessagePanel _parent;
        protected ForumMessage _message;
        protected MessageEditor _editor;
    }

    /** The forum panel in which we're hosted. */
    protected ForumPanel _parent;

    /** Contains the id of the thread whose messages we are displaying. */
    protected int _threadId;

    /** A button for posting a reply message. */
    protected Button _postReply;

    /** Our active reply panel if any. */
    protected ReplyPanel _reply;

    protected static final int MESSAGES_PER_PAGE = 10;
}
