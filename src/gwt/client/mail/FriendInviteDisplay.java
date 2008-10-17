//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.mail.gwt.ConvMessage;
import com.threerings.msoy.mail.gwt.MailService;
import com.threerings.msoy.mail.gwt.MailServiceAsync;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

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
            _membersvc.getFriendStatus(friendId, new MsoyCallback<Boolean>() {
                public void onSuccess (Boolean result) {
                    buildUI(result, roundtrip);
                }
            });
        }

        protected void buildUI (boolean friendStatus, boolean roundtrip)
        {
            if (friendStatus) {
                setText(0, 0, roundtrip ? _msgs.friendAccepted(""+_message.author.name) :
                        _msgs.friendAlreadyFriend(""+_message.author.name));
                setText(0, 1, "");

            } else {
                setText(0, 0, _msgs.friendInvitation(), 0, "rowPanelCell");

                Button ayeButton = new Button(_msgs.friendBtnAccept());
                new ClickCallback<Void>(ayeButton) {
                    public boolean callService () {
                        _membersvc.addFriend(_message.author.name.getMemberId(), this);
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
            _mailsvc.continueConversation(
                _convoId, _msgs.friendReplyBody(), null, new MsoyCallback.NOOP<ConvMessage>());
        }

        protected boolean _thirdPerson;
    }

    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
    protected static final MailServiceAsync _mailsvc = (MailServiceAsync)
        ServiceUtil.bind(GWT.create(MailService.class), MailService.ENTRY_POINT);
}
