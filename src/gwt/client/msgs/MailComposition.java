//
// $Id$

package client.msgs;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;

import com.threerings.msoy.web.data.MemberName;

import client.util.BorderedDialog;

/**
 * A mail composition popup.
 */
public class MailComposition extends BorderedDialog
{
    /**
     * Initializes a new composer when we already have the full name.
     */
    public MailComposition (MemberName recipient, String subject,
                            MailPayloadComposer bodyObjectComposer, String bodyText)
    {
        super(false);
        _senderId = CMsgs.getMemberId();
        _recipient = recipient;
        _bodyObjectComposer = bodyObjectComposer;
        buildUI(subject, bodyText);
    }

    /**
     * Initializes a new composer with a recipient id; we do a backend request to look
     * up the recipient's current name, and inject the name into the right UI element.
     */
    public MailComposition (int recipientId, String subject, MailPayloadComposer factory,
                            String bodyText)
    {
        this(new MemberName(CMsgs.mmsgs.memberId(String.valueOf(recipientId)), recipientId),
             subject, factory, bodyText);
        CMsgs.membersvc.getName(recipientId, new AsyncCallback() {
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
    }

    // @Override
    protected Widget createContents ()
    {
        _panel = new VerticalPanel();
        _panel.addStyleName("MailComposer");
        return _panel;
    }

    // generate the composer UI, prepopulated wih the given subject line and body text
    protected void buildUI (String subject, String bodyText)
    {
        _header.add(createTitleLabel(CMsgs.mmsgs.popupHeader(), "ComposerTitle"));

        // set up the recipient/subject header grid
        Grid grid = new Grid(2, 2);
        CellFormatter formatter = grid.getCellFormatter();
        grid.setStyleName("Headers");

        grid.setText(0, 0, CMsgs.mmsgs.hdrTo());
        formatter.setStyleName(0, 0, "Label");
        grid.setWidget(0, 1, _recipientBox = new Label(_recipient.toString()));
        formatter.setStyleName(0, 1, "Value");

        grid.setText(1, 0, CMsgs.mmsgs.hdrSubject());
        formatter.setStyleName(1, 0, "Label");
        _subjectBox = new TextBox();
        _subjectBox.addStyleName("SubjectBox");
        _subjectBox.setText(subject);
        grid.setWidget(1, 1, _subjectBox);
        formatter.setStyleName(1, 1, "Value");
        _panel.add(grid);

        if (_bodyObjectComposer != null) {
            Widget widget = _bodyObjectComposer.widgetForComposition();
            if (widget != null) {
                widget.addStyleName("Payload");
                _panel.add(widget);
            }
        }

        // then the textarea where we enter the body of the text
        // TODO: give us focus if this is a reply (otherwise the subject line)
        _messageBox = new TextArea();
        _messageBox.setCharacterWidth(60);
        _messageBox.setVisibleLines(20);
        _messageBox.addStyleName("Body");
        _messageBox.setText(bodyText);
        _panel.add(_messageBox);

        // then a button box
        HorizontalPanel buttonBox = new HorizontalPanel();
        // this needs a distinctive style name since it's in BorderedDialog's CSS context
        buttonBox.addStyleName("MailControls");

        // with a send button
        Button replyButton = new Button(CMsgs.mmsgs.btnSend());
        replyButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                deliverMail();
            }
        });
        buttonBox.add(replyButton);

        // and a discard button
        Button discardButton = new Button(CMsgs.mmsgs.btnDiscard());
        discardButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                // TODO: confirmation dialog?
                hide();
            }
        });
        buttonBox.add(discardButton);
        _footer.add(buttonBox);
    }
    
    // send the message off to the backend for delivery
    protected void deliverMail ()
    {
        AsyncCallback callback = new AsyncCallback() {
            public void onSuccess (Object result) {
                if (_bodyObjectComposer != null) {
                    _bodyObjectComposer.messageSent(_recipient);
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
        CMsgs.mailsvc.deliverMessage(CMsgs.creds, _recipient.getMemberId(), _subjectBox.getText(),
                                     _messageBox.getText(), (_bodyObjectComposer == null) ?
                                     null : _bodyObjectComposer.getComposedPayload(), callback);
    }

    protected int _senderId;
    protected MemberName _recipient;
    protected VerticalPanel _panel;
    protected MailPayloadComposer _bodyObjectComposer;
    protected TextBox _subjectBox;
    protected Label _recipientBox;
    protected TextArea _messageBox;
}
