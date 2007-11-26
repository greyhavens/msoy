//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.fora.data.ForumThread;

import client.util.ClickCallback;
import client.util.MsoyUI;

/**
 * Displays an interface for creating a new thread.
 */
public class NewThreadPanel extends ContentFooterPanel
{
    public NewThreadPanel (int groupId)
    {
        _groupId = groupId;

        addRow(CMsgs.mmsgs.ntpSubject(), _subject = new TextBox());
        _subject.setMaxLength(ForumThread.MAX_SUBJECT_LENGTH);
        _subject.setVisibleLength(40);

        addRow(CMsgs.mmsgs.ntpFirstMessage());
        addRow(_message = new MessageEditor());

        _footer.add(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
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
        _footer.add(submit);
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

    protected int _groupId;
    protected TextBox _subject;
    protected MessageEditor _message;
}
