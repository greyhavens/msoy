//
// $Id$

package client.reminders;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.reminders.gwt.Reminder.TrophyData;
import com.threerings.msoy.reminders.gwt.Reminder;
import com.threerings.msoy.reminders.gwt.ReminderType;
import com.threerings.msoy.reminders.gwt.RemindersService;
import com.threerings.msoy.reminders.gwt.RemindersServiceAsync;
import com.threerings.msoy.web.gwt.CookieNames;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.InfoCallback;
import client.util.MediaUtil;
import client.util.events.TrophyEvent;

/**
 * Displays messages of interest to the user reminding them that they work for us.
 */
public class RemindersPanel extends FlowPanel
{
    public RemindersPanel ()
    {
        setStyleName("reminders");
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
        content.add(_reminder = new SimplePanel());
        _reminder.setStyleName("reminder");
        add(content);

        if (CookieUtil.get(CookieNames.BOOKMARKED) == null) {
            Reminder bookmark = new Reminder();
            bookmark.type = ReminderType.BOOKMARK;
            _reminders.add(bookmark);
        }
        update();
        _svc.getReminders(CShell.getAppId(), new InfoCallback<List<Reminder>>() {
            @Override public void onSuccess (List<Reminder> result) {
                // TODO: do we need to re-sort?
                if (result != null) {
                    _reminders.addAll(result);
                    update();
                }
            }
        });
    }

    protected void update ()
    {
        int size = _reminders.size();
        if (size == 0 || _dismissed) {
            setVisible(false);
            return;
        }

        setVisible(true);
        _selected = Math.max(Math.min(_selected, size - 1), 0);
        _cycle.setVisible(size > 1);
        _reminder.setWidget(createPanel(_reminders.get(_selected)));
    }

    protected void selectNext ()
    {
        _selected = (_selected + 1) % _reminders.size();
        update();
    }

    protected Widget createPanel (final Reminder reminder)
    {
        AbsoluteCSSPanel panel = new AbsoluteCSSPanel("Content", "fixed");
        switch (reminder.type) {
        case BOOKMARK:
            panel.addStyleName("Bookmark");
            panel.add(MsoyUI.createHTML(_msgs.bookmarkReminder(), "Reminder"));
            panel.add(easyButton(1, _msgs.bookmarkDidIt(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        clearBookmarkReminder();
                        removeNotification(reminder);
                    }
                }));
            panel.add(easyButton(3, _msgs.bookmarkLater(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        removeNotification(reminder);
                    }
                }));
            break;
        case PROMOTION:
            break;
        case TROPHY:
            panel.addStyleName("PublishTrophy");
            final TrophyData trophyData = (TrophyData)reminder.data;
            final Trophy trophy = trophyData.trophy;
            panel.add(createThumbnail(trophy.trophyMedia, "Thumbnail"));
            panel.add(MsoyUI.createLabel(
                _msgs.publishTrophyTip(trophy.name, trophyData.gameName), "Tip"));
            panel.add(easyButton(2, _msgs.publishTrophy(), new ClickHandler () {
                @Override public void onClick (ClickEvent event) {
                    CShell.frame.dispatchEvent(new TrophyEvent(trophy.gameId, trophyData.gameName,
                        trophyData.gameDesc, trophyData.gameMediaURL, trophy.name, trophy.ident,
                        trophy.description, trophy.trophyMedia.getMediaPath(), true));
                    removeNotification(reminder);
                }
            }));
            break;
        }
        return panel;
    }

    protected void removeNotification (Reminder reminder)
    {
        int index = _reminders.indexOf(reminder);
        if (index < 0) {
            return;
        }
        _reminders.remove(index);
        if (index < _selected) {
            _selected--;
        } else if (index == _selected) {
            if (_selected == _reminders.size()) {
                _selected = 0;
            }
            update();
        }
    }

    protected void clearBookmarkReminder ()
    {
        CookieUtil.set("/", 365, CookieNames.BOOKMARKED, "t", null);
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
        Widget w = MediaUtil.createMediaView(media, MediaDescSize.THUMBNAIL_SIZE);
        w.addStyleName(style);
        return w;
    }

    protected List<Reminder> _reminders = Lists.newArrayList();
    protected PushButton _cycle;
    protected SimplePanel _reminder;
    protected boolean _dismissed;
    protected int _selected;

    protected static final RemindersServiceAsync _svc = GWT.create(RemindersService.class);
    protected static final RemindersMessages _msgs = GWT.create(RemindersMessages.class);
}
