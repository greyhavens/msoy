//
// $Id$

package client.admin;

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

import client.shell.Page;
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

        // add various controls
        HorizontalPanel controls = new HorizontalPanel();
        controls.setSpacing(10);
        setWidget(row++, 0, controls);

        controls.add(new Label(CAdmin.msgs.adminControls()));
        if (CAdmin.isAdmin()) {
            controls.add(new Button(CAdmin.msgs.displayDashboard(), new ClickListener() {
                public void onClick (Widget sender) {
                    ((Button)sender).setEnabled(false);
                    displayDashboard();
                }
            }));
            controls.add(new Button(CAdmin.msgs.browserPlayers(), new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Page.ADMIN, "browser");
                }
            }));
            controls.add(new Button(CAdmin.msgs.spamPlayers(), new ClickListener() {
                public void onClick (Widget sender) {
                    new SpamPlayersDialog().show();
                }
            }));
        }
        controls.add(new Button(CAdmin.msgs.reviewButton(), new ClickListener() {
            public void onClick (Widget sender) {
                Link.go(Page.ADMIN, "review");
            }
        }));

        controls = new HorizontalPanel();
        controls.setSpacing(10);
        setWidget(row++, 0, controls);
        controls.add(new Label(CAdmin.msgs.inviteControls()));
        if (CAdmin.isAdmin()) {
            controls.add(new Button(CAdmin.msgs.issueInvites(), new ClickListener() {
                public void onClick (Widget sender) {
                    new IssueInvitesDialog().show();
                }
            }));
        }

        // a/b testing controls
        controls = new HorizontalPanel();
        controls.setSpacing(10);
        setWidget(row++, 0, controls);
        controls.add(new Label(CAdmin.msgs.testingControls()));
        if (CAdmin.isAdmin()) {
            controls.add(new Button(CAdmin.msgs.viewABTests(), new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Page.ADMIN, "testlist");
                }
            }));
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
        CAdmin.frame.closeClient();

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
                                     "authtoken", CAdmin.ident.token }));
    }

    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
