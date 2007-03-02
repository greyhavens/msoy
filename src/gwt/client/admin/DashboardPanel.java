//
// $Id$

package client.admin;

import client.util.MsoyUI;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.ConnectConfig;

/**
 * Displays the various services available to support and admin personnel.
 */
public class DashboardPanel extends FlexTable
{
    public DashboardPanel (ConnectConfig config)
    {
        setStyleName("dashboardPanel");
        setCellSpacing(0);
        setCellPadding(0);

        int row = 0;
        getFlexCellFormatter().setStyleName(row, 0, "Title");
        setText(row++, 0, CAdmin.msgs.title());

        // add various controls
        HorizontalPanel controls = new HorizontalPanel();
        controls.setSpacing(10);
        setWidget(row++, 0, controls);

        controls.add(new Label(CAdmin.msgs.controls()));
        controls.add(new Button(CAdmin.msgs.reviewButton(), new ClickListener() {
            public void onClick (Widget sender) {
                new ReviewPopup().show();
            }
        }));
        if (CAdmin.creds.isAdmin) {
            controls.add(new Button(CAdmin.msgs.invitePlayers(), new ClickListener() {
                public void onClick (Widget sender) {
                    new InvitePlayersPopup().show();
                }
            }));
        }

        // add the server dashboard applet
        getFlexCellFormatter().setStyleName(row, 0, "Applet");
        setWidget(row++, 0, WidgetUtil.createApplet(
                      "admin", "/clients/admin-client.jar",
                      "com.threerings.msoy.admin.client.AdminApplet", 800, 400,
                      new String[] { "server", config.server,
                                     "port", "" + config.port,
                                     "authtoken", CAdmin.creds.token }));
    }
}
