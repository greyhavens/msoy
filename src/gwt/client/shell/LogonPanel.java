//
// $Id$

package client.shell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.BannedException;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.ui.DefaultTextListener;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.ServiceUtil;

/**
 * Displays a logon user interface.
 */
public class LogonPanel extends SmartTable
{
    public enum Mode { LANDING, HORIZ, VERT };

    public LogonPanel (Mode mode)
    {
        this(mode, MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _cmsgs.logonLogon(), null));
    }

    public LogonPanel (Mode mode, ButtonBase logon)
    {
        super("logonPanel", 0, 0);

        switch(mode) {
        case LANDING: setCellSpacing(2); break;
        case VERT: setCellSpacing(10); break;
        case HORIZ: setCellPadding(5); break;
        }

        // make our logon button initiate a logon
        ClickListener onLogon = new ClickListener() {
            public void onClick (Widget sender) {
                doLogon();
            }
        };
        logon.addClickListener(onLogon);

        // create the email entry widget
        _email = new TextBox();
        if (CookieUtil.get("who") != null) {
            _email.setText(CookieUtil.get("who"));
            // since our email is already filled in, we can focus the password field; note: we
            // don't focus the email field by default because we rely on the unfocused state
            // explaining what actually goes into the email field
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    _password.setFocus(true);
                }
            });
        } else {
            DefaultTextListener.configure(_email, _cmsgs.logonEmailDefault());
        }
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _password.setFocus(true);
            }
        }));

        // create the password entry widget
        _password = new PasswordTextBox();
        _password.addKeyboardListener(new EnterClickAdapter(onLogon));

        // create the forgot password tip link
        String lbl = _cmsgs.forgotPassword();
        Label forgot = MsoyUI.createActionLabel(lbl, "tipLabel", new ClickListener() {
            public void onClick (Widget widget) {
                String forgottenTitle = "Forgot your password?";
                ForgotPasswordDialog forgottenDialog =
                    new ForgotPasswordDialog(_email.getText().trim());
                CShell.frame.showDialog(forgottenTitle, forgottenDialog);
            }
        });

        // now lay everything out
        switch (mode) {
        case HORIZ:
            setWidget(0, 0, _email);
            setWidget(0, 1, _password);
            setWidget(0, 2, logon);
            setWidget(0, 3, forgot);
            break;

        case LANDING:
        case VERT:
            setWidget(0, 0, _email);
            setWidget(0, 1, forgot);
            setWidget(1, 0, _password);
            setWidget(1, 1, logon);
            break;
        }
    }

    protected void doLogon ()
    {
        String account = _email.getText(), password = _password.getText();
        if (account.length() <= 0 || password.length() <= 0) {
            return;
        }
        if (account.equals(_cmsgs.logonEmailDefault())) {
            MsoyUI.errorNear(_cmsgs.logonEmailPlease(), _email);
            return;
        }
        _usersvc.logon(DeploymentConfig.version, account, CShell.frame.md5hex(password),
                       WebUserService.DEFAULT_SESSION_DAYS, new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData data) {
                CShell.frame.dispatchDidLogon(data);
                didLogon();
            }
            public void onFailure (Throwable caught) {
                CShell.log("Logon failed [account=" + _email.getText() + "]", caught);
                String message = null;
                if (caught instanceof BannedException) {
                    BannedException be = (BannedException)caught;
                    message = _cmsgs.tempBan(be.getWarning(), "" + be.getExpires());
                } else {
                    message = CShell.serverError(caught);
                }
                MsoyUI.errorNear(message, _password);
            }
        });
    }

    protected void didLogon ()
    {
        _password.setText("");
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
            getFlexCellFormatter().setVerticalAlignment(0, col, HasAlignment.ALIGN_MIDDLE);
            setText(0, col++, _cmsgs.logonEmail());
            setWidget(0, col++, _email = new TextBox());
            _email.setText(oemail);

            Button forgot = new Button(_cmsgs.send());
            setWidget(0, col++, forgot);
            new ClickCallback<Void>(forgot) {
                @Override protected boolean callService () {
                    String account = _email.getText();
                    if (account.length() <= 0) {
                        return false;
                    }
                    _usersvc.sendForgotPasswordEmail(account, this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    MsoyUI.info(_cmsgs.forgotEmailSent());
                    CShell.frame.clearDialog();
                    return false;
                }
            };

            getFlexCellFormatter().setStyleName(0, col, "tipLabel");
            setText(0, col++, _cmsgs.forgotPasswordHelp());
        }
    }

    protected TextBox _email;
    protected PasswordTextBox _password;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
