//
// $Id$

package client.mail;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import client.util.ClickCallback;
import client.util.MsoyCallback;

/**
 * Displays a friend invitation.
 */
public class FriendInviteDisplay extends MailPayloadDisplay
{
    // @Override // from MailPayloadDisplay
    public Widget widgetForRecipient ()
    {
        return new InvitationWidget(false);
    }

    // @Override // from MailPayloadDisplay
    public Widget widgetForOthers ()
    {
        return new InvitationWidget(true);
    }

    // @Override // from MailPayloadDisplay
    public String okToDelete ()
    {
        // we're always happy to be deleted
        return null;
    }

    protected class InvitationWidget extends FlowPanel
    {
        protected InvitationWidget (boolean thirdPerson)
        {
            _thirdPerson = thirdPerson;
            setStyleName("friendshipInvitation");
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
            clear();
            if (friendStatus) {
                add(new InlineLabel(roundtrip ? CMail.msgs.friendAccepted(""+_message.author) :
                                    CMail.msgs.friendAlreadyFriend(""+_message.author)));
                return;
            }
            if (_thirdPerson) {
                add(new InlineLabel(CMail.msgs.friendPending()));
                return;
            }

            add(new InlineLabel(CMail.msgs.friendInvitation()));

            Button ayeButton = new Button(CMail.msgs.friendBtnAccept());
            ayeButton.addStyleName("AyeButton");
            new ClickCallback(ayeButton) {
                public boolean callService () {
                    CMail.membersvc.addFriend(CMail.ident, _message.author.name.getMemberId(), this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    mailResponse();
                    refreshUI(true);
                    return false;
                }
            };
            add(ayeButton);
        }

        protected void mailResponse ()
        {
// TODO
//                 CMail.mailsvc.deliverMessage(
//                     CMail.ident, _message.author.name.getMemberId(),
//                     CMail.msgs.friendReplySubject(),
//                     CMail.msgs.friendReplyBody(CMail.creds.name.toString()),
//                     null,
//                     new MsoyCallback() {
//                         public void onSuccess (Object result) {
//                             // Well that's nice.
//                         }
//                     });
        }

        protected boolean _thirdPerson;
    }
}
