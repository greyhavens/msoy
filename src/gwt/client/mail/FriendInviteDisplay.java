//
// $Id$

package client.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.mail.gwt.ConvMessage;
import com.threerings.msoy.mail.gwt.MailService;
import com.threerings.msoy.mail.gwt.MailServiceAsync;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.util.ClickCallback;
import client.util.InfoCallback;

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
            int friendId = _message.author.name.getId();
            _membersvc.getFriendship(friendId, new InfoCallback<Friendship>() {
                public void onSuccess (Friendship result) {
                    buildUI(result, roundtrip);
                }
            });
        }

        protected void buildUI (Friendship friendship, boolean roundtrip)
        {
            if (friendship == Friendship.INVITEE) { // normal case
                setText(0, 0, _msgs.friendInvitation(), 0, "rowPanelCell");

                Button ayeButton = new Button(_msgs.friendBtnAccept());
                new ClickCallback<Void>(ayeButton) {
                    @Override protected boolean callService () {
                        _membersvc.addFriend(_message.author.name.getId(), this);
                        return true;
                    }
                    @Override protected boolean gotResult (Void result) {
                        mailResponse();
                        refreshUI(true);
                        return false;
                    }
                };
                setWidget(0, 1, ayeButton);
                return;
            }

            String otherName = _message.author.name.toString();
            String text;
            if (friendship == Friendship.FRIENDS) { // success case
                text = roundtrip ? _msgs.friendAccepted(otherName)
                                 : _msgs.friendAlreadyFriend(otherName);

            } else if (friendship == Friendship.INVITED) { // weird, but ok
                text = _msgs.friendInvited(otherName);

            } else { // retracted
                text = _msgs.friendRetracted(otherName);
            }
            setText(0, 0, text);
            setText(0, 1, "");
        }

        protected void mailResponse ()
        {
            _mailsvc.continueConversation(
                _convoId, _msgs.friendReplyBody(), null, new InfoCallback.NOOP<ConvMessage>());
        }

        protected boolean _thirdPerson;
    }

    protected static final MailMessages _msgs = GWT.create(MailMessages.class);
    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);
    protected static final MailServiceAsync _mailsvc = GWT.create(MailService.class);
}
