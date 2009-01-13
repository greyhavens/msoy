//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlinePanel;
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
        super("statusPanel", 0, 3);

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
                setWidget(0, 0, Link.memberView(_creds.name));
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

        // name at the top left
        SmartTable top = new SmartTable(0, 0);
        top.setWidth("100%");
        top.setWidget(0, 0, Link.memberView(_creds.name));

        // logoff, help at the top right
        InlinePanel links = new InlinePanel(null);
        links.add(MsoyUI.createActionLabel(_cmsgs.statusLogoff(), new ClickListener() {
            public void onClick (Widget sender) {
                Session.didLogoff(Session.LogoffCondition.LOGOFF_REQUESTED);
            }
        }));
        links.add(new HTML("|"));
        links.add(Link.create(_cmsgs.statusHelp(), Pages.HELP, null));
        top.setWidget(0, 1, links);
        top.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);

        setWidget(0, 0, top);
        getFlexCellFormatter().setColSpan(0, 0, 2);

        // coins, bars, level on bottom
        int idx = 0;
        setWidget(1, idx++, _levels);
        CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.COINS, data.flow, 0));
        CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.BARS, data.gold, 0));
        CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.LEVEL, data.level, 0));

        // configure our 'new mail' indicator on bottom
        setWidget(1, idx++, _mail);
        CShell.frame.dispatchEvent(
            new StatusChangeEvent(StatusChangeEvent.MAIL, data.newMailCount, 0));
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

    protected static class MailDisplay extends SmartTable
    {
        public MailDisplay () {
            super("Mail", 0, 0);

            int idx = 0;
            setWidget(0, idx++, MsoyUI.createActionImage(
                          "/images/header/symbol_mail.png",
                          Link.createListener(Pages.MAIL, "")), 1, "Icon");
            _mailIx = idx; // the next cell will hold our count
            setCount(0);
        }

        public void setCount (int count)
        {
            setText(0, _mailIx, String.valueOf(count));
            setVisible(true);
            getWidget(0, _mailIx-1).setTitle(count > 0 ? _cmsgs.newMailTip() : _cmsgs.mailTip());
        }

        protected int _mailIx;
    }

    protected static class LevelsDisplay extends SmartTable
    {
        public LevelsDisplay () {
            super(0, 0);

            int idx = 0;
            getFlexCellFormatter().setWidth(0, idx++, "15px"); // gap!
            ClickListener onClick = NaviUtil.onViewTransactions(ReportType.COINS);
            setWidget(0, idx++, MsoyUI.createActionImage(Currency.COINS.getLargeIcon(),
                _cmsgs.coinsTip(), onClick), 1, "Icon");
            setText(0, _coinsIdx = idx++, "0");

            getFlexCellFormatter().setWidth(0, idx++, "15px"); // gap!
            setWidget(0, idx++, MsoyUI.createActionImage(Currency.BARS.getLargeIcon(),
                _cmsgs.barsTip(), NaviUtil.onViewTransactions(ReportType.BARS)), 1, "Icon");
            setText(0, _barsIdx = idx++, "0");

            getFlexCellFormatter().setWidth(0, idx++, "15px"); // gap!
            setWidget(0, idx++, MsoyUI.createActionImage("/images/header/symbol_level.png",
                _cmsgs.levelTip(), Link.createListener(Pages.ME, "passport")), 1, "Icon");
            setText(0, _levelIdx = idx++, "0");

            getFlexCellFormatter().setWidth(0, idx++, "12px"); // gap!
        }

        public void setLevel (int level) {
            setText(0, _levelIdx, String.valueOf(level));
        }

        public void setCoins (int coins) {
            setText(0, _coinsIdx, Currency.COINS.format(coins));
        }

        public void setBars (int bars) {
            setText(0, _barsIdx, Currency.BARS.format(bars));
        }

        public void showLevelUpPopup () {
            showPopup("/media/static/levelbling.swf", _levelIdx);
        }

        public void showEarnedCoinsPopup () {
            showPopup("/media/static/levelbling.swf", _coinsIdx);
        }

        protected void showPopup (String path, int idx) {
            PopupPanel bling = new PopupPanel(true);
            bling.add(WidgetUtil.createTransparentFlashContainer("levelBling", path, 60, 60, null));
            Element cell = getFlexCellFormatter().getElement(0, idx);
            bling.setPopupPosition(DOM.getAbsoluteLeft(cell) - 30, DOM.getAbsoluteTop(cell) - 23);
            bling.show();
        }

        protected int _coinsIdx, _barsIdx, _levelIdx;
    }

    protected WebCreds _creds;

    protected LevelsDisplay _levels = new LevelsDisplay();
    protected MailDisplay _mail = new MailDisplay();

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
