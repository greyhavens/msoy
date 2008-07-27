//
// $Id$

package client.shell;

import com.google.gwt.core.client.GWT;
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
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.BannedException;
import com.threerings.msoy.web.data.SessionData;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.ServiceUtil;

/**
 * Displays a logon user interface.
 */
public class LogonPanel extends SmartTable
        implements AsyncCallback<SessionData>
{
    public LogonPanel (boolean headerMode)
    {
        this(headerMode, new Button(_cmsgs.logonLogon()));
    }

    /**
     * Constructor that defaults showFloatingForgot to false
     * @param headerMode headerMode Changes the location of the form elements
     * @param logon logon Button to use for form submit
     */
    public LogonPanel (boolean headerMode, ButtonBase logon)
    {
        this(headerMode, logon, false);
    }

    /**
     * Constructor
     * @param headerMode Changes the location of the form elements
     * @param logon Button to use for form submit
     * @param showFloatingForgot If true, position it over the rest of the page, otherwise
     *        display the forgot password dialog inside the page frame
     */
    public LogonPanel (boolean headerMode, ButtonBase logon, final boolean showFloatingForgot)
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
        String lbl = _cmsgs.forgotPassword();
        Label forgot = MsoyUI.createActionLabel(lbl, "tipLabel", new ClickListener() {
            public void onClick (Widget widget) {
                String forgottenTitle = "Forgot your password?";
                ForgotPasswordDialog forgottenDialog =
                    new ForgotPasswordDialog(_email.getText().trim());
                if (showFloatingForgot) {
                    Frame.showPopupDialog(forgottenTitle, forgottenDialog);
                } else {
                    Frame.showDialog(forgottenTitle, forgottenDialog);
                }
            }
        });
        logon.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                doLogon();
            }
        });

        // now stick them in the right places
        if (headerMode) {
            setText(0, 0, _cmsgs.logonEmail(), 1, "rightLabel");
            setWidget(0, 1, _email);
            setWidget(0, 3, forgot);
            setText(1, 0, _cmsgs.logonPassword(), 1, "rightLabel");
            setWidget(1, 1, _password);
            setWidget(1, 3, logon);

        } else {
            int row = 0;
            setText(row++, 0, _cmsgs.logonEmail());
            setWidget(row++, 0, _email);
            setText(row++, 0, _cmsgs.logonPassword());
            setWidget(row, 0, _password);
            setWidget(row, 1, WidgetUtil.makeShim(3, 3));
            setWidget(row++, 2, forgot);
            // in non-header mode logon is handled externally
        }
    }

    @Override // from Widget
    protected void onAttach ()
    {
        super.onAttach();
        if (_email.getText().length() == 0) {
            _email.setFocus(true);
        } else {
            _password.setFocus(true);
        }
    }

    protected void doLogon ()
    {
        String account = _email.getText(), password = _password.getText();
        if (account.length() <= 0 || password.length() <= 0) {
            return;
        }

        _usersvc.login(DeploymentConfig.version, account, CShell.md5hex(password), 1, this);
    }

    public void onSuccess (SessionData data)
    {
        _password.setText("");
        CShell.app.didLogon(data);
    }

    public void onFailure (Throwable caught)
    {
        CShell.log("Logon failed [account=" + _email.getText() + "]", caught);
        String message = null;
        if (caught instanceof BannedException) {
            BannedException be = (BannedException)caught;
            message = _cmsgs.tempBan(be.getWarning(), "" + be.getExpires());
        } else {
            message = CShell.serverError(caught);
        }
        MsoyUI.errorNear(CShell.serverError(message), _password);
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
                public boolean callService () {
                    String account = _email.getText();
                    if (account.length() <= 0) {
                        return false;
                    }
                    _usersvc.sendForgotPasswordEmail(account, this);
                    return true;
                }
                public boolean gotResult (Void result) {
                    MsoyUI.info(_cmsgs.forgotEmailSent());
                    Frame.clearDialog();
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
