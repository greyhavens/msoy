//
// $Id$

package client.mail;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import client.util.ClickCallback;
import client.util.MsoyCallback;

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
            CMail.membersvc.getFriendStatus(CMail.ident, friendId, new MsoyCallback() {
                public void onSuccess (Object result) {
                    buildUI(((Boolean) result).booleanValue(), roundtrip);
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
                new ClickCallback(ayeButton) {
                    public boolean callService () {
                        CMail.membersvc.addFriend(
                            CMail.ident, _message.author.name.getMemberId(), this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
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
            CMail.mailsvc.continueConversation(
                CMail.ident, _convoId, CMail.msgs.friendReplyBody(), null, new MsoyCallback() {
                public void onSuccess (Object result) {
                    // Well that's nice.
                }
            });
        }

        protected boolean _thirdPerson;
    }
}
