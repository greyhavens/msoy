//
// $Id$

package client.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.data.WebCreds;

import client.shell.MsoyEntryPoint;
import client.shell.ShellContext;

/**
 * Displays an admin dashboard with various server status information and administrative
 * functionality.
 */
public class index extends MsoyEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public MsoyEntryPoint createEntryPoint () {
                return new index();
            }
        };
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "admin";
    }

    // @Override // from MsoyEntryPoint
    protected ShellContext createContext ()
    {
        return _ctx = new AdminContext();
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        _ctx.msgs = (AdminMessages)GWT.create(AdminMessages.class);
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        // nothing to do here
    }

    // @Override // from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        displayDashboard();
    }

    // @Override // from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        setContent(new Label(_ctx.msgs.indexLogon()));
    }

    protected void displayDashboard ()
    {
        if (_ctx.creds.isSupport) {
            setContent(new DashboardPanel(_ctx));
        } else {
            setContent(new Label(_ctx.msgs.lackPrivileges()));
        }
    }

    protected AdminContext _ctx;
}
