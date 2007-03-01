//
// $Id$

package client.admin;

import client.util.MsoyUI;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.ConnectConfig;

/**
 * Displays the various services available to support and admin personnel.
 */
public class DashboardPanel extends VerticalPanel
{
    public DashboardPanel (ConnectConfig config)
    {
        add(MsoyUI.createLabel(CAdmin.msgs.title(), "title"));

        add(WidgetUtil.createApplet(
                "admin", "/clients/admin-client.jar",
                "com.threerings.msoy.admin.client.AdminApplet", 800, 400,
                new String[] { "server", config.server,
                               "port", "" + config.port,
                               "authtoken", CAdmin.creds.token }));

        Button reviewButton = new Button(CAdmin.msgs.reviewButton());
        reviewButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                new ReviewPopup().show();
            }
        });
        add(reviewButton);
    }
}
