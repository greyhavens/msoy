//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.ConnectConfig;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays the various services available to support and admin personnel.
 */
public class DashboardPanel extends SmartTable
{
    public DashboardPanel ()
    {
        super("dashboardPanel", 0, 0);

        int row = 0;
        HorizontalPanel controls;

        // admin-only controls
        if (CShell.isAdmin()) {
            //controls.setSpacing(10);
            VerticalPanel rhs = new VerticalPanel();
            setText(row, 0, _msgs.adminControls());
            getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
            setWidget(row++, 1, rhs);
            rhs.add(controls = new HorizontalPanel());
            controls.setSpacing(10);
            controls.add(new Button(_msgs.displayDashboard(), new ClickListener() {
                public void onClick (Widget sender) {
                    ((Button)sender).setEnabled(false);
                    displayDashboard();
                }
            }));
            controls.add(new Button(_msgs.viewExchange(),
                                    Link.createListener(Pages.ADMINZ, "exchange")));
            controls.add(new Button(_msgs.cashOutButton(),
                                    Link.createListener(Pages.ADMINZ, "cashout")));
            rhs.add(controls = new HorizontalPanel());
            controls.setSpacing(10);
            controls.add(new Button(_msgs.viewABTests(),
                                    Link.createListener(Pages.ADMINZ, "testlist")));
            controls.add(new Button(_msgs.viewBureaus(),
                                    Link.createListener(Pages.ADMINZ, "bureaus")));
        }

        // support controls
        setText(row, 0, _msgs.supportControls());
        getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        setWidget(row++, 1, controls = new HorizontalPanel());
        controls.setSpacing(10);
        controls.add(new Button(_msgs.reviewButton(), Link.createListener(Pages.ADMINZ, "review")));
        controls.add(new Button(_msgs.affMapButton(), Link.createListener(Pages.ADMINZ, "affmap")));
        controls.add(new Button(_msgs.promosButton(), Link.createListener(Pages.ADMINZ, "promos")));
        controls.add(new Button(_msgs.browseButton(), Link.createListener(Pages.ADMINZ, "browser")));
    }

    protected void displayDashboard ()
    {
        // load up the information needed to display the dashboard applet
        _usersvc.getConnectConfig(new MsoyCallback<ConnectConfig>() {
            public void onSuccess (ConnectConfig config) {
                finishDisplayDashboard(config);
            }
        });
    }

    protected void finishDisplayDashboard (ConnectConfig config)
    {
        CShell.frame.closeClient();

        // we have to serve admin-client.jar from the server to which it will connect back due to
        // security restrictions and proxy the game jar through there as well
        String appletURL = config.getURL(
            "/clients/" + DeploymentConfig.version + "/admin-client.jar");

        int row = getRowCount();
        getFlexCellFormatter().setStyleName(row, 0, "Applet");
        setWidget(row, 0, WidgetUtil.createApplet(
                      "admin", appletURL,
                      "com.threerings.msoy.admin.client.AdminApplet", 680, 400, false,
                      new String[] { "server", config.server,
                                     "port", "" + config.port,
                                     "authtoken", CShell.getAuthToken() }),
                  2, null);
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
