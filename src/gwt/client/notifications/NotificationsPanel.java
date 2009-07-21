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
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.notifications.gwt.Notification;
import com.threerings.msoy.notifications.gwt.NotificationType;
import com.threerings.msoy.notifications.gwt.NotificationsService;
import com.threerings.msoy.notifications.gwt.NotificationsServiceAsync;
import com.threerings.msoy.notifications.gwt.Notification.TrophyData;
import com.threerings.msoy.web.gwt.CookieNames;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.InfoCallback;
import client.util.MediaUtil;
import client.util.events.TrophyEvent;


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
        int size = _notifications.size();
        if (size == 0 || _dismissed) {
            setVisible(false);
            return;
        }

        setVisible(true);
        _selected = Math.max(Math.min(_selected, size - 1), 0);
        _cycle.setVisible(size > 1);
        _notification.setWidget(createPanel(_notifications.get(_selected)));
    }

    protected void selectNext ()
    {
        _selected = (_selected + 1) % _notifications.size();
        update();
    }

    protected Widget createPanel (final Notification notif)
    {
        AbsoluteCSSPanel panel = new AbsoluteCSSPanel("Content", "fixed");
        switch (notif.type) {
        case BOOKMARK:
            panel.addStyleName("Bookmark");
            panel.add(MsoyUI.createHTML(_msgs.bookmarkReminder(), "Reminder"));
            panel.add(easyButton(1, _msgs.bookmarkDidIt(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        clearBookmarkReminder();
                        removeNotification(notif);
                    }
                }));
            panel.add(easyButton(3, _msgs.bookmarkLater(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        removeNotification(notif);
                    }
                }));
            break;
        case PROMOTION:
            break;
        case TROPHY:
            panel.addStyleName("PublishTrophy");
            final TrophyData trophyData = (TrophyData)notif.data;
            final Trophy trophy = trophyData.trophy;
            panel.add(createThumbnail(trophy.trophyMedia, "Thumbnail"));
            panel.add(MsoyUI.createLabel(
                _msgs.publishTrophyTip(trophy.name, trophyData.gameName), "Tip"));
            panel.add(easyButton(2, _msgs.publishTrophy(), new ClickHandler () {
                @Override public void onClick (ClickEvent event) {
                    CShell.frame.dispatchEvent(new TrophyEvent(trophy.gameId, trophyData.gameName,
                        trophyData.gameDesc, trophy.name, trophy.ident, trophy.description,
                        trophy.trophyMedia.getMediaPath()));
                    removeNotification(notif);
                }
            }));
            break;
        }
        return panel;
    }

    protected void removeNotification (Notification notif)
    {
        int index = _notifications.indexOf(notif);
        if (index < 0) {
            return;
        }
        _notifications.remove(index);
        if (index < _selected) {
            _selected--;
        } else if (index == _selected) {
            if (_selected == _notifications.size()) {
                _selected = 0;
            }
            update();
        }
    }

    protected void clearBookmarkReminder ()
    {
        CookieUtil.set("/", 365, CookieNames.BOOKMARKED, "t");
    }

    protected PushButton easyButton (int slot, String text, ClickHandler handler)
    {
        PushButton button = MsoyUI.createImageButton("easyButton", handler);
        button.setText(text);
        button.getElement().setAttribute("slot", "" + slot);
        return button;
    }

    protected Widget createThumbnail (MediaDesc media, String style)
    {
        Widget w = MediaUtil.createMediaView(media, MediaDesc.THUMBNAIL_SIZE);
        w.addStyleName(style);
        return w;
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
