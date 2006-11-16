//
// $Id$

package client.person;

import client.mail.MailPayloadComposer;
import client.mail.MailPayloadDisplay;
import client.mail.MailUpdateListener;
import client.util.InlineLabel;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.FriendInviteObject;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MemberGName;

public abstract class FriendInvite
{
    public static class Composer
        implements MailPayloadComposer
    {
        public Widget widgetForComposition (WebContext ctx)
        {
            return new InvitationWidget();
        }
        // @Override
        public MailPayload getComposedPayload ()
        {
            return new FriendInviteObject();
        }
        
        // @Override
        public void messageSent ()
        {
            // do the db friend invite here
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
        public Display (WebContext ctx, MailMessage message)
        {
            super(ctx, message);
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
                int senderId = _message.headers.sender.memberId;
                if (accepted) {
                    _ctx.membersvc.acceptFriend(_ctx.creds, senderId, callback);
                } else {
                    _ctx.membersvc.declineFriend(_ctx.creds, senderId, callback);
                }
            }
        
            protected void mailResponse (boolean accepted)
            {
                MemberGName inviter = _message.headers.sender;
                MemberGName invitee = _message.headers.recipient;
                String subject, body;
                if (accepted) {
                    subject = "Your friend invitation was accepted!";
                    body = "Your invitation to " + invitee.memberName + 
                           "was accepted! They are now a friend of yours.";
                } else {
                    subject = "Your friends invitation has been declined.";
                    body = "Your invitation to " + invitee.memberName +
                           "was declined. Alas!";
                }
                _ctx.mailsvc.deliverMessage(
                    _ctx.creds, inviter.memberId, subject, body, null, new AsyncCallback() {
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
