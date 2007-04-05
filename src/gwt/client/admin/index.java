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
import client.util.MsoyUI;
import client.shell.Page;

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
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override from Page
    public void onHistoryChanged (String token)
    {
        if (CAdmin.creds == null) {
            setContent(MsoyUI.createLabel(CAdmin.msgs.indexLogon(), "infoLabel"));
        } else {
            displayDashboard();
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "admin";
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CAdmin.adminsvc = (AdminServiceAsync)GWT.create(AdminService.class);
        ((ServiceDefTarget)CAdmin.adminsvc).setServiceEntryPoint("/adminsvc");

        // load up our translation dictionaries
        CAdmin.msgs = (AdminMessages)GWT.create(AdminMessages.class);
    }

    protected void displayDashboard ()
    {
        if (CAdmin.creds.isSupport) {
            setPageTitle(CAdmin.msgs.title());
            setContent(new DashboardPanel());
        } else {
            setContent(MsoyUI.createLabel(CAdmin.msgs.lackPrivileges(), "infoLabel"));
        }
    }
}
