//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ScrollPanel;
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
    public MessagesPanel (ThreadPanel parent, int scrollToId)
    {
        super(MESSAGES_PER_PAGE, 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
        setWidth("100%");
        setHeight("100%");

        _parent = parent;
        _scrollToId = scrollToId;
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
    protected Widget createWidget (Object item)
    {
        ForumMessage msg = (ForumMessage)item;
        final ThreadMessagePanel panel = new ThreadMessagePanel(
            ((ForumModels.ThreadMessages)_model).getThread(), msg);
        if (msg.messageId == _scrollToId) {
            _scrollToId = 0;
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    _scrolly.ensureVisible(panel);
                }
            });
        }
        return panel;
    }

    // @Override // from PagedGrid
    protected void addGrid (Grid grid)
    {
        add(_scrolly = new ScrollPanel(grid));
        int availHeight = (Window.getClientHeight() - USED_HEIGHT);
        setMaxHeight(_scrolly.getElement(), availHeight + "px");
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

    // I should be able to do:
    // DOM.setStyleAttribute(_scrolly.getElement(), "max-height", availHeight + "px");
    // but it doesn't work on Firefox for some reason. Yay!
    protected static native void setMaxHeight (Element elem, String maxHeight) /*-{
        elem.style.maxHeight = maxHeight;
    }-*/;

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

            if (CMsgs.getMemberId() != 0 &&
                CMsgs.getMemberId() != _message.poster.name.getMemberId()) {
                info.add(makeInfoLabel(CMsgs.mmsgs.inlineMail(), new ClickListener() {
                    public void onClick (Widget sender) {
                        new MailComposition(_message.poster.name,
                                            CMsgs.mmsgs.mailRe(_thread.subject), null, null).show();
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

    /** We scroll our messages in this panel. */
    protected ScrollPanel _scrolly;

    /** A message to scroll into view when we first receive our messages. */
    protected int _scrollToId;

    /** A button for posting a reply message. */
    protected Button _postReply;

    /** A button for editing this thread's flags. */
    protected Button _editFlags;

    protected static final int MESSAGES_PER_PAGE = 10;
    protected static final int USED_HEIGHT =
        50 /* navi */ + 20 /* title */ + 30 /* thread title */ + 20 /* footer */;
}
