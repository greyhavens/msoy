//
// $Id$

package client.msgs;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberName;

import client.util.BorderedDialog;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * A mail composition popup.
 */
public class MailComposition extends BorderedDialog
{
    public static interface MailSentListener
    {
        /**
         * Notifies this object that the message being composed was successfully sent, and that it
         * may perform any side-effects that should be associated with the event.
         */
        public void messageSent (MemberName recipient);
    }

    /**
     * Initializes a new composer.
     */
    public MailComposition (MemberName recipient, String subject,
                            MailPayloadComposer payloadComposer, String bodyText)
    {
        super(false);
        _recipient = recipient;

        setHeaderTitle(CMsgs.mmsgs.popupHeader());

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("MailComposer");

        // set up the recipient/subject header grid
        Grid grid = new Grid(2, 2);
        CellFormatter formatter = grid.getCellFormatter();
        grid.setStyleName("Headers");

        grid.setText(0, 0, CMsgs.mmsgs.hdrTo());
        formatter.setStyleName(0, 0, "Label");
        grid.setWidget(0, 1, _recipientBox = new Label(_recipient.toString()));
        formatter.setStyleName(0, 1, "Value");

        KeyboardListener keyListener = new KeyboardListenerAdapter() {
            public void onKeyUp (Widget sender, char keyCode, int modifiers) {
                updateButtons();
            }
        };
        grid.setText(1, 0, CMsgs.mmsgs.hdrSubject());
        formatter.setStyleName(1, 0, "Label");
        _subjectBox = new TextBox();
        _subjectBox.addStyleName("SubjectBox");
        _subjectBox.setText(subject);
        _subjectBox.addKeyboardListener(keyListener);
        grid.setWidget(1, 1, _subjectBox);
        formatter.setStyleName(1, 1, "Value");
        panel.add(grid);

        _payloadBox = new SimplePanel();
        panel.add(_payloadBox);

        // then the textarea where we enter the body of the text
        // TODO: give us focus if this is a reply (otherwise the subject line)
        _messageBox = new TextArea();
        _messageBox.setCharacterWidth(60);
        _messageBox.setVisibleLines(10);
        _messageBox.addStyleName("Body");
        _messageBox.setText(bodyText);
        panel.add(_messageBox);
        setContents(panel);

        // with a send button
        _sendButton = new Button(CMsgs.mmsgs.btnSend(), new ClickListener() {
            public void onClick (Widget sender) {
                _sendButton.setEnabled(false);
                if (_payloadComposer != null) {
                    String msg = _payloadComposer.okToSend();
                    if (msg != null) {
                        MsoyUI.error(msg);
                        return;
                    }
                }
                deliverMail();
            }
        });
        addButton(_sendButton);

        // add a button for attaching items
        _attachButton = new Button(CMsgs.mmsgs.btnAttach(), new ClickListener() {
            public void onClick (Widget sender) {
                _attachButton.setEnabled(false);
                setPayloadComposer(new ItemGift.Composer(MailComposition.this));
            }
        });
        addButton(_attachButton);

        // and a discard button
        Button discardButton = new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                // TODO: confirmation dialog?
                hide();
            }
        });
        addButton(discardButton);

        // setting the payload composer will properly set the state of all the buttons
        setPayloadComposer(payloadComposer);
    }

    public void addMailSentListener (MailSentListener listener)
    {
        if (listener != null) {
            _listeners.add(listener);
        }
    }

    public void removeMailSentListener (MailSentListener listener)
    {
        if (listener != null) {
            _listeners.remove(listener);
        }
    }

    /**
     * Remove the payload from the message currently being composed. This can be safely called
     * from payload implementations if they wish to vanish.
     */
    public void removePayload ()
    {
        setPayloadComposer(null);
    }

    protected void setPayloadComposer (MailPayloadComposer composer)
    {
        // if there's no change, change nothing
        if (composer == _payloadComposer) {
            return;
        }
        // if there was a composer in place, remove it as a MailSentListener
        if (_payloadComposer != null) {
            removeMailSentListener(_payloadComposer);
        }
        _payloadComposer = composer;
        if (composer != null) {
            // the new composer is also a MailSentListener
            addMailSentListener(composer);

            Widget widget = _payloadComposer.widgetForComposition();
            if (widget != null) {
                widget.addStyleName("Payload");
                _payloadBox.setWidget(widget);
            }

        } else {
            _payloadBox.clear();
        }
        updateButtons();
    }

    // send the message off to the backend for delivery
    protected void deliverMail ()
    {
        CMsgs.mailsvc.deliverMessage(
            CMsgs.ident, _recipient.getMemberId(), _subjectBox.getText(), _messageBox.getText(),
            (_payloadComposer == null) ? null : _payloadComposer.getComposedPayload(),
            new MsoyCallback() {
            public void onSuccess (Object result) {
                Iterator i = _listeners.iterator();
                while (i.hasNext()) {
                    ((MailSentListener) i.next()).messageSent(_recipient);
                }
                hide();
                MsoyUI.info(CMsgs.mmsgs.messageSent());
            }
        });
    }

    protected void updateButtons ()
    {
        _sendButton.setEnabled(_subjectBox.getText().length() > 0);
        _attachButton.setEnabled(_payloadBox.getWidget() == null);
    }

    protected MemberName _recipient;
    protected MailPayloadComposer _payloadComposer;
    protected Set _listeners = new HashSet();

    protected Button _attachButton, _sendButton;

    protected TextBox _subjectBox;
    protected SimplePanel _payloadBox;
    protected Label _recipientBox;
    protected TextArea _messageBox;
}
