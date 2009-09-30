//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;

import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * TODO: move to client.apps
 */
public class FacebookNotificationsPanel extends FlowPanel
{
    public FacebookNotificationsPanel ()
    {
        setStyleName("facebookNotifications");

        add(MsoyUI.createLabel(_msgs.fbNotifsDailyTitle(), "Title"));
        final TextArea dailyNotifications = MsoyUI.createTextArea("", 32, 5);
        Button updateRotation = new Button(_msgs.fbNotifsDailyUpdateBtn());
        add(MsoyUI.createFlowPanel("Rotation",
            makeRow(_msgs.fbNotifsDailyLabel(), dailyNotifications),
            makeRow("", updateRotation)));

        new ClickCallback<Void>(updateRotation) {
            @Override public boolean callService () {
                String[] ids = dailyNotifications.getText().split("( |\t|\r|\n)+");
                _adminsvc.setDailyNotifications(ids, this);
                return false;
            }
            @Override public boolean gotResult (Void result) {
                MsoyUI.info(_msgs.fbNotifsDailyUpdated());
                return true;
            }
        };
    }

    protected Widget makeRow (String label, Widget w)
    {
        FloatPanel row = new FloatPanel("Row");
        row.add(MsoyUI.createLabel(label, null));
        row.add(w);
        return row;
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
}
