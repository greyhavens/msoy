//
// $Id$

package client.msgs;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.data.FriendInvitePayload;
import com.threerings.msoy.person.data.MailMessage;
import com.threerings.msoy.person.data.MailPayload;

import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.MsoyCallback;

public abstract class FriendInvite
{
    public static class Composer
        implements MailPayloadComposer
    {
        // from MailPayloadComposer
        public Widget widgetForComposition ()
        {
            return new InvitationWidget();
        }

        // from MailPayloadComposer
        public MailPayload getComposedPayload ()
        {
            return new FriendInvitePayload();
        }

        // from MailPayloadComposer
        public String okToSend ()
        {
            // we're always ready to be sent
            return null;
        }

        // from MailPayloadComposer
        public void messageSent (MemberName recipient)
        {
            // the successful dispatch of an invite finishes one of the tutorial steps
            FlashClients.tutorialEvent("friendInvited");
        }

        /**
         * A miniature version of the widget displayed by the mail reader.
         */
        protected class InvitationWidget extends FlowPanel
        {
            protected InvitationWidget ()
            {
                super();
                add(new InlineLabel(CMsgs.mmsgs.friendInviting()));
            }
        }
    }

    public static class Display extends MailPayloadDisplay
    {
        public Display (MailMessage message)
        {
            super(message);
        }

        // @Override
        public Widget widgetForRecipient (MailUpdateListener listener)
        {
            return new InvitationWidget(false);
        }

        // @Override
        public Widget widgetForOthers ()
        {
            return new InvitationWidget(true);
        }

        // @Override
        public String okToDelete ()
        {
            // we're always happy to be deleted
            return null;
        }

        protected class InvitationWidget extends DockPanel
        {
            protected InvitationWidget (boolean thirdPerson)
            {
                super();
                _thirdPerson = thirdPerson;
                setStyleName("friendshipInvitation");

                _status = new Label();
                add(_status, DockPanel.SOUTH);
                _content = new VerticalPanel();
                add(_content, DockPanel.CENTER);

                refreshUI(false);
            }

            protected void refreshUI (final boolean roundtrip)
            {
                CMsgs.membersvc.getFriendStatus(
                    CMsgs.ident, _message.headers.sender.getMemberId(), new AsyncCallback() {
                       public void onSuccess (Object result) {
                           buildUI(((Boolean) result).booleanValue(), roundtrip);
                       }
                       public void onFailure (Throwable caught) {
                           _status.setText(CMsgs.serverError(caught));
                       }
                    });
            }

            protected void buildUI (boolean friendStatus, boolean roundtrip)
            {
                _content.clear();
                if (friendStatus) {
                    _content.add(new InlineLabel(roundtrip ?
                        CMsgs.mmsgs.friendAccepted(_message.headers.sender.toString()) :
                        CMsgs.mmsgs.friendAlreadyFriend(_message.headers.sender.toString())));
                    return;
                }
                if (_thirdPerson) {
                    _content.add(new InlineLabel(CMsgs.mmsgs.friendPending()));
                    return;
                }

                _content.add(new InlineLabel(CMsgs.mmsgs.friendInvitation()));

                Button ayeButton = new Button(CMsgs.mmsgs.friendBtnAccept());
                ayeButton.addStyleName("AyeButton");
                new ClickCallback(ayeButton) {
                    public boolean callService () {
                        CMsgs.membersvc.addFriend(
                            CMsgs.ident, _message.headers.sender.getMemberId(), this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
                        mailResponse();
                        refreshUI(true);
                        return false;
                    }
                };
                _content.add(ayeButton);
            }

            protected void mailResponse ()
            {
                MemberName inviter = _message.headers.sender;
                MemberName invitee = _message.headers.recipient;

                CMsgs.mailsvc.deliverMessage(
                    CMsgs.ident, inviter.getMemberId(),
                    CMsgs.mmsgs.friendReplySubject(),
                    CMsgs.mmsgs.friendReplyBody(invitee.toString()),
                    null,
                    new MsoyCallback() {
                        public void onSuccess (Object result) {
                            // Well that's nice.
                        }
                    });
            }

            protected boolean _thirdPerson;
            protected Label _status;
            protected VerticalPanel _content;
        }
    }
}
