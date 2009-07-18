//
// $Id$

package client.notifications;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.notifications.gwt.Notification;
import com.threerings.msoy.notifications.gwt.NotificationType;
import com.threerings.msoy.notifications.gwt.NotificationsService;
import com.threerings.msoy.notifications.gwt.NotificationsServiceAsync;
import com.threerings.msoy.web.gwt.CookieNames;

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
        // the outer panel cannot be absolute for some reason, so embed one <sigh>
        AbsoluteCSSPanel content = new AbsoluteCSSPanel("Absolute", "fixed");
        content.add(MsoyUI.createImageButton("Close", new ClickHandler() {
            public void onClick (ClickEvent event) {
                _dismissed = true;
                update();
            }
        }));
        content.add(_cycle = MsoyUI.createImageButton("Cycle", new ClickHandler() {
            public void onClick (ClickEvent event) {
                selectNext();
            }
        }));
        content.add(_notification = new SimplePanel());
        _notification.setStyleName("notification");
        add(content);

        if (!DeploymentConfig.fbnotifications) {
            setVisible(false);
            return;
        }

        if (CookieUtil.get(CookieNames.BOOKMARKED) == null) {
            Notification bookmark = new Notification();
            bookmark.type = NotificationType.BOOKMARK;
            _notifications.add(bookmark);
        }
        update();
        _nsvc.getNotifications(new InfoCallback<List<Notification>>() {
            @Override public void onSuccess (List<Notification> result) {
                // TODO: do we need to re-sort?
                if (result != null) {
                    _notifications.addAll(result);
                    update();
                }
            }
        });
    }

    protected void update ()
    {
        if (_notifications.size() == 0 || _dismissed) {
            setVisible(false);
            return;
        }

        setVisible(true);
        _cycle.setVisible(_notifications.size() > 1);
        _notification.setWidget(createPanel(_selected));
    }

    protected void selectNext ()
    {
        _selected = (_selected + 1) % _notifications.size();
        update();
    }

    protected Widget createPanel (final int index)
    {
        Notification notification = _notifications.get(index);
        switch (notification.type) {
        case BOOKMARK:
            AbsoluteCSSPanel panel = new AbsoluteCSSPanel("Bookmark", "fixed");
            PushButton button;
            panel.add(MsoyUI.createHTML(_msgs.bookmarkReminder(), "Reminder"));
            panel.add(button = MsoyUI.createImageButton("DidIt", new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        clearBookmarkReminder();
                        removeNotification(index);
                    }
                }));
            button.setText(_msgs.bookmarkDidIt());
            panel.add(button = MsoyUI.createImageButton("Later", new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        removeNotification(index);
                    }
                }));
            button.setText(_msgs.bookmarkLater());
            return panel;
        case PROMOTION:
        case TROPHY:
        }
        return null;
    }

    protected void removeNotification (int index)
    {
        if (index >= _notifications.size()) {
            return;
        }
        _notifications.remove(index);
        if (index < _selected) {
            _selected--;
        }
        update();
    }

    protected void clearBookmarkReminder ()
    {
        CookieUtil.set("/", 365, CookieNames.BOOKMARKED, "t");
    }

    protected List<Notification> _notifications = new ArrayList<Notification>();
    protected PushButton _cycle;
    protected SimplePanel _notification;
    protected boolean _dismissed;
    protected int _selected;

    protected static final NotificationsServiceAsync _nsvc =
        GWT.create(NotificationsService.class);
    protected static final NotificationsMessages _msgs = GWT.create(NotificationsMessages.class);
}
