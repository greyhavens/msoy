//
// $Id$

package client.msgs;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MemberName;

import client.util.HeaderValueTable;

/**
 * A mail composition popup.
 */
public class MailComposition extends PopupPanel
{
    /**
     * Initializes a new composer when we already have the full name.
     */
    public MailComposition (MsgsContext ctx, MemberName recipient, String subject,
                            MailPayloadComposer bodyObjectComposer, String bodyText)
    {
        super(false);
        _ctx = ctx;
        _senderId = ctx.creds.memberId;
        _recipient = recipient;
        _bodyObjectComposer = bodyObjectComposer;
        buildUI(subject, bodyText);
    }

    /**
     * Initializes a new composer with a recipient id; we do a backend request to look
     * up the recipient's current name, and inject the name into the right UI element.
     */ 
    public MailComposition (MsgsContext ctx, int recipientId, String subject,
                            MailPayloadComposer factory, String bodyText)
    {
        this(ctx, new MemberName("Member #" + recipientId, recipientId),
             subject, factory, bodyText);
        _ctx.membersvc.getName(recipientId, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (result != null) {
                    _recipient = (MemberName) result;
                    _recipientBox.setText(_recipient.toString());
                }
            }
            public void onFailure (Throwable caught) {
                // let's ignore this error, it's just a display thing
            }
        });
        buildUI(subject, bodyText);
    }

    // generate the composer UI, prepopulated wih the given subject line and body text
    protected void buildUI (String subject, String bodyText)
    {
        setStyleName("mailComposition");
        VerticalPanel panel = new VerticalPanel();
        panel.setSpacing(5);

        // build the headers
        HeaderValueTable headers = new HeaderValueTable();
        headers.setStyleName("mailCompositionHeaders");
        _recipientBox = new Label(_recipient.toString());
        headers.addRow("To", _recipientBox);
        _subjectBox = new TextBox();
        _subjectBox.setText(subject);
        headers.addRow("Subject", _subjectBox);
        panel.add(headers);
        
        // then a button box
        HorizontalPanel buttonBox = new HorizontalPanel();
        buttonBox.setStyleName("mailCompositionButtons");

        // with a send button
        Button replyButton = new Button("Send");
        replyButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                deliverMail();
            }
        });
        buttonBox.add(replyButton);

        // and a discard button
        Button discardButton = new Button("Discard");
        discardButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                // TODO: cheesy confirmation dialogue?
                hide();
            }
        });
        buttonBox.add(discardButton);
        panel.add(buttonBox);

        if (_bodyObjectComposer != null) {
            Widget widget = _bodyObjectComposer.widgetForComposition(_ctx);
            if (widget != null) {
                panel.add(widget);
            }
        }

        // then the textarea where we enter the body of the text
        // TODO: give us focus if this is a reply (otherwise the subject line)
        // TODO: style this better, right now it looks a bit like a hungry void
        _messageBox = new TextArea();
        _messageBox.setCharacterWidth(60);
        _messageBox.setVisibleLines(20);
        _messageBox.setStyleName("mailCompositionBody");
        _messageBox.setText(bodyText);
        panel.add(_messageBox);

        // add the usual error container where we let the user know if something went wrong
        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");
        panel.add(_errorContainer);

        setWidget(panel);
    }
    
    // send the message off to the backend for delivery
    protected void deliverMail ()
    {
        AsyncCallback callback = new AsyncCallback() {
            public void onSuccess (Object result) {
                if (_bodyObjectComposer != null) {
                    _bodyObjectComposer.messageSent(_ctx, _recipient);
                }
                hide();
            }
            public void onFailure (Throwable caught) {
                // for now, just show that something went wrong and return to the composer
                PopupPanel popup = new PopupPanel(false);
                VerticalPanel panel = new VerticalPanel();
                panel.add(new Label("Ai! The message could not be sent."));
                Button okButton = new Button("OK");
                okButton.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        hide();
                    }
                });
                panel.add(okButton);
                popup.setWidget(panel);
                popup.show();
            }
        };
        _ctx.mailsvc.deliverMessage(_ctx.creds, _recipient.getMemberId(), _subjectBox.getText(),
                                    _messageBox.getText(), (_bodyObjectComposer == null) ?
                                    null : _bodyObjectComposer.getComposedPayload(), callback);
    }

    protected MsgsContext _ctx;
    protected int _senderId;
    protected MemberName _recipient;
    protected MailPayloadComposer _bodyObjectComposer;
    protected TextBox _subjectBox;
    protected Label _recipientBox;
    protected TextArea _messageBox;
    protected VerticalPanel _errorContainer;
}
