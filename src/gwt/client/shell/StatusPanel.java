//
// $Id$

package client.shell;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.web.data.WebCreds;

import client.util.FlashClients;

/**
 * Displays basic player status (name, flow count) and handles logging on and logging off.
 */
public class StatusPanel extends FlexTable
{
    public StatusPanel (MsoyEntryPoint app)
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
     * Clears out our credentials and displays the logon interface.
     */
    public void logoff ()
    {
        _creds = null;
        clearCookie("creds");
        _app.didLogoff();

        reset();
        setText(0, 0, "New to MetaSOY? Create an account!");
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
        boolean needHeaderClient = _app.didLogon(_creds);

        reset();
        int idx = 0;

        // create the header Flash client if we don't have a real client on the page
        if (needHeaderClient) {
            setWidget(0, idx++, FlashClients.createHeaderClient(_creds.token));
        }

        setText(0, idx++, _creds.name + ":");
        getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
        setText(0, idx++, "47"); // gold
        getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
        setText(0, idx++, "12895"); // flow
        getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
        setText(0, idx++, "638"); // whuffy
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

    protected class LogonPopup extends PopupPanel
        implements ClickListener
    {
        public LogonPopup ()
        {
            super(true);
            setStyleName("logonPopup");

            FlexTable contents = new FlexTable();
            setWidget(contents);
            contents.setText(0, 0, "Email:");
            contents.setWidget(0, 1, _email = new TextBox());
            if (_creds != null) {
                _email.setText(_creds.accountName);
            }
            _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
                public void onClick (Widget sender) {
                    _password.setFocus(true);
                }
            }));

            contents.setText(1, 0, "Password:");
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
                _status.setText("Logging in...");
                CShell.usersvc.login(account, md5hex(password), 1, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        hide();
                        didLogon((WebCreds)result);
                    }
                    public void onFailure (Throwable caught) {
                        _status.setText("Error: " + caught.getMessage());
                    }
                });
            }
        }

        protected TextBox _email;
        protected PasswordTextBox _password;
        protected Label _status;
    }

    protected MsoyEntryPoint _app;
    protected WebCreds _creds;

    /** The height of the header UI in pixels. */
    protected static final int HEADER_HEIGHT = 50;
}
