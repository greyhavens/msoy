//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.fora.data.ForumThread;

import client.util.ClickCallback;
import client.util.MsoyUI;

/**
 * Displays an interface for creating a new thread.
 */
public class NewThreadPanel extends VerticalPanel
{
    public NewThreadPanel (int groupId)
    {
        _groupId = groupId;

        setStyleName("newThreadPanel");

        FlexTable content = new FlexTable();
        content.setStyleName("Content");
        content.setCellPadding(0);
        content.setCellSpacing(5);
        add(content);

        addRow(content, CMsgs.mmsgs.ntpSubject(), _subject = new TextBox());
        _subject.setMaxLength(ForumThread.MAX_SUBJECT_LENGTH);
        _subject.setVisibleLength(40);

        addRow(content, CMsgs.mmsgs.ntpFirstMessage());
        addRow(content, _message = new MessageEditor());

        FlowPanel footer = new FlowPanel();
        footer.setStyleName("Footer");
        footer.add(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                ((ForumPanel)getParent()).displayGroupThreads(_groupId);
            }
        }));
        Button submit = new Button(CMsgs.cmsgs.submit());
        new ClickCallback(submit) {
            public boolean callService () {
                return submitNewThread(this);
            }
            public boolean gotResult (Object result) {
                ((ForumPanel)getParent()).newThreadPosted((ForumThread)result);
                return false;
            }
        };
        footer.add(submit);
        add(footer);
    }

    protected boolean submitNewThread (ClickCallback callback)
    {
        String subject = _subject.getText().trim();
        if (subject.length() == 0) {
            MsoyUI.error(CMsgs.mmsgs.errNoSubject());
            return false;
        }

        String message = _message.getHTML();
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
    protected MessageEditor _message;
}
