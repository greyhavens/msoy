//
// $Id$

package client.shell;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebCreds;

import client.util.MsoyUI;
import client.util.events.FlashEvents;
import client.util.events.FriendEvent;
import client.util.events.SceneBookmarkEvent;
import client.util.events.StatusChangeEvent;
import client.util.events.StatusChangeListener;

/**
 * Displays basic player status (name, flow count) and handles logging on and logging off.
 */
public class StatusPanel extends FlexTable
{
    public StatusPanel (Application app)
    {
        setStyleName("statusPanel");
        setCellPadding(0);
        setCellSpacing(0);
        _app = app;

        FlashEvents.addListener(new StatusChangeListener() {
            public void statusChanged (StatusChangeEvent event) {
                switch(event.getType()) {
                case StatusChangeEvent.LEVEL:
                    int newLevel = event.getValue();
                    int oldLevel = event.getOldValue();
                    _levels.setLevel(newLevel);
                    // a user's level is never 0, so 0 is used to indicate that this is the first
                    // update, and the new level popup should not be shown.
                    if (oldLevel != 0 && oldLevel != newLevel) {
                        _levels.showLevelUpPopup();
                    }
                    break;
                case StatusChangeEvent.FLOW:
                    _levels.setFlow(event.getValue());
                    break;
                case StatusChangeEvent.GOLD:
                    _levels.setGold(event.getValue());
                    break;
                case StatusChangeEvent.MAIL:
                    _mail.setCount(event.getValue());
                    break;
                }
            }
        });
    }

    /**
     * Called to forcibly set our unread mail count when FlashEvents aren't available.
     */
    public void notifyUnreadMailCount (int unread)
    {
        _mail.setCount(unread);
    }

    protected void didLogon (SessionData data)
    {
        _creds = data.creds;
        setCookie("creds", _creds.token);
        setCookie("who", _creds.accountName);

        // configure our levels
        int idx = 0;
        setText(0, idx++, _creds.name.toString());
        setWidget(0, idx++, _levels);
        FlashEvents.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.FLOW, data.flow, 0));
        FlashEvents.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.GOLD, data.gold, 0));
        FlashEvents.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.LEVEL, data.level, 0));

        // configure our 'new mail' indicator
        setWidget(0, idx++, _mail);
        FlashEvents.dispatchEvent(
            new StatusChangeEvent(StatusChangeEvent.MAIL, data.newMailCount, 0));

        // notify listeners of our friends and scenes
        for (int ii = 0, ll = data.friends.size(); ii < ll; ii++) {
            FriendEntry entry = (FriendEntry)data.friends.get(ii);
            FlashEvents.dispatchEvent(new FriendEvent(FriendEvent.FRIEND_ADDED, entry.name));
        }
        for (int ii = 0, ll = data.scenes.size(); ii < ll; ii++) {
            SceneBookmarkEntry entry = (SceneBookmarkEntry)data.scenes.get(ii);
            FlashEvents.dispatchEvent(new SceneBookmarkEvent(
                                          SceneBookmarkEvent.SCENEBOOKMARK_ADDED,
                                          entry.sceneName, entry.sceneId));
        }

        // add a logoff link
        setWidget(0, idx++, MsoyUI.createActionLabel(CShell.cmsgs.statusLogoff(), new ClickListener() {
            public void onClick (Widget sender) {
                CShell.app.didLogoff();
            }
        }));
    }

    protected void didLogoff ()
    {
        _creds = null;
        clearCookie("creds");
    }

    protected void setCookie (String name, String value)
    {
        CookieUtil.set("/", 7, name, value);
    }

    protected void clearCookie (String name)
    {
        CookieUtil.clear("/", name);
    }

    protected static class MailDisplay extends FlexTable
    {
        public MailDisplay () {
            setCellPadding(0);
            setCellSpacing(0);

            int idx = 0;
            getFlexCellFormatter().setStyleName(0, idx, "Icon");
            Image image = new Image("/images/header/symbol_mail.png");
            image.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.MAIL, "");
                }
            });
            setWidget(0, idx++, image);
            _mailIx = idx; // the next cell will hold our count
            setCount(0);
        }

        public void setCount (int count)
        {
            if (count > 0) {
                setText(0, _mailIx, String.valueOf(count));
                setVisible(true);
            } else {
                setVisible(false);
            }
        }

        protected int _mailIx;
    }

    protected static class LevelsDisplay extends FlexTable
    {
        public LevelsDisplay () {
            setCellPadding(0);
            setCellSpacing(0);

            int idx = 0;
            getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
            getFlexCellFormatter().setStyleName(0, idx, "Icon");
            setWidget(0, idx++, new Image("/images/header/symbol_flow.png"));
            setText(0, _flowIdx = idx++, "0");

            // TODO: display once we've implemented gold!
            /*getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
            getFlexCellFormatter().setStyleName(0, idx, "Icon");
            setWidget(0, idx++, new Image("/images/header/symbol_gold.png"));
            setText(0, _goldIdx = idx++, "0");*/

            getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
            getFlexCellFormatter().setStyleName(0, idx, "Icon");
            setWidget(0, idx++, new Image("/images/header/symbol_level.png"));
            setText(0, _levelIdx = idx++, "0");

            getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
            getFlexCellFormatter().setStyleName(0, idx, "Icon");
        }

        public void setLevel (int level) {
            setText(0, _levelIdx, String.valueOf(level));
        }

        public void setFlow (int flow) {
            setText(0, _flowIdx, String.valueOf(flow));
        }

        public void setGold (int gold) {
            //setText(0, _goldIdx, String.valueOf(gold));
        }

        public void showLevelUpPopup () {
            PopupPanel bling = new PopupPanel(true);
            bling.add(WidgetUtil.createTransparentFlashContainer("levelBling",
                "/media/static/levelbling.swf", 60, 60, null));
            Element cell = getFlexCellFormatter().getElement(0, _levelIdx);
            bling.setPopupPosition(DOM.getAbsoluteLeft(cell) - 30, DOM.getAbsoluteTop(cell) - 23);
            bling.show();
        }

        protected int _flowIdx, _goldIdx, _levelIdx;
    }

    protected Application _app;
    protected WebCreds _creds;

    protected LevelsDisplay _levels = new LevelsDisplay();
    protected MailDisplay _mail = new MailDisplay();

    /** The height of the header UI in pixels. */
    protected static final int HEADER_HEIGHT = 50;
}
