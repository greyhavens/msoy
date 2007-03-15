//
// $Id$

package client.msgs;

import client.util.ClickCallback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.web.data.FriendInviteObject;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MemberName;

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
            return new FriendInviteObject();
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
            CMsgs.membersvc.inviteFriend(CMsgs.creds, recipient.getMemberId(), new AsyncCallback() {
                public void onSuccess (Object result) {
                    // good -- nothing to do here
                }
                public void onFailure (Throwable caught) {
                    // this'll get slightly confusing, but not a huge deal
                }
            });
        }

        /**
         * A miniature version of the widget displayed by the mail reader.
         */
        protected class InvitationWidget extends FlowPanel
        {
            protected InvitationWidget ()
            {
                super();
                add(new InlineLabel("You are inviting the recipient of this message to be your friend."));
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
                setStyleName("InvitationWidget");

                _status = new Label();
                add(_status, DockPanel.SOUTH);
                _content = new FlowPanel();
                add(_content, DockPanel.CENTER);
                
                refreshUI();
            }

            protected void refreshUI ()
            {
                CMsgs.membersvc.getFriendStatus(
                    CMsgs.creds, _message.headers.sender.getMemberId(), new AsyncCallback() {
                       public void onSuccess (Object result) {
                           buildUI(((Boolean) result).booleanValue());
                       }
                       public void onFailure (Throwable caught) {
                           _status.setText(CMsgs.serverError(caught));
                       }
                    });
            }
            
            protected void buildUI (boolean friendStatus)
            {
                _content.clear();
                if (friendStatus) {
                    _content.add(new InlineLabel("This invitation has been accepted."));
                    return;
                }
                if (_thirdPerson) {
                    _content.add(new InlineLabel("This invitation is still pending."));
                    return;
                }

                _content.add(new InlineLabel("You can "));
                Button ayeButton = new Button("ACCEPT");
                new ClickCallback(ayeButton, _status) {
                    public boolean callService () {
                        CMsgs.membersvc.acceptFriend(
                            CMsgs.creds, _message.headers.sender.getMemberId(), this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
                        mailResponse(true);
                        refreshUI();
                        return false;
                    }
                };
                _content.add(ayeButton);
                _content.add(new InlineLabel(" this invitation, or "));
                Button nayButton = new Button("DECLINE");
                new ClickCallback(nayButton, _status) {
                    public boolean callService () {
                        CMsgs.membersvc.declineFriend(
                            CMsgs.creds, _message.headers.sender.getMemberId(), this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
                        mailResponse(false);
                        refreshUI();
                        return false;
                    }
                };
                _content.add(nayButton);
                _content.add(new InlineLabel(" it."));
            }

            protected void mailResponse (boolean accepted)
            {
                MemberName inviter = _message.headers.sender;
                MemberName invitee = _message.headers.recipient;
                String subject, body;
                if (accepted) {
                    subject = "Your friend invitation was accepted!";
                    body = "Your invitation to " + invitee + " was accepted! " +
                        "They are now a friend of yours.";
                } else {
                    subject = "Your friends invitation has been declined.";
                    body = "Your invitation to " + invitee + " was declined. Alas!";
                }
                CMsgs.mailsvc.deliverMessage(CMsgs.creds, inviter.getMemberId(), subject, body,
                                             null, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        // Well that's nice.
                    }
                    public void onFailure (Throwable caught) {
                        // I am not sure anything useful can be done here.
                    }
                });
            }

            protected boolean _thirdPerson;
            protected Label _status;
            protected FlowPanel _content;
        }
    }
}
