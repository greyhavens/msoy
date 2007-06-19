//
// $Id$

package client.shell;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.util.MsoyUI;
import client.util.events.FlashEvents;
import client.util.events.LevelUpdateEvent;
import client.util.events.LevelsListener;

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

        // create our mail notifier for use later
        String mailImg = "<img class='MailNotification' src='/images/mail/button_mail.png'/>";
        _mailNotifier = new HTML(Application.createLinkHtml(mailImg, "mail", ""));
        _mailNotifier.setWidth("20px");

        FlashEvents.addListener(new LevelsListener() {
            public void levelUpdated (LevelUpdateEvent event) {
                switch(event.getType()) {
                case LevelUpdateEvent.LEVEL:
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
                case LevelUpdateEvent.FLOW:
                    _levels.setFlow(event.getValue());
                    _levels.setVisible(true);
                    break;
                case LevelUpdateEvent.GOLD:
                    _levels.setGold(event.getValue());
                    _levels.setVisible(true);
                    break;
                case LevelUpdateEvent.MAIL:
                    _mailNotifier.setVisible(event.getValue() > 0);
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
     * Requests that we display our logon popup.
     */
    public void showLogonPopup ()
    {
        showLogonPopup(-1, -1);
    }

    /**
     * Requests that we display our logon popup at the specified position.
     */
    public void showLogonPopup (int px, int py)
    {
        LogonPopup popup = new LogonPopup(this);
        popup.show();
        popup.setPopupPosition(px == -1 ? (Window.getClientWidth() - popup.getOffsetWidth()) : px,
                               py == -1 ? HEADER_HEIGHT : py);
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
        _mailNotifier.setVisible(false);

//         if (DeploymentConfig.devDeployment) {
            setText(0, 0, "New to Whirled?");
            setHTML(0, 1, "&nbsp;");
            setWidget(0, 2, MsoyUI.createActionLabel("Create an account!", new ClickListener() {
                public void onClick (Widget sender) {
                    new CreateAccountDialog(StatusPanel.this, null).show();
                }
            }));
//         } else {
//             setText(0, 0, "Welcome to the First Whirled!");
//         }
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
                        _creds = (WebCreds)result;
                        didLogon(_creds);
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

    protected void didLogon (WebCreds creds)
    {
        _creds = creds;
        setCookie("creds", _creds.token);
        setCookie("who", _creds.accountName);
        _app.didLogon(_creds);

        int idx = 0;
        setText(0, idx++, _creds.name.toString());
        setWidget(0, idx++, _levels);
        _levels.setVisible(false); // we'll soon have a call to refreshLevels()

        // begin with 'new mail' turned off until we hear otherwise
        setWidget(0, idx++, _mailNotifier);
        _mailNotifier.setVisible(false);
    }

    protected void actionClicked ()
    {
        if (_creds == null) {
            showLogonPopup();
        } else {
            logoff();
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

            getFlexCellFormatter().setWidth(0, idx++, "25px"); // gap!
            getFlexCellFormatter().setStyleName(0, idx, "Icon");
            setWidget(0, idx++, new Image("/images/header/symbol_gold.png"));
            setText(0, _goldIdx = idx++, "0");

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
            setText(0, _goldIdx, String.valueOf(gold));
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
    protected HTML _mailNotifier;

    /** The height of the header UI in pixels. */
    protected static final int HEADER_HEIGHT = 50;
}
