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
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.InfoCallback;
import client.util.Link;
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
        AbsoluteCSSPanel panel = new AbsoluteCSSPanel("Content", "fixed");
        Notification notif = _notifications.get(index);
        switch (notif.type) {
        case BOOKMARK:
            panel.addStyleName("Bookmark");
            panel.add(MsoyUI.createHTML(_msgs.bookmarkReminder(), "Reminder"));
            panel.add(easyButton(1, _msgs.bookmarkDidIt(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        clearBookmarkReminder();
                        removeNotification(index);
                    }
                }));
            panel.add(easyButton(2, _msgs.bookmarkLater(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        removeNotification(index);
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
            panel.add(MsoyUI.createLabel(_msgs.publishTrophyTip(trophy.name), "Tip"));
            panel.add(easyButton(1, _msgs.publishTrophy(), new ClickHandler () {
                @Override public void onClick (ClickEvent event) {
                    CShell.frame.dispatchEvent(new TrophyEvent(trophy.gameId, trophyData.gameName,
                        trophyData.gameDesc, trophy.name, trophy.ident, trophy.description,
                        trophy.trophyMedia.getMediaPath()));
                    //removeNotification(index);
                }
            }));
            panel.add(easyButton(2, _msgs.publishTrophyInfo(), Link.createHandler(Pages.GAMES,
                SharedNaviUtil.GameDetails.TROPHIES.args(trophy.gameId))));
            break;
        }
        return panel;
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

    protected PushButton easyButton (int row, String text, ClickHandler handler)
    {
        PushButton button = MsoyUI.createImageButton("easyButton", handler);
        button.setText(text);
        button.getElement().setAttribute("row", "" + row);
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
