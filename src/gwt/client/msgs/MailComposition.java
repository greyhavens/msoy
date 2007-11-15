//
// $Id$

package client.msgs;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;

import com.threerings.msoy.data.all.MemberName;

import client.util.BorderedDialog;
import client.util.MsoyUI;

/**
 * A mail composition popup.
 */
public class MailComposition extends BorderedDialog
{
    public static interface MailSentListener
    {
        /**
         * Notifies this object that the message being composed was successfully sent, and that it may
         * perform any side-effects that should be associated with the event.
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
        setPayloadComposer(payloadComposer);
        buildUI(subject, bodyText, payloadComposer);
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

    // @Override
    protected Widget createContents ()
    {
        _panel = new VerticalPanel();
        _panel.addStyleName("MailComposer");
        return _panel;
    }

    // generate the composer UI, prepopulated wih the given subject line and body text
    protected void buildUI (String subject, String bodyText, MailPayloadComposer composer)
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
        _panel.add(grid);

        _payloadBox = new SimplePanel();
        _panel.add(_payloadBox);

        // then the textarea where we enter the body of the text
        // TODO: give us focus if this is a reply (otherwise the subject line)
        _messageBox = new TextArea();
        _messageBox.setCharacterWidth(60);
        _messageBox.setVisibleLines(10);
        _messageBox.addStyleName("Body");
        _messageBox.setText(bodyText);
        _messageBox.addKeyboardListener(keyListener);
        _panel.add(_messageBox);

        // then a button box
        HorizontalPanel buttonBox = new HorizontalPanel();
        // this needs a distinctive style name since it's in BorderedDialog's CSS context
        buttonBox.addStyleName("MailControls");

        // with a send button
        _sendButton = new Button(CMsgs.mmsgs.btnSend());
        _sendButton.addClickListener(new ClickListener() {
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
        buttonBox.add(_sendButton);

        // add a button for attaching items
        _attachButton = new Button(CMsgs.mmsgs.btnAttach());
        _attachButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                _attachButton.setEnabled(false);
                setPayloadComposer(new ItemGift.Composer(MailComposition.this));
            }
        });
        buttonBox.add(_attachButton);

        // and a discard button
        Button discardButton = new Button(CMsgs.mmsgs.btnDiscard());
        discardButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                // TODO: confirmation dialog?
                hide();
            }
        });
        buttonBox.add(discardButton);

        // setting the payload composer will properly set the state of all the buttons
        setPayloadComposer(composer);

        _footer.add(buttonBox);
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
        AsyncCallback callback = new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator i = _listeners.iterator();
                while (i.hasNext()) {
                    ((MailSentListener) i.next()).messageSent(_recipient);
                }
                hide();
                MsoyUI.info(CMsgs.mmsgs.messageSent());
            }
            public void onFailure (Throwable caught) {
                // for now, just show that something went wrong and return to the composer
                MsoyUI.error("Ai! The message could not be sent.");
            }
        };
        CMsgs.mailsvc.deliverMessage(CMsgs.ident, _recipient.getMemberId(), _subjectBox.getText(),
                                     _messageBox.getText(), (_payloadComposer == null) ?
                                     null : _payloadComposer.getComposedPayload(), callback);
    }

    protected void updateButtons ()
    {
        _sendButton.setEnabled(
            _subjectBox.getText().length() > 0 && _messageBox.getText().length() > 0);
        _attachButton.setEnabled(_payloadBox.getWidget() == null);
    }

    protected MemberName _recipient;
    protected VerticalPanel _panel;
    protected MailPayloadComposer _payloadComposer;
    protected Set _listeners = new HashSet();

    protected Button _attachButton, _sendButton;

    protected TextBox _subjectBox;
    protected SimplePanel _payloadBox;
    protected Label _recipientBox;
    protected TextArea _messageBox;
}
