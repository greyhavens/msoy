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

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.SessionData;

import client.util.MsoyUI;
import client.util.Predicate;

/**
 * Displays a logon user interface.
 */
public class LogonPanel extends SimplePanel
{
    public static void toggleShowLogon (StatusPanel parent)
    {
        int cleared = Frame.clearDialog(new Predicate() {
            public boolean isMatch (Object o) {
                return (o instanceof LogonPanel);
            }
        });
        if (cleared == 0) {
            Frame.showDialog(new LogonPanel(parent));
        }
    }

    // @Override // from Wiget
    public void onAttach ()
    {
        super.onAttach();
        if (_email.getText().length() == 0) {
            _email.setFocus(true);
        } else {
            _password.setFocus(true);
        }
    }

    protected LogonPanel (StatusPanel parent)
    {
        setStyleName("logonPanel");
        _parent = parent;
        displayLogon();
    }

    protected void displayLogon ()
    {
        FlexTable contents = new FlexTable();
        contents.setCellSpacing(10);
        setWidget(contents);

        int col = 0;
        contents.getFlexCellFormatter().setStyleName(0, col, "rightLabel");
        contents.setText(0, col++, CShell.cmsgs.logonEmail());
        contents.setWidget(0, col++, _email = new TextBox());
        _email.setText(CookieUtil.get("who"));
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _password.setFocus(true);
            }
        }));

        contents.getFlexCellFormatter().setStyleName(0, col, "rightLabel");
        contents.setText(0, col++, CShell.cmsgs.logonPassword());
        contents.setWidget(0, col++, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                doLogon();
            }
        }));
        contents.setWidget(0, col++, new Button(CShell.cmsgs.menuLogon(), new ClickListener() {
            public void onClick (Widget sender) {
                doLogon();
            }
        }));

        String lbl = CShell.cmsgs.forgotPassword();
        contents.setWidget(0, col++, MsoyUI.createActionLabel(lbl, "tipLabel", new ClickListener() {
            public void onClick (Widget widget) {
                displayForgotPassword();
            }
        }));

        contents.setWidget(1, 0, _status = new Label(""));
        contents.getFlexCellFormatter().setColSpan(1, 0, contents.getCellCount(0));
    }

    protected void displayForgotPassword ()
    {
        FlexTable contents = new FlexTable();
        contents.setCellSpacing(10);
        setWidget(contents);

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

    protected StatusPanel _parent;
    protected TextBox _email;
    protected PasswordTextBox _password;
    protected Label _status;
}
