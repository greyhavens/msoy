//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.fora.data.ForumMessage;

import client.shell.MessagePanel;
import client.util.ClickCallback;
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
        _postReply.setEnabled(((ForumModels.ThreadMessages)_model).canPostReply());
        super.displayResults(start, count, list);
    }

    protected void postReplyMessage (final ForumMessage message)
    {
        if (_reply != null) {
            new PromptPopup(CMsgs.mmsgs.replaceInProgressReply()) {
                public void onAffirmative () {
                    clearReplyPanel();
                    postReplyMessage(message);
                }
            }.prompt();
        } else {
            _parent.add(_reply = new ReplyPanel(message));
        }
    }

    protected void clearReplyPanel ()
    {
        if (_reply != null) {
            _parent.remove(_reply);
            _reply = null;
        }
    }

    protected void replyPosted (ForumMessage message)
    {
        MsoyUI.info(CMsgs.mmsgs.msgReplyPosted());
        ((ForumModels.ThreadMessages)_model).appendItem(message);
        displayPage(_page, true);
    }

    protected class ThreadMessagePanel extends MessagePanel
    {
        public ThreadMessagePanel (ForumMessage message)
        {
            _message = message;
            setMessage(message.poster, message.created, message.message);
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
        }

        protected ForumMessage _message;
    }

    protected class ReplyPanel extends FlexTable
    {
        public ReplyPanel (ForumMessage inReplyTo)
        {
            setStyleName("replyPanel");
            setText(0, 0, "Post a message to this thread:");

            setWidget(1, 0, _message = new TextArea());
            _message.setCharacterWidth(80);
            _message.setVisibleLines(4);

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
                    String text = _message.getText().trim();
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

        protected TextArea _message;
    }

    /** The forum panel in which we're hosted. */
    protected ForumPanel _parent;

    /** Contains the id of the thread whose messages we are displaying. */
    protected int _threadId;

    /** A button for posting a reply message. */
    protected Button _postReply;

    /** Our active reply panel if any. */
    protected ReplyPanel _reply;

    protected static final int MESSAGES_PER_PAGE = 20;
}
