//
// $Id$

package client.shell;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;

import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.WebCreds;

import client.util.CookieUtil;

/**
 * Displays an interface for logging on, or a user's current credentials.
 */
public class LogonPanel extends FlexTable
{
    public LogonPanel (WebContext ctx, MsoyEntryPoint app)
    {
        setStyleName("logonPanel");
        setCellPadding(0);
        setCellSpacing(0);

        _ctx = ctx;
        _app = app;

        // create our interface elements
        setWidget(0, 0, _top = new Label(""));
        _top.setStyleName("Top");
        setWidget(0, 1, _action = new Button("", new ClickListener() {
            public void onClick (Widget sender) {
                actionClicked();
            }
        }));
        getFlexCellFormatter().setRowSpan(0, 1, 2);
        setWidget(1, 0, _main = new Label(""));
        _main.setStyleName("Main");
    }

    /**
     * Called once the rest of our application is set up. Checks to see if
     * we're already logged on, in which case it triggers a call to didLogon().
     */
    public void init ()
    {
        _who = CookieUtil.get("who");
        _creds = WebCreds.fromCookie(CookieUtil.get("creds"));
        if (_creds == null) {
            logout();
        } else {
            didLogon(_creds);
        }
    }

    /**
     * Returns our credentials or null if we are not logged in.
     */
    public WebCreds getCredentials ()
    {
        return _creds;
    }

    /**
     * Clears out our credentials and displays the logon interface.
     */
    public void logout ()
    {
        _creds = null;
        clearCookie("creds");
        _app.didLogoff();

        _top.setText("Logon or");
        _main.setText("Join!");
        _action.setText("Go");
    }

    protected void didLogon (WebCreds creds)
    {
        _creds = creds;
        setCookie("creds", _creds.toCookie());
        setCookie("who", _who);
        _app.didLogon(_creds);

        _top.setText("Welcome");
        _main.setText(_who);
        _action.setText("Logoff");
    }

    protected void actionClicked ()
    {
        if (_creds == null) {
            LogonPopup popup = new LogonPopup();
            popup.show();
            popup.setPopupPosition(
                Window.getClientWidth() - popup.getOffsetWidth(), HEADER_HEIGHT);
        } else {
            logout();
        }
    }

    protected void setCookie (String name, String value)
    {
        String domain = GWT.isScript() ? "metasoy.com" : "";
        CookieUtil.set(domain, "/", 7, name, value);
    }

    protected void clearCookie (String name)
    {
        String domain = GWT.isScript() ? "metasoy.com" : "";
        CookieUtil.clear(domain, "/", name);
    }

    protected native String md5hex (String text) /*-{
       return $wnd.hex_md5(text);
    }-*/;

    protected class LogonPopup extends PopupPanel
        implements ClickListener, AsyncCallback
    {
        public LogonPopup ()
        {
            super(true);
            setStyleName("logonPopup");

            FlexTable contents = new FlexTable();
            setWidget(contents);
            contents.setText(0, 0, "Email:");
            contents.setWidget(0, 1, _email = new TextBox());
            if (_who != null) {
                _email.setText(_who);
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
            _who = _email.getText();
            String password = _password.getText();
            if (_who.length() > 0 && password.length() > 0) {
                _status.setText("Logging in...");
                _ctx.usersvc.login(_who, md5hex(password), false, this);
            }
        }

        // from interface AsyncCallback
        public void onSuccess (Object result)
        {
            hide();
            didLogon((WebCreds)result);
        }

        // from interface AsyncCallback
        public void onFailure (Throwable caught)
        {
            _status.setText("Error: " + caught.getMessage());
        }

        protected TextBox _email;
        protected PasswordTextBox _password;
        protected Label _status;
    }

    protected WebContext _ctx;
    protected MsoyEntryPoint _app;

    protected String _who;
    protected WebCreds _creds;

    protected Label _top, _main;
    protected Button _action;

    /** The height of the header UI in pixels. */
    protected static final int HEADER_HEIGHT = 43;
}
