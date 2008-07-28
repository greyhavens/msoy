//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.person.gwt.ConvMessage;
import com.threerings.msoy.person.gwt.MailServiceAsync;
import com.threerings.msoy.person.gwt.MailService;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;

import client.util.ClickCallback;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays a friend invitation.
 */
public class FriendInviteDisplay extends MailPayloadDisplay
{
    @Override // from MailPayloadDisplay
    public Widget widgetForRecipient ()
    {
        return new InvitationWidget();
    }

    protected class InvitationWidget extends SmartTable
    {
        protected InvitationWidget ()
        {
            super(0, 0);
            refreshUI(false);
        }

        protected void refreshUI (final boolean roundtrip)
        {
            int friendId = _message.author.name.getMemberId();
            _membersvc.getFriendStatus(CMail.ident, friendId, new MsoyCallback<Boolean>() {
                public void onSuccess (Boolean result) {
                    buildUI(result, roundtrip);
                }
            });
        }

        protected void buildUI (boolean friendStatus, boolean roundtrip)
        {
            if (friendStatus) {
                setText(0, 0, roundtrip ? CMail.msgs.friendAccepted(""+_message.author.name) :
                        CMail.msgs.friendAlreadyFriend(""+_message.author.name));
                setText(0, 1, "");

            } else {
                setText(0, 0, CMail.msgs.friendInvitation(), 0, "rowPanelCell");

                Button ayeButton = new Button(CMail.msgs.friendBtnAccept());
                new ClickCallback<Void>(ayeButton) {
                    public boolean callService () {
                        _membersvc.addFriend(CMail.ident, _message.author.name.getMemberId(), this);
                        return true;
                    }
                    public boolean gotResult (Void result) {
                        mailResponse();
                        refreshUI(true);
                        return false;
                    }
                };
                setWidget(0, 1, ayeButton);
            }
        }

        protected void mailResponse ()
        {
            _mailsvc.continueConversation(CMail.ident, _convoId, CMail.msgs.friendReplyBody(), null,
                new MsoyCallback<ConvMessage>() {
                    public void onSuccess (ConvMessage result) {
                        // Well that's nice.
                    }
                });
        }

        protected boolean _thirdPerson;
    }

    protected static final MemberServiceAsync _membersvc = (MemberServiceAsync)
        ServiceUtil.bind(GWT.create(MemberService.class), MemberService.ENTRY_POINT);
    protected static final MailServiceAsync _mailsvc = (MailServiceAsync)
        ServiceUtil.bind(GWT.create(MailService.class), MailService.ENTRY_POINT);
}
