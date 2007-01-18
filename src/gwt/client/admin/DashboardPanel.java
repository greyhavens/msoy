//
// $Id$

package client.admin;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import client.util.MsoyUI;

/**
 * Displays the various services available to support and admin personnel.
 */
public class DashboardPanel extends VerticalPanel
{
    public DashboardPanel (AdminContext ctx)
    {
        _ctx = ctx;

        add(MsoyUI.createLabel(_ctx.msgs.title(), "title"));
        add(new Label(_ctx.msgs.todo()));
    }

    protected AdminContext _ctx;
}
