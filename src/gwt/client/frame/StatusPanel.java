//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebCreds;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.ReportType;

import client.shell.CShell;
import client.shell.Session;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.NaviUtil;
import client.util.events.FlashEvents;
import client.util.events.NameChangeEvent;
import client.util.events.NameChangeListener;
import client.util.events.StatusChangeEvent;
import client.util.events.StatusChangeListener;

/**
 * Displays basic player status (name, flow count) and handles logging on and logging off.
 */
public class StatusPanel extends SmartTable
    implements Session.Observer
{
    public StatusPanel ()
    {
        super("statusPanel", 0, 0);

        Session.addObserver(this);

        FlashEvents.addListener(new StatusChangeListener() {
            public void statusChanged (StatusChangeEvent event) {
                switch(event.getType()) {
                case StatusChangeEvent.LEVEL:
                    _levels.setLevel(event.getValue());
                    // if our level changed, display some fancy graphics
                    if (isIncrease(event)) {
                        _levels.showLevelUpPopup();
                    }
                    break;

                case StatusChangeEvent.COINS:
                    _levels.setCoins(event.getValue());
                    // if we earned flow, display some fancy graphics
                    if (isIncrease(event)) {
                        _levels.showEarnedCoinsPopup();
                    }
                    break;

                case StatusChangeEvent.BARS:
                    _levels.setBars(event.getValue());
                    break;

                case StatusChangeEvent.MAIL:
                    _mail.setCount(event.getValue());
                    break;
                }
            }
        });

        FlashEvents.addListener(new NameChangeListener() {
            public void nameChanged (NameChangeEvent event) {
                _creds.name = new MemberName(event.getName(), _creds.name.getMemberId());
                _namePanel.setWidget(Link.memberView(_creds.name));
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

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        _creds = data.creds;
        CookieUtil.set("/", Session.SESSION_DAYS, "who", _creds.accountName);

        boolean permaguest = MemberName.isPermaguest(_creds.accountName);

        // mail, name, help, sign out in a box at top
        HorizontalPanel links = new HorizontalPanel();
        if (!permaguest) {
            links.add(_mail);
            CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.MAIL,
                data.newMailCount, 0));
            links.add(MsoyUI.createLabel("|", "Spacer"));
            _namePanel.setWidget(Link.memberView(_creds.name));
            links.add(_namePanel);
            links.add(MsoyUI.createLabel("|", "Spacer"));
        }
        links.add(Link.create(_cmsgs.statusHelp(), Pages.HELP, null));
        links.add(MsoyUI.createLabel("|", "Spacer"));
        if (permaguest) {
            links.add(MsoyUI.createActionLabel(_cmsgs.statusSaveGuest(), new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Pages.ACCOUNT, "create");
                }
            }));
        } else {
            links.add(MsoyUI.createActionLabel(_cmsgs.statusLogoff(), new ClickListener() {
                public void onClick (Widget sender) {
                    Session.didLogoff(Session.LogoffCondition.LOGOFF_REQUESTED);
                }
            }));
        }

        // white top box aligned to right of window
        HorizontalPanel topBox = new HorizontalPanel();

        // friends = coins blurb on top left
        topBox.add(MsoyUI.createImageButton("InviteFriends", Link.createListener(Pages.PEOPLE,
            "invites")));
        topBox.add(WidgetUtil.makeShim(10, 10));

        topBox.add(new Image("/images/header/status_bg_left.png"));
        topBox.add(MsoyUI.createSimplePanel(links, "TopBoxLinks"));
        topBox.add(new Image("/images/header/status_bg_right.png"));
        setWidget(0, 0, topBox);
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_RIGHT);
        getFlexCellFormatter().setColSpan(0, 0, 3);

        // coins, bars, level on bottom left
        setWidget(1, 0, _levels);
        CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.COINS, data.flow, 0));
        CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.BARS, data.gold, 0));
        CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.LEVEL, data.level, 0));
    }

    // from interface Session.Observer
    public void didLogoff ()
    {
        _creds = null;
    }

    protected static boolean isIncrease (StatusChangeEvent event)
    {
        int oldLevel = event.getOldValue();
        return (oldLevel != 0 && oldLevel < event.getValue());
    }

    protected static class MailDisplay extends HorizontalPanel
    {
        public MailDisplay () {
            addStyleName("Mail");
            add(_mailImage = MsoyUI.createActionImage("/images/header/symbol_mail.png",
                Link.createListener(Pages.MAIL, "")));
            add(_mailLabel = MsoyUI.createActionLabel("(0)", Link.createListener(Pages.MAIL, "")));
            setCount(0);
        }

        public void setCount (int count) {
            _mailLabel.setText("(" + String.valueOf(count) + ")");
            _mailImage.setTitle(count > 0 ? _cmsgs.newMailTip() : _cmsgs.mailTip());
        }

        protected Image _mailImage;
        protected Label _mailLabel;
    }

    protected static class LevelsDisplay extends SmartTable
    {
        public LevelsDisplay () {
            super("Levels", 0, 0);

            int idx = 0;
            FloatPanel coins = new FloatPanel("Coins");
            coins.add(new Image(Currency.COINS.getSmallIcon()));
            coins.add(_coinsLabel = new Label("0"));
            FocusPanel coinsFocus = new FocusPanel(coins);
            coinsFocus.addClickListener(NaviUtil.onViewTransactions(ReportType.COINS));
            setWidget(0, idx++, coinsFocus);

            FloatPanel bars = new FloatPanel("Bars");
            bars.add(new Image(Currency.BARS.getSmallIcon()));
            bars.add(_barsLabel = new Label("0"));
            FocusPanel barsFocus = new FocusPanel(bars);
            barsFocus.addClickListener(NaviUtil.onViewTransactions(ReportType.BARS));
            setWidget(0, idx++, barsFocus);
            setWidget(0, idx++, MsoyUI.createActionLabel(_cmsgs.statusBuyBars(), "BuyBars",
                NaviUtil.onBuyBars()));

            FloatPanel level = new FloatPanel("Level");
            level.add(new Image("/images/header/symbol_level.png"));
            level.add(_levelLabel = new Label("0"));
            FocusPanel levelFocus = new FocusPanel(level);
            levelFocus.addClickListener(Link.createListener(Pages.ME, "passport"));
            setWidget(0, idx++, levelFocus);

            getFlexCellFormatter().setWidth(0, idx++, "12px"); // gap!
        }

        public void setCoins (int coins) {
            _coinsLabel.setText(Currency.COINS.format(coins));
        }

        public void setBars (int bars) {
            _barsLabel.setText(Currency.BARS.format(bars));
        }

        public void setLevel (int level) {
            _levelLabel.setText(String.valueOf(level));
        }

        public void showEarnedCoinsPopup () {
            _coinPopup = showPopup("/media/static/levelbling.swf", _coinsLabel, _coinPopup);
        }

        public void showLevelUpPopup () {
            _levelPopup = showPopup("/media/static/levelbling.swf", _levelLabel, _levelPopup);
        }

        protected PopupPanel showPopup (String path, Widget underWidget, PopupPanel previous) {
            if (previous != null) {
                previous.hide();
            }
            PopupPanel bling = new PopupPanel(true);
            bling.add(WidgetUtil.createTransparentFlashContainer("levelBling", path, 60, 60, null));
            Element elem = underWidget.getElement();
            bling.setPopupPosition(DOM.getAbsoluteLeft(elem) - 30, DOM.getAbsoluteTop(elem) - 23);
            bling.show();
            return bling;
        }

        protected Label _coinsLabel;
        protected Label _barsLabel;
        protected Label _levelLabel;
        protected PopupPanel _coinPopup;
        protected PopupPanel _levelPopup;
    }

    protected WebCreds _creds;

    protected LevelsDisplay _levels = new LevelsDisplay();
    protected MailDisplay _mail = new MailDisplay();
    protected SimplePanel _namePanel = MsoyUI.createSimplePanel(null, "Name");

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
