//
// $Id$

package client.admin;

import client.util.MsoyUI;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays the various services available to support and admin personnel.
 */
public class DashboardPanel extends VerticalPanel
{
    public DashboardPanel ()
    {
        add(MsoyUI.createLabel(CAdmin.msgs.title(), "title"));
        Button reviewButton = new Button(CAdmin.msgs.reviewButton());
        reviewButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                new ReviewPopup().show();
            }
        });
        add(reviewButton);
    }
}
