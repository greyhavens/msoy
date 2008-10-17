//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;

import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Handles resetting of a user's password.
 */
public class ResetPasswordPanel extends FlexTable
{
    public ResetPasswordPanel (Args args)
    {
        setCellSpacing(10);
        setStyleName("formPanel");

        int row = 0;
        getFlexCellFormatter().setColSpan(row, 0, 2);
        getFlexCellFormatter().setStyleName(row, 0, "Intro");

        // make sure we're not currently logged in
        if (!CShell.isGuest()) {
            setText(row, 0, _msgs.resetLogout());
            return;
        }

        // make sure we got something in the way of arguments
        _memberId = args.get(1, 0);
        _code = args.get(2, "");
        if (_memberId == 0 || _code.equals("")) {
            setText(row, 0, _msgs.resetInvalid());
            return;
        }

        // all systems go!
        setText(row++, 0, _msgs.resetIntro());

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, _msgs.resetPassword());
        setWidget(row++, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_validator);

        ClickListener submit = new ClickListener() {
            public void onClick (Widget sender) {
                sendResetRequest();
            }
        };

        getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        setText(row, 0, _msgs.resetConfirm());
        setWidget(row++, 1, _confirm = new PasswordTextBox());
        _confirm.addKeyboardListener(new EnterClickAdapter(submit));
        _confirm.addKeyboardListener(_validator);

        getFlexCellFormatter().setColSpan(row, 0, 2);
        getFlexCellFormatter().setStyleName(row, 0, "Status");
        setWidget(row++, 0, _status = new Label(""));
        _status.setText(_msgs.resetMissingPassword());

        getFlexCellFormatter().setColSpan(row, 0, 2);
        getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        setWidget(row++, 0, _submit = new Button(_msgs.resetSubmit(), submit));
    }

    protected void validateData ()
    {
        boolean valid = false;
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        if (password.length() == 0) {
            _status.setText(_msgs.resetMissingPassword());
        } else if (confirm.length() == 0) {
            _status.setText(_msgs.resetMissingConfirm());
        } else if (!password.equals(confirm)) {
            _status.setText(_msgs.resetPasswordMismatch());
        } else {
            _status.setText(_msgs.resetReady());
            valid = true;
        }
        _submit.setEnabled(valid);
    }

    protected void sendResetRequest ()
    {
        String password = CShell.frame.md5hex(_password.getText().trim());
        _usersvc.resetPassword(_memberId, _code, password, new MsoyCallback<Boolean>() {
            public void onSuccess (Boolean result) {
                if (result) {
                    MsoyUI.info(_msgs.resetReset());
                } else {
                    MsoyUI.error(_msgs.resetInvalid());
                }
            }
        });
    }

    protected KeyboardListener _validator = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    validateData();
                }
            });
        }
    };

    protected int _memberId;
    protected String _code;
    protected PasswordTextBox _password, _confirm;
    protected Button _submit;
    protected Label _status;

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
