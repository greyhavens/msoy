//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.web.gwt.CookieNames;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebCreds;

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
 * TODO: this can possibly be a simpler widget since it no longer uses pieced background graphics
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
                _creds.name = new MemberName(event.getName(), _creds.name.getId());
                _namePanel.setWidget(Link.memberView(_creds.name));
            }
        });
    }

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        _creds = data.creds;
        CookieUtil.set("/", MAX_WHO_AGE, CookieNames.WHO, _creds.accountName, null);

        boolean permaguest = MemberMailUtil.isPermaguest(_creds.accountName);

        // mail, name, help, sign out in a box at top
        FlowPanel links = MsoyUI.createFlowPanel("Links");
        if (!permaguest) {
            links.add(_mail);
            links.add(MsoyUI.createLabel("|", "Spacer"));
            _namePanel.setWidget(Link.memberView(_creds.name));
            links.add(_namePanel);
            links.add(MsoyUI.createLabel("|", "Spacer"));
        }
        links.add(Link.create(_cmsgs.statusHelp(), Pages.HELP));
        links.add(MsoyUI.createLabel("|", "Spacer"));
        if (permaguest) {
            links.add(MsoyUI.createActionLabel(_cmsgs.statusLogon(),
                                               Link.createHandler(Pages.ACCOUNT, "logon")));
        } else {
            links.add(MsoyUI.createActionLabel(_cmsgs.statusLogoff(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    CShell.frame.logoff();
                }
            }));
        }

        // "sign up" or "invite friends" on top left
        Widget action;
        if (permaguest) {
            action = new PushButton(_cmsgs.headerSignup(), NaviUtil.onMustRegister());
            action.setStyleName("SignupButton");
            action.addStyleName("Button");
        } else {
            action = MsoyUI.createHTML("", null);
        }

        setWidget(0, 0, action);
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        getFlexCellFormatter().setRowSpan(0, 1, 2);

        SmartTable lbox = new SmartTable(0, 0);
        lbox.setText(0, 0, "");
        lbox.setWidget(0, 1, links, 1, "TopBoxLinks");
        lbox.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        lbox.setText(0, 2, "");
        setWidget(0, 2, lbox);
        getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_CENTER);

        // coins, bars, level on bottom
        setWidget(1, 0, _levels, 0);
        getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_RIGHT);
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

    protected static class MailDisplay extends FlowPanel
    {
        public MailDisplay () {
            addStyleName("Mail");
            ClickHandler handler = Link.createHandler(Pages.MAIL, "");
            add(_mailImage = MsoyUI.createActionImage("/images/header/symbol_mail.png", handler));
            add(_mailLabel = MsoyUI.createActionLabel("", handler));
            setCount(0);
        }

        public void setCount (int count) {
            // TODO: remove this max line after the -1 bug is really fixed.
            count = Math.max(0, count);
            _mailLabel.setText(String.valueOf(count));
            _mailLabel.setVisible(count > 0);
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
            coinsFocus.addClickHandler(NaviUtil.onViewTransactions(ReportType.COINS));
            setWidget(0, idx++, coinsFocus);

            FloatPanel bars = new FloatPanel("Bars");
            bars.add(new Image(Currency.BARS.getSmallIcon()));
            bars.add(_barsLabel = new Label("0"));
            FocusPanel barsFocus = new FocusPanel(bars);
            barsFocus.addClickHandler(NaviUtil.onViewTransactions(ReportType.BARS));
            setWidget(0, idx++, barsFocus);
            setWidget(0, idx++, Link.create(_cmsgs.statusBuyBars(), Pages.BILLING), 1, "BuyBars");

            FloatPanel level = new FloatPanel("Level");
            level.add(new Image("/images/header/symbol_level.png"));
            level.add(_levelLabel = new Label("0"));
            FocusPanel levelFocus = new FocusPanel(level);
            levelFocus.addClickHandler(Link.createHandler(Pages.ME, "passport"));
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
    protected static final int MAX_WHO_AGE = 365; // days
}
