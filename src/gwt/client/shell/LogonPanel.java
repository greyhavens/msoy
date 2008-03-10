//
// $Id$

package client.shell;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.CookieUtil;
import com.threerings.gwt.util.Predicate;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.SessionData;

import client.util.ClickCallback;
import client.util.MsoyUI;

/**
 * Displays a logon user interface.
 */
public class LogonPanel extends SmartTable
{
    public LogonPanel (boolean headerMode)
    {
        this(headerMode, new Button(CShell.cmsgs.logonLogon()));
    }

    public LogonPanel (boolean headerMode, Button logon)
    {
        super("logonPanel", 0, 2);

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
                Frame.showDialog("Forgot your password?",
                                 new ForgotPasswordDialog(_email.getText().trim()));
            }
        });
        logon.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                doLogon();
            }
        });

        // now stick them in the right places
        if (headerMode) {
            setText(0, 0, CShell.cmsgs.logonEmail(), 1, "rightLabel");
            setWidget(0, 1, _email);
            setWidget(0, 3, forgot);
            setText(1, 0, CShell.cmsgs.logonPassword(), 1, "rightLabel");
            setWidget(1, 1, _password);
            setWidget(1, 3, logon);

        } else {
            int row = 0;
            setText(row++, 0, CShell.cmsgs.logonEmail());
            setWidget(row++, 0, _email);
            setText(row++, 0, CShell.cmsgs.logonPassword());
            setWidget(row, 0, _password);
            setWidget(row, 1, WidgetUtil.makeShim(3, 3));
            setWidget(row++, 2, forgot);
            // in non-header mode logon is handled externally
        }
    }

    protected void doLogon ()
    {
        String account = _email.getText(), password = _password.getText();
        if (account.length() <= 0 || password.length() <= 0) {
            return;
        }

        CShell.usersvc.login(
            DeploymentConfig.version, account, CShell.md5hex(password), 1, new AsyncCallback() {
            public void onSuccess (Object result) {
                _password.setText("");
                CShell.app.didLogon((SessionData)result);
            }
            public void onFailure (Throwable caught) {
                CShell.log("Logon failed [account=" + _email.getText() + "]", caught);
                MsoyUI.errorNear(CShell.serverError(caught), _password);
            }
        });
    }

    protected class ForgotPasswordDialog extends SmartTable
    {
        public ForgotPasswordDialog (String oemail)
        {
            super(0, 10);

            if (oemail.length() == 0) {
                oemail = CookieUtil.get("who");
            }

            int col = 0;
            getFlexCellFormatter().setStyleName(0, col, "rightLabel");
            setText(0, col++, CShell.cmsgs.logonEmail());
            setWidget(0, col++, _email = new TextBox());
            _email.setText(oemail);

            Button forgot = new Button(CShell.cmsgs.submit());
            setWidget(0, col++, forgot);
            new ClickCallback(forgot) {
                public boolean callService () {
                    String account = _email.getText();
                    if (account.length() <= 0) {
                        return false;
                    }
                    CShell.usersvc.sendForgotPasswordEmail(account, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    MsoyUI.info(CShell.cmsgs.forgotEmailSent());
                    Frame.clearDialog(ForgotPasswordDialog.this);
                    return false;
                }
            };

            getFlexCellFormatter().setStyleName(0, col, "tipLabel");
            setText(0, col++, CShell.cmsgs.forgotPasswordHelp());
        }
    }

    protected TextBox _email;
    protected PasswordTextBox _password;
}
