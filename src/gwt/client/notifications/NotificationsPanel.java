//
// $Id$

package client.notifications;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.AbsoluteCSSPanel;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.notifications.gwt.Notification;
import com.threerings.msoy.notifications.gwt.NotificationsService;
import com.threerings.msoy.notifications.gwt.NotificationsServiceAsync;

import client.ui.MsoyUI;
import client.util.InfoCallback;


/**
 * Displays messages of interest to the user.
 */
public class NotificationsPanel extends FlowPanel
{
    public NotificationsPanel ()
    {
        setStyleName("notifications");
        AbsoluteCSSPanel content = new AbsoluteCSSPanel("Absolute", "fixed");
        content.add(MsoyUI.createLabel("Notifications", "Test"));
        content.add(MsoyUI.createImageButton("Close", new ClickHandler() {
            public void onClick (ClickEvent event) {
                setVisible(false);
            }
        }));
        add(content);
        if (DeploymentConfig.fbnotifications) {
            // TODO: check for bookmark cookie here, then add the others to the list when they
            // come in
            _nsvc.getNotifications(new InfoCallback<List<Notification>>() {
                @Override public void onSuccess (List<Notification> result) {
                    // TODO
                }
            });
        } else {
            setVisible(false);
        }
    }

    protected List<Notification> _notifications = new ArrayList<Notification>();

    protected static final NotificationsServiceAsync _nsvc =
        GWT.create(NotificationsService.class);
}
