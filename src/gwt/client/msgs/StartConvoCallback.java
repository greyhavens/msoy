//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.person.gwt.MailPayload;
import com.threerings.msoy.person.gwt.MailService;
import com.threerings.msoy.person.gwt.MailServiceAsync;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.ServiceUtil;

/**
 * A callback that handles starting a conversation.
 */
public abstract class StartConvoCallback extends ClickCallback<Void>
{
    public StartConvoCallback (SourcesClickEvents trigger, TextBox subject, TextArea body)
    {
        super(trigger);
        _subject = subject;
        _body = body;
    }

    @Override // from ClickCallback
    public boolean callService ()
    {
        String subject = _subject.getText().trim();
        String body = _body.getText().trim();
        if (subject.length() == 0) {
            MsoyUI.error(CMsgs.mmsgs.sccMissingSubject());
            return false;
        }
        if (body.length() == 0) {
            MsoyUI.error(CMsgs.mmsgs.sccMissingBody());
            return false;
        }
        _mailsvc.startConversation(
            CMsgs.ident, getRecipientId(), subject, body, getPayload(), this);
        return true;
    }

    protected abstract int getRecipientId ();

    protected MailPayload getPayload ()
    {
        return null;
    }

    protected TextBox _subject;
    protected TextArea _body;

    protected static final MailServiceAsync _mailsvc = (MailServiceAsync)
        ServiceUtil.bind(GWT.create(MailService.class), MailService.ENTRY_POINT);
}
