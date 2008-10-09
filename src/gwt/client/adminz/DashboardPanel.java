//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.ConnectConfig;

import client.shell.CShell;
import client.shell.Pages;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays the various services available to support and admin personnel.
 */
public class DashboardPanel extends FlexTable
{
    public DashboardPanel ()
    {
        setStyleName("dashboardPanel");
        setCellSpacing(0);
        setCellPadding(0);

        int row = 0;
        HorizontalPanel controls;

        // admin-only controls
        if (CShell.isAdmin()) {
            controls = new HorizontalPanel();
            controls.setSpacing(10);
            setWidget(row++, 0, controls);
            controls.add(new Label(_msgs.adminControls()));
            controls.add(new Button(_msgs.displayDashboard(), new ClickListener() {
                public void onClick (Widget sender) {
                    ((Button)sender).setEnabled(false);
                    displayDashboard();
                }
            }));
            controls.add(new Button(_msgs.browserPlayers(),
                                    Link.createListener(Pages.ADMINZ, "browser")));
            controls.add(new Button(_msgs.spamPlayers(), new ClickListener() {
                public void onClick (Widget sender) {
                    new SpamPlayersDialog().show();
                }
            }));

            controls.add(new Button("Exchange", Link.createListener(Pages.ADMINZ, "exchange")));
        }

        // support controls
        controls = new HorizontalPanel();
        controls.setSpacing(10);
        setWidget(row++, 0, controls);
        controls.add(new Label(_msgs.supportControls()));
        controls.add(new Button(_msgs.reviewButton(), Link.createListener(Pages.ADMINZ, "review")));
        controls.add(new Button(_msgs.affMapButton(), Link.createListener(Pages.ADMINZ, "affmap")));
        
        // Only add cash out button if bars are enabled.
        if (CShell.barsEnabled()) {
            controls.add(new Button(_msgs.cashOutButton(), Link.createListener(Pages.ADMINZ, "cashout")));
        }
        
        // invitation controls
        controls = new HorizontalPanel();
        controls.setSpacing(10);
        setWidget(row++, 0, controls);
        controls.add(new Label(_msgs.inviteControls()));
        if (CShell.isAdmin()) {
            controls.add(new Button(_msgs.issueInvites(), new ClickListener() {
                public void onClick (Widget sender) {
                    new IssueInvitesDialog().show();
                }
            }));
        }

        // a/b testing controls
        controls = new HorizontalPanel();
        controls.setSpacing(10);
        setWidget(row++, 0, controls);
        controls.add(new Label(_msgs.testingControls()));
        if (CShell.isAdmin()) {
            controls.add(new Button(_msgs.viewABTests(),
                                    Link.createListener(Pages.ADMINZ, "testlist")));
        }
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
                                     "authtoken", CShell.getAuthToken() }));
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
