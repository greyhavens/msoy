//
// $Id$

package client.shell;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.util.CookieUtil;
import com.threerings.gwt.util.Predicate;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.SessionData;

import client.util.MsoyUI;

/**
 * Displays a logon user interface.
 */
public class LogonPanel extends FlexTable
{
    public LogonPanel (boolean headerMode)
    {
        this(headerMode, new Button(CShell.cmsgs.menuLogon()));
    }

    public LogonPanel (boolean headerMode, Button logon)
    {
        setStyleName("logonPanel");
        setCellSpacing(2);

        // create the widgets we'll use in our layout
        _email = new TextBox();
        _email.setText(CookieUtil.get("who"));
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _password.setFocus(true);
            }
        }));
        _password = new PasswordTextBox();
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                doLogon();
            }
        }));
        String lbl = CShell.cmsgs.forgotPassword();
        Label forgot = MsoyUI.createActionLabel(lbl, "tipLabel", new ClickListener() {
            public void onClick (Widget widget) {
                Frame.showDialog("Forgot your password?", createForgotPassword());
            }
        });
        logon.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                doLogon();
            }
        });
        _status = new Label("");

        // now stick them in the right places
        if (headerMode) {
            getFlexCellFormatter().setStyleName(0, 0, "rightLabel");
            setText(0, 0, CShell.cmsgs.logonEmail());
            setWidget(0, 1, _email);
            setWidget(0, 2, forgot);
            getFlexCellFormatter().setStyleName(1, 0, "rightLabel");
            setText(1, 0, CShell.cmsgs.logonPassword());
            setWidget(1, 1, _password);
            setWidget(1, 2, logon);
            setWidget(2, 0, _status);
            getFlexCellFormatter().setColSpan(2, 0, getCellCount(1));

        } else {
            int row = 0;
            setText(row++, 0, CShell.cmsgs.logonEmail());
            setWidget(row++, 0, _email);
            setText(row++, 0, CShell.cmsgs.logonPassword());
            setWidget(row, 0, _password);
            setWidget(row++, 1, forgot);
            // in non-header mode logon is handled externally
            setWidget(row++, 0, _status);
        }
    }

    protected FlexTable createForgotPassword ()
    {
        FlexTable contents = new FlexTable();
        contents.setCellSpacing(10);

        // if they entered an email address, use that one, otherwise use the cookie
        String oemail = _email.getText().trim();
        if (oemail.length() == 0) {
            oemail = CookieUtil.get("who");
        }

        int col = 0;
        contents.getFlexCellFormatter().setStyleName(0, col, "rightLabel");
        contents.setText(0, col++, CShell.cmsgs.logonEmail());
        contents.setWidget(0, col++, _email = new TextBox());
        _email.setText(oemail);
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                doForgotPassword();
            }
        }));
        contents.setWidget(0, col++, new Button(CShell.cmsgs.submit(), new ClickListener() {
            public void onClick (Widget sender) {
                doForgotPassword();
            }
        }));

        contents.getFlexCellFormatter().setStyleName(0, col, "tipLabel");
        contents.setText(0, col++, CShell.cmsgs.forgotPasswordHelp());

        contents.setWidget(1, 0, _status = new Label(""));
        contents.getFlexCellFormatter().setColSpan(1, 0, contents.getCellCount(0));

        return contents;
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
                dismiss();
                _status.setText("");
                _password.setText("");
                CShell.app.didLogon((SessionData)result);
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
                dismiss();
                MsoyUI.info(CShell.cmsgs.forgotEmailSent());
            }
            public void onFailure (Throwable caught) {
                _status.setText(CShell.serverError(caught));
            }
        });
    }

    protected void dismiss ()
    {
        Frame.clearDialog(this);
    }

    protected TextBox _email;
    protected PasswordTextBox _password;
    protected Label _status;
}
