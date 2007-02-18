//
// $Id$

package client.msgs;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
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
        public Widget widgetForComposition ()
        {
            return new InvitationWidget();
        }

        // @Override
        public MailPayload getComposedPayload ()
        {
            return new FriendInviteObject();
        }

        // @Override
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
        protected class InvitationWidget extends HorizontalPanel
        {
            protected InvitationWidget ()
            {
                super();
                setSpacing(3);
                add(new InlineLabel("You can "));
                Button ayeButton = new Button("ACCEPT");
                ayeButton.setEnabled(false);
                add(ayeButton);
                add(new InlineLabel(" this invitation, or "));
                Button nayButton = new Button("DECLINE");
                nayButton.setEnabled(false);
                add(nayButton);
                add(new InlineLabel("it."));
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
            return new InvitationWidget(true);
        }

        // @Override
        public Widget widgetForOthers ()
        {
            return new InvitationWidget(false);
        }

        protected class InvitationWidget extends HorizontalPanel
        {
            protected InvitationWidget (boolean active)
            {
                super();
                setSpacing(3);
                add(new InlineLabel("You can "));
                Button ayeButton = new Button("ACCEPT");
                ayeButton.setEnabled(active);
                ayeButton.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        respondToInvite(true);
                    }
                });
                add(ayeButton);
                add(new InlineLabel(" this invitation, or "));
                Button nayButton = new Button("DECLINE");
                nayButton.setEnabled(active);
                nayButton.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        respondToInvite(false);
                    }
                });
                add(nayButton);
                add(new InlineLabel("it."));
            }

            protected void respondToInvite(final boolean accepted)
            {
                AsyncCallback callback = new AsyncCallback() {
                    public void onSuccess (Object result) {
                        mailResponse(accepted);
                    }
                    public void onFailure (Throwable caught) {
                        // TODO: error handling ...
                    }
                };
                int senderId = _message.headers.sender.getMemberId();
                if (accepted) {
                    CMsgs.membersvc.acceptFriend(CMsgs.creds, senderId, callback);
                } else {
                    CMsgs.membersvc.declineFriend(CMsgs.creds, senderId, callback);
                }
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
        }
    }
}
