//
// $Id$

package client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.client.WebCreds;

import client.util.CookieUtil;

/**
 * Displays an interface for logging on, or a user's current credentials.
 */
public class LogonPanel extends HorizontalPanel
    implements KeyboardListener, AsyncCallback
{
    public LogonPanel (WebContext ctx, MsoyEntryPoint app)
    {
        _ctx = ctx;
        _app = app;

        setSpacing(5);

        // create our interface elements
        _status = new HTML("");
        _email = new TextBox();
        _email.addKeyboardListener(this);
        _password = new PasswordTextBox();
        _password.addKeyboardListener(this);
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
            displayChoice();
        } else {
            displayLoggedOn();
            _app.didLogon(_creds);
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
        CookieUtil.set("creds", "");
        _app.didLogoff();
        displayChoice();
    }

    // from interface KeyboardListener
    public void onKeyDown (Widget sender, char keyCode, int modifiers)
    {
        if (keyCode != KeyboardListener.KEY_ENTER) {
            return;
        }
        _who = _email.getText();
        String password = _password.getText();
        if (_who.length() > 0 && password.length() > 0) {
            displayStatus("Logging in...");
            _ctx.usersvc.login(_who, md5hex(password), false, this);
        }
    }

    // from interface KeyboardListener
    public void onKeyPress (Widget sender, char keyCode, int modifiers)
    {
    }

    // from interface KeyboardListener
    public void onKeyUp (Widget sender, char keyCode, int modifiers)
    {
    }

    // from interface AsyncCallback
    public void onSuccess (Object result)
    {
        _creds = (WebCreds)result;
        _password.setText("");
        CookieUtil.set("creds", _creds.toCookie());
        CookieUtil.set("who", _who);
        displayLoggedOn();
        _app.didLogon(_creds);
    }

    // from interface AsyncCallback
    public void onFailure (Throwable caught)
    {
        // TODO: report user friendly error; make it possible to display status
        // and the logon elements at the same time
        displayStatus("Error: " + caught.toString());
        displayLogon();
    }

    protected void displayChoice ()
    {
        clear();
        add(new Button("Login", new ClickListener() {
            public void onClick (Widget sender) {
                displayLogon();
            }
        }));
        add(new Label("or"));
        add(new Button("join!"));
    }

    protected void displayLogon ()
    {
        clear();
        add(new Label("Email:"));
        add(_email);
        if (_who != null) {
            _email.setText(_who);
        }
        add(new Label("Password"));
        add(_password);
    }

    protected void displayLoggedOn ()
    {
        clear();
        add(new HTML("Welcome <b>" + _who + "</b>"));
        add(new Button("Logout", new ClickListener() {
            public void onClick (Widget sender) {
                logout();
            }
        }));
    }

    protected void displayStatus (String status)
    {
        clear();
        add(_status);
        _status.setHTML(status);
    }

    protected native String md5hex (String text) /*-{
       return $wnd.hex_md5(text);
    }-*/;

    protected WebContext _ctx;
    protected MsoyEntryPoint _app;

    protected String _who;
    protected WebCreds _creds;

    protected TextBox _email;
    protected PasswordTextBox _password;
    protected HTML _status;
}
