//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.fora.data.ForumThread;

import client.util.ClickCallback;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays forum threads and messages.
 */
public class ForumPanel extends VerticalPanel
{
    public ForumPanel ()
    {
        setStyleName("forumPanel");
    }

    public void displayGroupThreads (int groupId)
    {
        clear();

        add(MsoyUI.createLabel(CMsgs.mmsgs.groupThreadListHeader(), "Header"));

        // TODO: cache group thread models...
        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayGroupThreads(groupId);
        add(threads);
    }

    public void startNewThread (int groupId)
    {
        clear();
        add(new NewThreadPanel(groupId));
    }

    public void displayThreadMessages (ForumThread thread)
    {
        clear();
        if (thread.isAnnouncement()) {
            add(MsoyUI.createLabel(CMsgs.mmsgs.groupAnnouncementHeader(thread.subject), "Header"));
        } else {
            add(MsoyUI.createLabel(CMsgs.mmsgs.groupThreadHeader(thread.subject), "Header"));
        }
        add(new ThreadPanel(this, thread.threadId));
    }

    public void postReplyMessage (int threadId)
    {
        // TODO
    }

    protected void newThreadPosted (ForumThread thread)
    {
        MsoyUI.info("New thread posted.");
        // TODO: add it to our local model and reuse our cached model
        displayGroupThreads(thread.groupId);
    }

    protected class NewThreadPanel extends FlexTable
    {
        public NewThreadPanel (int groupId)
        {
            _groupId = groupId;

            setStyleName("newThreadPanel");
            setCellPadding(0);
            setCellSpacing(5);

            addRow("Start New Thread");

            addRow("Subject:", _subject = new TextBox());
            _subject.setMaxLength(ForumThread.MAX_SUBJECT_LENGTH);
            _subject.setVisibleLength(80);

            addRow("First message:");

            addRow(_message = new TextArea());
            _message.setCharacterWidth(80);
            _message.setVisibleLines(8);
            getFlexCellFormatter().setColSpan(3, 0, 2);

            RowPanel buttons = new RowPanel();
            buttons.add(new Button("Cancel", new ClickListener() {
                public void onClick (Widget sender) {
                    ((ForumPanel)getParent()).displayGroupThreads(_groupId);
                }
            }));
            Button submit = new Button("Submit");
            buttons.add(submit);
            new ClickCallback(submit) {
                public boolean callService () {
                    return submitNewThread(this);
                }
                public boolean gotResult (Object result) {
                    ((ForumPanel)getParent()).newThreadPosted((ForumThread)result);
                    return false;
                }
            };
            int brow = addRow(buttons);
            getFlexCellFormatter().setHorizontalAlignment(brow, 0, HasAlignment.ALIGN_RIGHT);
        }

        protected boolean submitNewThread (ClickCallback callback)
        {
            String subject = _subject.getText().trim();
            if (subject.length() == 0) {
                MsoyUI.error("Please enter a subject for your new thread.");
                return false;
            }

            String message = _message.getText().trim();
            if (message.length() == 0) {
                MsoyUI.error("Please enter the starting message for your new thread.");
                return false;
            }

            CMsgs.forumsvc.createThread(CMsgs.ident, _groupId, 0, subject, message, callback);
            return true;
        }

        protected int addRow (String label, Widget widget)
        {
            int row = getRowCount();
            setText(row, 0, label);
            setWidget(row, 1, widget);
            return row;
        }

        protected int addRow (String text)
        {
            int row = getRowCount();
            setText(row, 0, text);
            getFlexCellFormatter().setColSpan(row, 0, 2);
            return row;
        }

        protected int addRow (Widget widget)
        {
            int row = getRowCount();
            setWidget(row, 0, widget);
            getFlexCellFormatter().setColSpan(row, 0, 2);
            return row;
        }

        protected int _groupId;
        protected TextBox _subject;
        protected TextArea _message;
    }
}
