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

import com.threerings.msoy.web.client.WebUserServiceAsync;

import client.util.CookieUtil;

/**
 * Displays an interface for logging on, or a user's current credentials.
 */
public class LogonPanel extends HorizontalPanel
    implements KeyboardListener, AsyncCallback
{
    public LogonPanel (WebUserServiceAsync usersvc)
    {
        _usersvc = usersvc;

        // create our interface elements
        _status = new HTML("");
        _email = new TextBox();
        _email.addKeyboardListener(this);
        _password = new PasswordTextBox();
        _password.addKeyboardListener(this);

        _who = CookieUtil.get("who");
        _token = CookieUtil.get("token");
        if (_token == null || _token.length() == 0) {
            displayChoice();
        } else {
            displayLoggedOn();
        }
    }

    /**
     * Returns our authentication token or null if we are not logged in.
     */
    public String getAuthToken ()
    {
        return _token;
    }

    /**
     * Clears out our credentials and displays the logon interface.
     */
    public void logout ()
    {
        _token = null;
        CookieUtil.set("token", "");
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
            _usersvc.login(_who, md5hex(password), false, this);
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
        _token = (String)result;
        _password.setText("");
        CookieUtil.set("token", _token);
        CookieUtil.set("who", _who);
        displayLoggedOn();
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

    protected WebUserServiceAsync _usersvc;
    protected String _who, _token;

    protected TextBox _email;
    protected PasswordTextBox _password;
    protected HTML _status;
}
