//
// $Id$

package client;

import com.google.gwt.user.client.rpc.AsyncCallback;

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
        _username = new TextBox();
        _username.addKeyboardListener(this);
        _password = new PasswordTextBox();
        _password.addKeyboardListener(this);

        String username = CookieUtil.get("who");
        _token = CookieUtil.get("token");
        if (_token == null) {
            displayLogon(username);
        } else {
            displayCreds(username);
        }
    }

    /**
     * Returns our authentication token or null if we are not logged in.
     */
    public String getAuthToken ()
    {
        return _token;
    }

    // from interface KeyboardListener
    public void onKeyDown (Widget sender, char keyCode, int modifiers)
    {
        if (keyCode != KeyboardListener.KEY_ENTER) {
            return;
        }
        _who = _username.getText();
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
        CookieUtil.set("token", _token);
        CookieUtil.set("who", _who);
        displayCreds(_who);
    }

    // from interface AsyncCallback
    public void onFailure (Throwable caught)
    {
        // TODO: report user friendly error; make it possible to display status
        // and the logon elements at the same time
        displayStatus("Error: " + caught.toString());
        displayLogon(_who);
    }

    protected void displayLogon (String who)
    {
        clear();
        add(new Label("Username:"));
        add(_username);
        if (who != null) {
            _username.setText(who);
        }
        add(new Label("Password"));
        add(_password);
    }

    protected void displayStatus (String status)
    {
        clear();
        add(_status);
        _status.setHTML(status);
    }

    protected void displayCreds (String who)
    {
        displayStatus("Welcome <b>" + who + "</b>");
    }

    protected native String md5hex (String text) /*-{
       return $wnd.hex_md5(text);
    }-*/;

    protected WebUserServiceAsync _usersvc;
    protected String _who, _token;

    protected TextBox _username;
    protected PasswordTextBox _password;
    protected HTML _status;
}
