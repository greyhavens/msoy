//
// $Id$

package client.mail;

import client.util.HeaderValueTable;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.MemberGName;

public class MailComposition extends PopupPanel
{
    public MailComposition (WebContext ctx, int recipientId, String subject)
    {
        super(false);
        _ctx = ctx;
        _senderId = ctx.creds.memberId;
        _recipient = new MemberGName("Member #" + recipientId, recipientId);
        _ctx.membersvc.getName(recipientId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _recipient = (MemberGName) result;
                refreshRecipient();
            }
            public void onFailure (Throwable caught) {
                // let's ignore this error, everything will still work fine
            }
        });
        buildUI(subject);
    }

    public MailComposition (WebContext ctx, MemberGName recipient, String subject)
    {
        super(false);
        _ctx = ctx;
        _senderId = ctx.creds.memberId;
        _recipient = recipient;
        buildUI(subject);
    }

    protected void buildUI (String subject)
    {
        DockPanel panel = new DockPanel();
        setWidget(panel);

        HeaderValueTable headers = new HeaderValueTable();
        headers.addRow("To", _recipient.memberName);
        _subjectBox = new TextBox();
        _subjectBox.setText(subject);
        headers.addRow("Subject", _subjectBox);
        panel.add(headers, DockPanel.NORTH);

        HorizontalPanel buttonBox = new HorizontalPanel();
        Button replyButton = new Button("Send");
        replyButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                deliverMail();
            }
        });
        buttonBox.add(replyButton);
        Button discardButton = new Button("Discard");
        replyButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                // TODO: cheesy confirmation dialogue?
                hide();
            }
        });
        buttonBox.add(discardButton);
        panel.add(buttonBox);

        _messageBox = new TextArea();
        panel.add(_messageBox, DockPanel.CENTER);
        
        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");
        panel.add(_errorContainer, DockPanel.SOUTH);
    }
    
    protected void refreshRecipient ()
    {
            
    }


    
    protected void deliverMail ()
    {
        AsyncCallback callback = new AsyncCallback() {
            public void onSuccess (Object result) {
                hide();
            }
            public void onFailure (Throwable caught) {
                
            }
        };
        _ctx.mailsvc.deliverMessage(_ctx.creds, _recipient.memberId, _subjectBox.getText(),
                                    _messageBox.getText(), callback);
    }

    




    protected WebContext _ctx;
    protected int _senderId;
    protected MemberGName _recipient;
    protected TextBox _subjectBox;
    protected TextArea _messageBox;
    protected VerticalPanel _errorContainer;
}
