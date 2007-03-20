//
// $Id$

package client.shell;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.util.BorderedPopup;
import client.util.FlashClients;
import client.util.MsoyUI;

/**
 * Displays basic player status (name, flow count) and handles logging on and logging off.
 */
public class StatusPanel extends FlexTable
{
    public StatusPanel (Application app)
    {
        setStyleName("statusPanel");
        setCellPadding(0);
        setCellSpacing(0);
        _app = app;
    }

    /**
     * Called once the rest of our application is set up. Checks to see if we're already logged on,
     * in which case it triggers a call to didLogon().
     */
    public void init ()
    {
        validateSession(CookieUtil.get("creds"));
    }

    /**
     * Returns our credentials or null if we are not logged in.
     */
    public WebCreds getCredentials ()
    {
        return _creds;
    }

    /**
     * Requests that we display our logon popup.
     */
    public void showLogonPopup ()
    {
        showLogonPopup(-1, -1);
    }

    /**
     * Requests that we display our logon popup at the specified position.
     */
    public void showLogonPopup (int px, int py)
    {
        LogonPopup popup = new LogonPopup();
        popup.show();
        popup.setPopupPosition(px == -1 ? (Window.getClientWidth() - popup.getOffsetWidth()) : px,
                               py == -1 ? HEADER_HEIGHT : py);
    }

    /**
     * Rereads our flow, gold, etc. levels and updates our header display.
     */
    public void refreshLevels ()
    {
        if (_creds != null) {
            int[] levels = FlashClients.getLevels();
            setText(0, _flowIdx, String.valueOf(levels[0]));
            setText(0, _goldIdx, String.valueOf(levels[1]));
            setText(0, _levelIdx, String.valueOf(levels[2]));
        } else {
            CShell.log("Ignoring refreshLevels() request as we're not logged on.");
        }
    }

    /**
     * Rereads our mail notification status and updates our header display.
     */
    public void refreshMailNotification ()
    {
        if (_creds != null) {
            _mailNotifier.setVisible(FlashClients.getMailNotification());
        } else {
            CShell.log("Ignoring refreshMailNotification() request as we're not logged on.");
        }
    }

    /**
     * Clears out our credentials and displays the logon interface.
     */
    public void logoff ()
    {
        _creds = null;
        clearCookie("creds");
        _app.didLogoff();

        // give the header client a chance to logoff before we nix it
        logoffHeaderClient();

        reset();
        if (DeploymentConfig.devDeployment) {
            setText(0, 0, "New to Whirled?");
            setText(0, 1, "");
            getFlexCellFormatter().setWidth(0, 1, "5px");
            setWidget(0, 2, MsoyUI.createActionLabel("Create an account!", new ClickListener() {
                public void onClick (Widget sender) {
                    new CreateAccountDialog(StatusPanel.this).show();
                }
            }));
        } else {
            setText(0, 0, "Welcome to the First Whirled!");
        }
    }

    protected void validateSession (String token)
    {
        if (token != null) {
            // validate our session before considering ourselves logged on
            CShell.usersvc.validateSession(token, 1, new AsyncCallback() {
                public void onSuccess (Object result) {
                    if (result == null) {
                        logoff();
                    } else {
                        _creds = (WebCreds)result;
                        didLogon(_creds);
                    }
                }
                public void onFailure (Throwable t) {
                    logoff();
                }
            });

        } else {
            logoff();
        }
    }

    protected void didLogon (WebCreds creds)
    {
        _creds = creds;
        setCookie("creds", _creds.token);
        setCookie("who", _creds.accountName);
        _app.didLogon(_creds);

        reset();
        int idx = 0;

        setText(0, idx++, _creds.name + ":");

        getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
        getFlexCellFormatter().setStyleName(0, idx, "Icon");
        setWidget(0, idx++, new Image("/images/header/symbol_flow.png"));
        setText(0, _flowIdx = idx++, "0");

        getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
        getFlexCellFormatter().setStyleName(0, idx, "Icon");
        setWidget(0, idx++, new Image("/images/header/symbol_gold.png"));
        setText(0, _goldIdx = idx++, "0");

        getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
        getFlexCellFormatter().setStyleName(0, idx, "Icon");
        setWidget(0, idx++, new Image("/images/header/symbol_level.png"));
        setText(0, _levelIdx = idx++, "0");

        getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
        getFlexCellFormatter().setStyleName(0, idx, "Icon");
        _mailNotifier = new HTML(
            "<a href='/mail'>" +
            "<img class='MailNotification' src='/images/mail/button_mail.png'/></a>");
        // begin with 'new mail' turned off until we hear otherwise
        _mailNotifier.setVisible(false);
        getFlexCellFormatter().setWidth(0, idx, "20px");
        setWidget(0, idx++, _mailNotifier);
    }

    protected void reset ()
    {
        if (getRowCount() > 0) {
            for (int col = 0; col < getCellCount(0); col++) {
                clearCell(0, col);
                getFlexCellFormatter().setWidth(0, col, "0px");
            }
        }
    }

    protected void actionClicked ()
    {
        if (_creds == null) {
            showLogonPopup();
        } else {
            logoff();
        }
    }

    protected void setCookie (String name, String value)
    {
        CookieUtil.set("/", 7, name, value);
    }

    protected void clearCookie (String name)
    {
        CookieUtil.clear("/", name);
    }

    protected native String md5hex (String text) /*-{
       return $wnd.hex_md5(text);
    }-*/;

    protected class LogonPopup extends BorderedPopup
        implements ClickListener
    {
        public LogonPopup ()
        {
            super(true);
            _centerOnShow = false;

            FlexTable contents = new FlexTable();
            contents.setStyleName("logonPopup");
            setWidget(contents);
            contents.getFlexCellFormatter().setStyleName(0, 0, "rightLabel");
            contents.setText(0, 0, CShell.cmsgs.logonEmail());
            contents.setWidget(0, 1, _email = new TextBox());
            _email.setText(CookieUtil.get("who"));
            _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
                public void onClick (Widget sender) {
                    _password.setFocus(true);
                }
            }));

            contents.getFlexCellFormatter().setStyleName(1, 0, "rightLabel");
            contents.setText(1, 0, CShell.cmsgs.logonPassword());
            contents.setWidget(1, 1, _password = new PasswordTextBox());
            _password.addKeyboardListener(new EnterClickAdapter(this));

            contents.setWidget(2, 0, _status = new Label(""));
            contents.getFlexCellFormatter().setColSpan(2, 0, 2);
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

        // from interface ClickListener
        public void onClick (Widget sender)
        {
            String account = _email.getText(), password = _password.getText();
            if (account.length() > 0 && password.length() > 0) {
                _status.setText(CShell.cmsgs.loggingOn());
                CShell.usersvc.login(account, md5hex(password), 1, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        hide();
                        didLogon((WebCreds)result);
                    }
                    public void onFailure (Throwable caught) {
                        _status.setText(CShell.serverError(caught));
                    }
                });
            }
        }

        protected TextBox _email;
        protected PasswordTextBox _password;
        protected Label _status;
    }

    /**
     * Called when we logoff to give the header client a chance to logoff.
     */
    protected static native void logoffHeaderClient () /*-{
       var client = $doc.getElementById("asclient");
       if (client) {
           client.onUnload();
       }
    }-*/;

    protected Application _app;
    protected WebCreds _creds;

    protected int _flowIdx, _goldIdx, _levelIdx;
    protected HTML _mailNotifier;

    /** The height of the header UI in pixels. */
    protected static final int HEADER_HEIGHT = 50;
}
