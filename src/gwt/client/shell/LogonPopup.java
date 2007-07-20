//
// $Id$

package client.shell;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.SessionData;

import client.util.BorderedPopup;
import client.util.MsoyUI;

/**
 * Displays a logon user interface.
 */
public class LogonPopup extends BorderedPopup
{
    public LogonPopup (StatusPanel parent)
    {
        super(true);
        _centerOnShow = false;
        _parent = parent;
        displayLogon();
    }

    // @Override // from PopupPanel
    public void show ()
    {
        super.show();
        if (_email.getText().length() == 0) {
            _email.setFocus(true);
        } else {
            _password.setFocus(true);
        }
    }

    protected void displayLogon ()
    {
        FlexTable contents = new FlexTable();
        contents.setStyleName("logonPopup");
        contents.setCellSpacing(10);
        setWidget(contents);

        int row = 0, cols = 3;
        contents.setText(row, 0, CShell.cmsgs.logonHelp());
        contents.getFlexCellFormatter().setColSpan(row++, 0, cols);

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.logonEmail());
        contents.setWidget(row++, 1, _email = new TextBox());
        _email.setText(CookieUtil.get("who"));
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _password.setFocus(true);
            }
        }));

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.logonPassword());
        contents.setWidget(row, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                doLogon();
            }
        }));
        contents.setWidget(row++, 2, new Button(CShell.cmsgs.menuLogon(), new ClickListener() {
            public void onClick (Widget sender) {
                doLogon();
            }
        }));

        String lbl = CShell.cmsgs.forgotPassword();
        contents.setWidget(row, 0, MsoyUI.createActionLabel(lbl, "Forgot", new ClickListener() {
            public void onClick (Widget widget) {
                displayForgotPassword();
            }
        }));
        contents.getFlexCellFormatter().setColSpan(row++, 0, cols);

        contents.setWidget(row, 0, _status = new Label(""));
        contents.getFlexCellFormatter().setColSpan(row++, 0, cols);
    }

    protected void displayForgotPassword ()
    {
        FlexTable contents = new FlexTable();
        contents.setStyleName("logonPopup");
        contents.setCellSpacing(10);
        setWidget(contents);

        int row = 0;
        contents.setText(row, 0, CShell.cmsgs.forgotPasswordHelp());
        contents.getFlexCellFormatter().setColSpan(row++, 0, 2);

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.logonEmail());
        contents.setWidget(row++, 1, _email = new TextBox());
        _email.setText(CookieUtil.get("who"));
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                doForgotPassword();
            }
        }));

        contents.setWidget(row, 0, _status = new Label(""));
        contents.getFlexCellFormatter().setColSpan(row++, 0, 2);
    }

    protected void doLogon ()
    {
        String account = _email.getText(), password = _password.getText();
        if (account.length() <= 0 || password.length() <= 0) {
            return;
        }

        _status.setText(CShell.cmsgs.loggingOn());
        CShell.usersvc.login(
            DeploymentConfig.version, account, CShell.md5hex(password), 1, new AsyncCallback() {
            public void onSuccess (Object result) {
                hide();
                _parent.didLogon((SessionData)result);
            }
            public void onFailure (Throwable caught) {
                CShell.log("Logon failed [account=" + _email.getText() + "]", caught);
                _status.setText(CShell.serverError(caught));
            }
        });
    }

    protected void doForgotPassword ()
    {
        String account = _email.getText();
        if (account.length() <= 0) {
            return;
        }

        _status.setText(CShell.cmsgs.sendingForgotEmail());
        CShell.usersvc.sendForgotPasswordEmail(account, new AsyncCallback() {
            public void onSuccess (Object result) {
                hide();
                MsoyUI.info(CShell.cmsgs.forgotEmailSent());
            }
            public void onFailure (Throwable caught) {
                _status.setText(CShell.serverError(caught));
            }
        });
    }

    protected StatusPanel _parent;
    protected TextBox _email;
    protected PasswordTextBox _password;
    protected Label _status;
}
