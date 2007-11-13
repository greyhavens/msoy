//
// $Id$

package client.shell;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

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
import com.threerings.msoy.web.client.DeploymentConfig;
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
                    _levels.setVisible(true);
                    break;
                case StatusChangeEvent.FLOW:
                    _levels.setFlow(event.getValue());
                    _levels.setVisible(true);
                    break;
                case StatusChangeEvent.GOLD:
                    _levels.setGold(event.getValue());
                    _levels.setVisible(true);
                    break;
                case StatusChangeEvent.MAIL:
                    _mail.setCount(event.getValue());
                    break;
                }
            }
        });
    }

    /**
     * Called once the rest of our application is set up. Checks to see if we're already logged on,
     * in which case it triggers a call to didLogon().
     */
    public void init ()
    {
        validateSession(CookieUtil.get("creds"));
    }

    /**
     * Returns our credentials or null if we are not logged in.
     */
    public WebCreds getCredentials ()
    {
        return _creds;
    }

    /**
     * Clears out our credentials and displays the logon interface.
     */
    public void logoff ()
    {
        _creds = null;
        clearCookie("creds");
        _app.didLogoff();

        // hide our logged on bits
        _levels.setVisible(false);
        _mail.setVisible(false);

        setText(0, 0, "New to Whirled?");
        setHTML(0, 1, "&nbsp;");
        setWidget(0, 2, MsoyUI.createActionLabel("Create an account!", new ClickListener() {
            public void onClick (Widget sender) {
                new CreateAccountDialog(StatusPanel.this, null).show();
            }
        }));
    }

    protected void validateSession (String token)
    {
        if (token != null) {
            // validate our session before considering ourselves logged on
            CShell.usersvc.validateSession(DeploymentConfig.version, token, 1, new AsyncCallback() {
                public void onSuccess (Object result) {
                    if (result == null) {
                        logoff();
                    } else {
                        didLogon((SessionData)result);
                    }
                }
                public void onFailure (Throwable t) {
                    logoff();
                }
            });

        } else {
            logoff();
        }
    }

    protected void didLogon (SessionData data)
    {
        _creds = data.creds;
        setCookie("creds", _creds.token);
        setCookie("who", _creds.accountName);
        _app.didLogon(_creds);

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

            String mailImg = "<img class='MailNotification' src='/images/mail/button_mail.png'/>";
            setWidget(0, idx++, new HTML(Application.createLinkHtml(mailImg, "mail", "")));

            setText(0, _mailIx = idx++, "0");
            setVisible(false);
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
