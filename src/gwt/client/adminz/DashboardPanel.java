//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.ConnectConfig;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.ClickCallback;
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
        super("dashboardPanel", 0, 10);
        int row = 0, col = 0;

        // display some infoez
        setHTML(row, 0, _msgs.dashVersion(DeploymentConfig.version));
        setHTML(row++, 1, _msgs.dashBuilt(DeploymentConfig.buildTime));

        // admin-only controls
        if (CShell.isAdmin()) {
            FlowPanel admin = new FlowPanel();
            admin.add(MsoyUI.createLabel(_msgs.adminControls(), "Title"));
            admin.add(MsoyUI.createActionLabel(_msgs.displayDashboard(), new ClickListener() {
                public void onClick (Widget sender) {
                    ((Label)sender).removeClickListener(this);
                    sender.removeStyleName("actionLabel");
                    displayDashboard();
                }
            }));
            admin.add(makeLink(_msgs.viewExchange(), "exchange"));
            admin.add(makeLink(_msgs.cashOutButton(), "cashout"));
            admin.add(makeLink(_msgs.statsButton(), "stats"));
            admin.add(makeLink(_msgs.viewABTests(), "testlist"));
            admin.add(makeLink(_msgs.viewBureaus(), "bureaus"));
            admin.add(makeLink(_msgs.panopticonStatus(), "panopticonStatus"));
            setWidget(row, col, admin);
            getFlexCellFormatter().setVerticalAlignment(0, col++, HasAlignment.ALIGN_TOP);

            FlowPanel reboot = new FlowPanel();
            reboot.add(MsoyUI.createLabel(_msgs.adminReboot(), "Title"));
            reboot.add(makeReboot(_msgs.rebootNow(), 0));
            reboot.add(makeReboot(_msgs.rebootInFive(), 5));
            reboot.add(makeReboot(_msgs.rebootInFifteen(), 15));
            reboot.add(makeReboot(_msgs.rebootCancel(), -1));
            setWidget(row, col, reboot);
            getFlexCellFormatter().setVerticalAlignment(0, col++, HasAlignment.ALIGN_TOP);
        }

        // support controls
        FlowPanel support = new FlowPanel();
        support.add(MsoyUI.createLabel(_msgs.supportControls(), "Title"));
        support.add(makeLink(_msgs.reviewButton(), "review"));
        support.add(makeLink(_msgs.affMapButton(), "affmap"));
        support.add(makeLink(_msgs.promosButton(), "promos"));
        support.add(makeLink(_msgs.contestsButton(), "contests"));
        support.add(makeLink(_msgs.browseButton(), "browser"));
        setWidget(row++, col, support);
        getFlexCellFormatter().setVerticalAlignment(0, col++, HasAlignment.ALIGN_TOP);
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

    protected Widget makeLink (String title, String args)
    {
        Widget link = Link.create(title, Pages.ADMINZ, args);
        link.removeStyleName("inline");
        return link;
    }

    protected Widget makeReboot (String title, final int minutes)
    {
        Button reboot = new Button(title);
        new ClickCallback<Void>(reboot) {
            protected boolean callService () {
                _adminsvc.scheduleReboot(minutes, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(minutes < 0 ? _msgs.rebootCancelled() :
                            _msgs.rebootScheduled(""+minutes));
                return true;
            }
        };
        return reboot;
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
                  getCellCount(0), null);
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
