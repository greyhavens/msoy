//
// $Id$

package client.admin;

import client.util.MsoyUI;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

/**
 * Displays the various services available to support and admin personnel.
 */
public class DashboardPanel extends VerticalPanel
{
    public DashboardPanel ()
    {
        add(MsoyUI.createLabel(CAdmin.msgs.title(), "title"));

        add(WidgetUtil.createApplet(
                "admin", "/clients/admin-client.jar",
                "com.threerings.msoy.admin.client.AdminApplet", 800, 400,
                new String[] { "server", "localhost", // config.server,
                               "port", "4010", // "" + config.port,
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
