//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.FriendInvitePayload;
import com.threerings.msoy.person.gwt.MailPayload;

import client.msgs.StartConvoCallback;
import client.shell.ShellMessages;
import client.util.BorderedDialog;
import client.util.MsoyUI;

/**
 * Displays a popup requesting to invite someone as your friend.
 */
public class InviteFriendPopup extends BorderedDialog
{
    public static ClickListener createListener (final MemberName target)
    {
        return new ClickListener() {
            public void onClick (Widget sender) {
                new InviteFriendPopup(target).show();
            }
        };
    }

    protected InviteFriendPopup (final MemberName target)
    {
        setHeaderTitle(CPeople.msgs.ifriendTitle(""+target));

        SmartTable contents = new SmartTable("inviteFriend", 0, 5);
        contents.setText(0, 0, CPeople.msgs.ifriendTo(), 1, "rightLabel");
        contents.setText(0, 1, ""+target);

        contents.setText(1, 0, CPeople.msgs.ifriendSubject(), 1, "rightLabel");
        contents.setWidget(1, 1, _subject = new TextBox());
        _subject.setWidth("300px");
        _subject.setText(CPeople.msgs.ifriendDefSubject());

        contents.setText(2, 0, CPeople.msgs.ifriendMessage(), 1, "rightLabel");
        contents.setWidget(2, 1, _body = new TextArea());
        _body.setWidth("300px");
        _body.setVisibleLines(5);
        _body.setText(CPeople.msgs.ifriendDefMessage());

        setContents(contents);

        Button send = new Button(CPeople.msgs.ifriendSend());
        new StartConvoCallback(send, _subject, _body) {
            public boolean gotResult (Void result) {
                MsoyUI.info(CPeople.msgs.ifriendSent());
                hide();
                return false;
            }
            protected int getRecipientId () {
                return target.getMemberId();
            }
            protected MailPayload getPayload () {
                return new FriendInvitePayload();
            }
        };
        addButton(send);

        addButton(new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        }));
    }

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        _subject.setFocus(true);
    }

    protected TextBox _subject;
    protected TextArea _body;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
