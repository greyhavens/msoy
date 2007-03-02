//
// $Id$

package client.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.client.AdminService;
import com.threerings.msoy.web.client.AdminServiceAsync;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.editem.EditemEntryPoint;
import client.shell.MsoyEntryPoint;

/**
 * Displays an admin dashboard with various server status information and administrative
 * functionality.
 */
public class index extends EditemEntryPoint
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
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CAdmin.adminsvc = (AdminServiceAsync)GWT.create(AdminService.class);
        ((ServiceDefTarget)CAdmin.adminsvc).setServiceEntryPoint("/adminsvc");

        // load up our translation dictionaries
        CAdmin.msgs = (AdminMessages)GWT.create(AdminMessages.class);
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        // nothing to do here
    }

    // @Override // from MsoyEntryPoint
    protected boolean didLogon (WebCreds creds)
    {
        boolean header = super.didLogon(creds);
        displayDashboard();
        return false; // TEMP: no header client as it conflicts with dashboard
    }

    // @Override // from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        setContent(new Label(CAdmin.msgs.indexLogon()));
    }

    protected void displayDashboard ()
    {
        if (CAdmin.creds.isSupport) {
            setContent(new DashboardPanel());
        } else {
            setContent(new Label(CAdmin.msgs.lackPrivileges()));
        }
    }
}
