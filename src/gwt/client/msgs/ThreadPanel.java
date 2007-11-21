//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.fora.data.ForumMessage;

import client.shell.MessagePanel;

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
                _parent.postReplyMessage(_threadId);
            }
        });
        _postReply.setEnabled(false);
        controls.setWidget(0, 0, _postReply);
    }

    // @Override // from PagedGrid
    protected void displayResults (int start, int count, List list)
    {
        super.displayResults(start, count, list);
        _postReply.setEnabled(((ForumModels.ThreadMessages)_model).canPostReply());
    }

    protected class ThreadMessagePanel extends MessagePanel
    {
        public ThreadMessagePanel (ForumMessage message)
        {
            setMessage(message.poster, message.created, message.message);
        }
    }

    /** The forum panel in which we're hosted. */
    protected ForumPanel _parent;

    /** Contains the id of the thread whose messages we are displaying. */
    protected int _threadId;

    /** A button for posting a reply message. */
    protected Button _postReply;

    protected static final int MESSAGES_PER_PAGE = 20;
}
