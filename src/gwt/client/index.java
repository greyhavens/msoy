//
// $Id$

package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;

/**
 * Handles the MetaSOY main page.
 */
public class index
    implements EntryPoint, HistoryListener
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // get access to our service
        _usersvc = (WebUserServiceAsync)GWT.create(WebUserService.class);
        ServiceDefTarget target = (ServiceDefTarget)_usersvc;
        target.setServiceEntryPoint("/user");

        LoginHelper helper = new LoginHelper();
        FlexTable table = new FlexTable();
        table.setText(0, 0, "Username");
        table.setWidget(0, 1, _username = new TextBox());
        _username.addChangeListener(helper);
        table.setText(1, 0, "Password");
        table.setWidget(1, 1, _password = new TextBox());
        _password.addChangeListener(helper);
        table.setText(2, 0, "Cookie");
        table.setWidget(2, 1, _cookie = new Label(""));
        RootPanel.get("content").add(table);

        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else {
            // displaySummary();
        }
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        // TODO
    }

    protected class LoginHelper implements ChangeListener, AsyncCallback {
        public void onChange (Widget sender) {
            String username = _username.getText();
            String password = _password.getText();
            if (username.length() > 0 && password.length() > 0) {
                _usersvc.login(username, password, false, this);
            }
        }
        public void onSuccess (Object result) {
            _cookie.setText((String)result);
        }
        public void onFailure (Throwable caught) {
            _cookie.setText("Error: " + caught.toString());
        }
    }

    protected WebUserServiceAsync _usersvc;
    protected TextBox _username;
    protected TextBox _password;
    protected Label _cookie;
}
