//
// $Id: $

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.EnterClickAdapter;

import com.threerings.gwt.ui.Popups;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MediaMimeTypes;

import client.ui.MsoyUI;
import client.util.InfoCallback;
import client.util.TextBoxUtil;

/**
 * Handles resetting of a user's password.
 */
public class BlacklistWidget extends FlexTable
{
    public BlacklistWidget (ClickHandler done)
    {
        _done = done;
        setCellSpacing(10);
        setStyleName("formPanel");

        int row = 0;
        getFlexCellFormatter().setColSpan(row, 0, 2);
        getFlexCellFormatter().setStyleName(row, 0, "Intro");

        // all systems go!
        setText(row++, 0, _msgs.blacklistIntro());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, _msgs.blacklistName());
        setWidget(row, 1, _name = new TextBox());
        _name.setVisibleLength(45);
        _name.setMaxLength(45);
        _name.addKeyPressHandler(new EnterClickAdapter(new ClickHandler() {
            public void onClick (ClickEvent event) {
                _note.setFocus(true);
            }
        }));
        TextBoxUtil.addTypingListener(_name, _validator);

        ClickHandler submit = new ClickHandler() {
            public void onClick (ClickEvent event) {
                sendResetRequest();
            }
        };
        row += 1;
        
        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, _msgs.blacklistNote());
        setWidget(row++, 1, _note = new TextBox());
        _note.setVisibleLength(45);
        _note.setMaxLength(45);
        TextBoxUtil.addTypingListener(_note, _validator);

        getFlexCellFormatter().setColSpan(row, 0, 2);
        getFlexCellFormatter().setStyleName(row, 0, "Status");
        setWidget(row++, 0, _status = new Label(""));
        _status.setText(_msgs.blacklistMissingName());

        getFlexCellFormatter().setColSpan(row, 0, 2);
        getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        setWidget(row++, 0, _submit = new Button(_msgs.blacklistSubmit(), submit));
    }

    protected void validateData ()
    {
        boolean valid = false;
        String name = _name.getText().trim(), note = _note.getText().trim();
        int ix = name.indexOf('.');

        if (name.length() == 0) {
            _status.setText(_msgs.blacklistMissingName());

        } else if (ix != 40) {
            _status.setText(_msgs.blacklistShortName());

        } else if (MediaMimeTypes.INVALID_MIME_TYPE == MediaMimeTypes.suffixToMimeType(name)) {
            _status.setText(_msgs.blacklistUnknownType());

        } else if (note.length() == 0) {
            _status.setText(_msgs.blacklistMissingNote());

        } else {
            _status.setText(_msgs.blacklistReady());
            valid = true;
        }
        _submit.setEnabled(valid);
    }

    protected void sendResetRequest ()
    {
        String name = _name.getText().trim(), note = _note.getText().trim();
        int ix = name.indexOf('.');
        if (ix != 40) {
            MsoyUI.error(_msgs.blacklistShortName());
            return;
        }
        byte[] hash = MediaDesc.stringToHash(name.substring(0, ix));
        byte type = MediaMimeTypes.suffixToMimeType(name);
        if (type == MediaMimeTypes.INVALID_MIME_TYPE) {
            MsoyUI.error(_msgs.blacklistUnknownType());
            return;
        }
        _adminsvc.nukeMedia(new MediaDesc(hash, type), note, new InfoCallback<Void>() {
            public void onSuccess (Void result) {
                if (_done != null) {
                    MsoyUI.infoAction(_msgs.blacklistDone(), "OK", _done);

                } else {
                    MsoyUI.info(_msgs.blacklistDone());
                }
            }
        });
    }

    protected Command _validator = new Command() {
        public void execute () {
            validateData();
        }
    };

    protected ClickHandler _done;
    protected TextBox _name, _note;
    protected Button _submit;
    protected Label _status;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
}
