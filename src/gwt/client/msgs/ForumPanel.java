//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.InlineLabel;
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

        FlowPanel header = new FlowPanel();
        header.setStyleName("Header");
        header.add(MsoyUI.createLabel(CMsgs.mmsgs.groupThreadListHeader(), "Title"));
        add(header);

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

    public void displayThreadMessages (final ForumThread thread)
    {
        clear();
        FlexTable header = new FlexTable();
        header.setCellSpacing(0);
        header.setCellPadding(0);
        header.setWidth("100%");
        header.setStyleName("Header");
        header.setWidget(0, 0, MsoyUI.createActionLabel("", "Back", new ClickListener() {
            public void onClick (Widget sender) {
                displayGroupThreads(thread.groupId);
            }
        }));
        header.setText(0, 1, thread.isAnnouncement() ?
                       CMsgs.mmsgs.groupAnnouncementHeader(thread.subject) :
                       CMsgs.mmsgs.groupThreadHeader(thread.subject));
        header.getFlexCellFormatter().setStyleName(0, 1, "Title");
        header.getFlexCellFormatter().setWidth(0, 1, "100%");
        add(header);
        add(new ThreadPanel(this, thread.threadId));
    }

    public void postReplyMessage (int threadId)
    {
        // TODO
    }

    protected void newThreadPosted (ForumThread thread)
    {
        MsoyUI.info(CMsgs.mmsgs.msgNewThreadPosted());
        // TODO: add it to our local model and reuse our cached model
        displayGroupThreads(thread.groupId);
    }

    protected class NewThreadPanel extends VerticalPanel
    {
        public NewThreadPanel (int groupId)
        {
            _groupId = groupId;

            setStyleName("newThreadPanel");

            add(MsoyUI.createLabel(CMsgs.mmsgs.startNewThread(), "Header"));

            FlexTable content = new FlexTable();
            content.setStyleName("Content");
            content.setCellPadding(0);
            content.setCellSpacing(5);
            add(content);

            addRow(content, CMsgs.mmsgs.ntpSubject(), _subject = new TextBox());
            _subject.setMaxLength(ForumThread.MAX_SUBJECT_LENGTH);
            _subject.setVisibleLength(40);

            addRow(content, CMsgs.mmsgs.ntpFirstMessage());

            addRow(content, _message = new TextArea());
            _message.setCharacterWidth(80);
            _message.setVisibleLines(8);

            FlexTable buttons = new FlexTable();
            buttons.setCellPadding(0);
            buttons.setCellSpacing(0);
            buttons.getFlexCellFormatter().setStyleName(0, 0, "FooterLeft");
            buttons.setWidget(0, 1, new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    ((ForumPanel)getParent()).displayGroupThreads(_groupId);
                }
            }));
            Button submit = new Button(CMsgs.cmsgs.submit());
            buttons.setWidget(0, 2, submit);
            new ClickCallback(submit) {
                public boolean callService () {
                    return submitNewThread(this);
                }
                public boolean gotResult (Object result) {
                    ((ForumPanel)getParent()).newThreadPosted((ForumThread)result);
                    return false;
                }
            };
            add(buttons);
        }

        protected boolean submitNewThread (ClickCallback callback)
        {
            String subject = _subject.getText().trim();
            if (subject.length() == 0) {
                MsoyUI.error(CMsgs.mmsgs.errNoSubject());
                return false;
            }

            String message = _message.getText().trim();
            if (message.length() == 0) {
                MsoyUI.error(CMsgs.mmsgs.errNoMessage());
                return false;
            }

            CMsgs.forumsvc.createThread(CMsgs.ident, _groupId, 0, subject, message, callback);
            return true;
        }

        protected int addRow (FlexTable table, String label, Widget widget)
        {
            int row = table.getRowCount();
            table.setText(row, 0, label);
            table.setWidget(row, 1, widget);
            return row;
        }

        protected int addRow (FlexTable table, String text)
        {
            int row = table.getRowCount();
            table.setText(row, 0, text);
            table.getFlexCellFormatter().setColSpan(row, 0, 2);
            return row;
        }

        protected int addRow (FlexTable table, Widget widget)
        {
            int row = table.getRowCount();
            table.setWidget(row, 0, widget);
            table.getFlexCellFormatter().setColSpan(row, 0, 2);
            return row;
        }

        protected int _groupId;
        protected TextBox _subject;
        protected TextArea _message;
    }
}
