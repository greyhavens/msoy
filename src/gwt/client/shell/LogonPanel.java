//
// $Id$

package client.shell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.web.gwt.BannedException;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Displays a logon user interface.
 */
public class LogonPanel extends SmartTable
{
    public enum Mode { LANDING, HORIZ, VERT };

    public static void addLogonBehavior (final TextBox email, final TextBox password,
                                         final ButtonBase action, final Command onLogon)
    {
        // make our logon button initiate a logon
        ClickHandler onAction = new ClickHandler() {
            public void onClick (ClickEvent event) {
                doLogon(email, password, onLogon);
            }
        };
        action.addClickHandler(onAction);

        String who = CookieUtil.get(CookieNames.WHO);
        if (who != null && !MemberMailUtil.isPermaguest(who)) {
            email.setText(who);
            // since our email is already filled in, we can focus the password field; note: we
            // don't focus the email field by default because we rely on the unfocused state
            // explaining what actually goes into the email field
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    password.setFocus(true);
                }
            });
        } else {
            Widgets.setPlaceholderText(email, _cmsgs.logonEmailDefault());
        }
        EnterClickAdapter.bind(email, new ClickHandler() {
            public void onClick (ClickEvent event) {
                password.setFocus(true);
            }
        });
        EnterClickAdapter.bind(password, onAction);
    }

    public static Label newForgotPassword (final String email)
    {
        String lbl = _cmsgs.forgotPassword();
        return MsoyUI.createActionLabel(lbl, "tipLabel", new ClickHandler() {
            public void onClick (ClickEvent event) {
                String forgottenTitle = "Forgot your password?";
                ForgotPasswordDialog forgottenDialog =
                    new ForgotPasswordDialog(email);
                CShell.frame.showDialog(forgottenTitle, forgottenDialog);
            }
        });
    }

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

        _email = new TextBox();
        _password = new PasswordTextBox();
        addLogonBehavior(_email, _password, logon, new Command() {
                public void execute () {
                    didLogon();
                }
            });

        Label forgot = newForgotPassword(_email.getText().trim());
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

    protected void didLogon ()
    {
        _password.setText("");
    }

    protected static void doLogon (
        final TextBox email, final TextBox passbox, final Command onLogon)
    {
        String account = email.getText(), password = passbox.getText();
        if (account.length() <= 0 || password.length() <= 0) {
            return;
        }
        if (account.equals(_cmsgs.logonEmailDefault())) {
            MsoyUI.errorNear(_cmsgs.logonEmailPlease(), email);
            return;
        }
        _usersvc.logon(DeploymentConfig.version, account, CShell.frame.md5hex(password),
                       WebUserService.SESSION_DAYS, new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData data) {
                CShell.frame.dispatchDidLogon(data);
                if (onLogon != null) {
                    onLogon.execute();
                }
            }
            public void onFailure (Throwable caught) {
                CShell.log("Logon failed [account=" + email.getText() + "]", caught);
                String message = null;
                if (caught instanceof BannedException) {
                    BannedException be = (BannedException)caught;
                    // add a little so user never sees "0 hours and 0 minutes"
                    int seconds = be.getExpires() + 59;
                    if (be.getExpires() >= 0) {
                        message = _cmsgs.tempBan(be.getWarning(), String.valueOf(seconds / 3600),
                            String.valueOf((seconds / 60) % 60));
                    } else {
                        message = _cmsgs.ban(be.getWarning());
                    }
                } else {
                    message = CShell.serverError(caught);
                }
                MsoyUI.errorNear(message, passbox);
            }
        });
    }

    public static class ForgotPasswordDialog extends SmartTable
    {
        public ForgotPasswordDialog (String oemail)
        {
            super(0, 10);

            if (oemail.length() == 0) {
                oemail = CookieUtil.get(CookieNames.WHO);
            }

            final TextBox email = new TextBox();
            int col = 0;
            getFlexCellFormatter().setStyleName(0, col, "rightLabel");
            getFlexCellFormatter().setVerticalAlignment(0, col, HasAlignment.ALIGN_MIDDLE);
            setText(0, col++, _cmsgs.logonEmail());
            setWidget(0, col++, email);
            email.setText(oemail);

            Button forgot = new Button(_cmsgs.send());
            setWidget(0, col++, forgot);
            new ClickCallback<Void>(forgot) {
                @Override protected boolean callService () {
                    String account = email.getText();
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
    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
}
