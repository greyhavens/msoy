//
// $Id$

package client.msgs;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;

import client.shell.MessagePanel;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;

/**
 * Displays the messages in a particular thread.
 */
public class MessagesPanel extends PagedGrid
{
    public MessagesPanel (ThreadPanel parent)
    {
        super(MESSAGES_PER_PAGE, 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
        setWidth("100%");

        _parent = parent;
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new ThreadMessagePanel(
            ((ForumModels.ThreadMessages)_model).getThread(), (ForumMessage)item);
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
                _parent.postReply(null);
            }
        });
        _postReply.setEnabled(false);
        controls.setWidget(0, 0, _postReply);

        // add a button for editing this thread's flags
        _editFlags = new Button(CMsgs.mmsgs.editFlags(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.editFlags();
            }
        });
        _editFlags.setEnabled(false);
        controls.setWidget(0, 1, _editFlags);
    }

    // @Override // from PagedGrid
    protected void displayResults (int start, int count, List list)
    {
        ForumModels.ThreadMessages tmodel = (ForumModels.ThreadMessages)_model;
        _parent.gotThread(tmodel.getThread(), tmodel.getGroup());
        _postReply.setEnabled(tmodel.canPostReply() && !tmodel.getThread().isLocked());
        _editFlags.setEnabled(tmodel.isManager());
        super.displayResults(start, count, list);
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

    protected static InlineLabel makeInfoLabel (String text, ClickListener listener)
    {
        InlineLabel label = new InlineLabel(text, false, true, false);
        label.addClickListener(listener);
        label.addStyleName("Posted");
        label.addStyleName("actionLabel");
        return label;
    }

    protected class ThreadMessagePanel extends MessagePanel
    {
        public ThreadMessagePanel (ForumThread thread, ForumMessage message)
        {
            _thread = thread;
            setMessage(message);
        }

        public void setMessage (ForumMessage message)
        {
            _message = message;
            setMessage(message.poster, message.created, message.message);

            if (!message.lastEdited.equals(message.created)) {
                getFlexCellFormatter().setRowSpan(0, 0, 3); // extend the photo cell
                setText(2, 0, "Edited on " + _pfmt.format(message.lastEdited));
                getFlexCellFormatter().setStyleName(2, 0, "Posted");
                getFlexCellFormatter().addStyleName(2, 0, "LeftPad");
                getFlexCellFormatter().addStyleName(2, 0, "BottomPad");
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
                info.add(makeInfoLabel(CMsgs.mmsgs.inlineReply(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.postReply(null);
                    }
                }));
                info.add(makeInfoLabel(CMsgs.mmsgs.inlineQReply(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.postReply(_message);
                    }
                }));
            }

            if (CMsgs.getMemberId() == _message.poster.name.getMemberId()) {
                info.add(makeInfoLabel(CMsgs.mmsgs.inlineEdit(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _parent.editPost(_message, new MsoyCallback() {
                            public void onSuccess (Object result) {
                                setMessage((ForumMessage)result);
                            }
                        });
                    }
                }));
            }

            // TODO: also if forum admin
            if (CMsgs.getMemberId() == _message.poster.name.getMemberId()) {
                info.add(makeInfoLabel(CMsgs.mmsgs.inlineDelete(), new ClickListener() {
                    public void onClick (Widget sender) {
                        deletePost(_message, false);
                    }
                }));
            }
        }

        // @Override // from MessagePanel
        protected String getIconPath ()
        {
            return "/images/msgs/" +
                ((_message.messageId > _thread.lastReadPostId) ? "unread" : "read") + ".png";
        }

        protected ForumThread _thread;
        protected ForumMessage _message;
    }

    /** The thread panel in which we're hosted. */
    protected ThreadPanel _parent;

    /** A button for posting a reply message. */
    protected Button _postReply;

    /** A button for editing this thread's flags. */
    protected Button _editFlags;

    protected static final int MESSAGES_PER_PAGE = 10;
}
