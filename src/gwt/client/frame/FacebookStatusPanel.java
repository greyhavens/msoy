//
// $Id$

package client.frame;

import java.util.List;

import com.google.common.collect.Lists;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.web.gwt.SessionData;

import client.shell.CShell;
import client.shell.Session;
import client.shell.ShellMessages;
import client.ui.BorderedDialog;
import client.ui.MsoyUI;

import client.util.events.FlashEventListener;
import client.util.events.FlashEvents;
import client.util.events.StatusChangeEvent;
import client.util.events.StatusChangeListener;
import client.util.events.TrophyEvent;

/**
 * Status panel for the Facebook application. Shows name, current level, number of trophies and
 * maybe at some point the daily bonus progress.
 */
public class FacebookStatusPanel extends AbsoluteCSSPanel
{
    public FacebookStatusPanel ()
    {
        super("fbstatus");
        add(MsoyUI.createAbsoluteCSSPanel("Top",
            _name = MsoyUI.createLabel("", "Name"),
            _level = MsoyUI.createLabel("", "Level"),
            MsoyUI.createFlowPanel("TrophyIcon"),
            _trophies = MsoyUI.createLabel("", "Trophies")));
        add(_levelProgressBar = new ProgressBar("LevelProgress", _msgs.fbStatusNextLevel()));

        Session.addObserver(_observer = new Session.Observer() {
            @Override public void didLogon (final SessionData data) {
                setData(new Data(data));

                // do some hot popup action, but later
                final SessionData.Extra extra = data.extra;
                if (extra != null && (extra.levelsGained != 0 || extra.flowAwarded != 0)) {
                    new Timer() {
                        @Override public void run () {
                            popupDailyVisit(extra.levelsGained, extra.flowAwarded);
                        }
                    }.schedule(250);
                }
            }
            @Override public void didLogoff () {
            }
        });

        _listeners.add(new StatusChangeListener() {
            public void statusChanged (StatusChangeEvent event) {
                // we don't care about events that are just setting the value
                if (event.isInitializing() || _data == null) {
                    return;
                }
                switch(event.getType()) {
                case StatusChangeEvent.LEVEL:
                    _data.level = event.getValue();

                    // approximate the distance to the next level, not 100% accurate but this is
                    // not going to be a very common event
                    // TODO: update these fields properly
                    _data.lastCoins = _data.currCoins = _data.nextCoins;
                    _data.nextCoins += ((_data.level + 1) * 17.8 - 49) * 50;
                    update();
                    break;

                case StatusChangeEvent.COINS:
                    _data.currCoins += event.getValue() - event.getOldValue();
                    update();
                    break;
                }
            }
        });

        _listeners.add(new TrophyEvent.Listener() {
            @Override public void trophyEarned (TrophyEvent event) {
                if (_data != null) {
                    _data.trophies++;
                    update();
                }
            }
        });

        for (FlashEventListener listener : _listeners) {
            FlashEvents.addListener(listener);
        }

        if (_data != null) {
            update();
        }
    }

    @Override
    protected void onUnload ()
    {
        super.onUnload();
        for (FlashEventListener listener : _listeners) {
            FlashEvents.removeListener(listener);
        }
        if (_observer != null) {
            Session.removeObserver(_observer);
        }
        _listeners.clear();
        _observer = null;
    }

    protected void setData (Data data)
    {
        _data = data;
        update();
    }

    protected void update ()
    {
        _name.setText(_data.name);
        _level.setText(_msgs.fbstatusLevel(String.valueOf(_data.level)));
        _trophies.setText("" + _data.trophies);
        if (_data.nextCoins != 0) {
            _levelProgressBar.set(_data.lastCoins, _data.nextCoins, _data.currCoins);
        } else {
            _levelProgressBar.setVisible(false);
        }
    }

    protected void popupDailyVisit (int levelsGained, int flowAwarded)
    {
        BorderedDialog popup = new BorderedDialog() {};
        if (levelsGained == 0 && _data.nextCoins > _data.currCoins) {
            popup.setHeaderTitle(_msgs.fbStatusCoinAwardPopupTitle());
            String text = _msgs.fbStatusCoinAwardPopupText(String.valueOf(flowAwarded),
                String.valueOf(_data.nextCoins - _data.currCoins)).replace("\n", "<br/>");
            CShell.log("Text", "value", text);
            popup.setContents(MsoyUI.createHTML(text, "Text"));
            popup.addButton(new Button(_msgs.fbStatusCoinAwardPopupBtn(), popup.onAction(null)));

        } else if (levelsGained > 0) {
            popup.setHeaderTitle(_msgs.fbStatusLevelPopupTitle());
            popup.setContents(MsoyUI.createLabel(_msgs.fbStatusLevelPopupText(
                String.valueOf(_data.level)), "Content"));
            popup.addButton(new Button(_msgs.fbStatusLevelPopupBtn(), popup.onAction(
                new Command() {
                @Override public void execute () {
                    LevelUpFeeder.publishLevelup(_data.level);
                }
            })));
        } else {
            return;
        }

        popup.show();
    }

    protected static class ProgressBar extends AbsoluteCSSPanel
    {
        public ProgressBar (String style, String label)
        {
            super("fbprogressBar");
            addStyleName(style);
            add(_meter = MsoyUI.createFlowPanel("Meter"));
            add(MsoyUI.createLabel(label, "Label"));
            add(_detail = MsoyUI.createLabel("", "Detail"));
        }

        public void set (int min, int max, int current)
        {
            _min = min;
            _max = max;
            _current = current;
            update();
        }

        protected void update ()
        {
            int range = _max - _min;
            int progress = _current - _min;
            float percent = (float)progress / range * 100;
            _detail.setText(_msgs.fbStatusProgress(""+progress, ""+range));
            DOM.setStyleAttribute(_meter.getElement(), "width", (int)percent + "%");
        }

        protected Label _detail;
        protected FlowPanel _meter;
        protected int _min, _max, _current;
    }

    protected class Data
    {
        public String name;
        public int level;
        public int trophies;
        public int lastCoins;
        public int nextCoins;
        public int currCoins;

        public Data (SessionData sessionData)
        {
            name = sessionData.creds.name.toString();
            level = sessionData.level;
            if (sessionData.extra != null) {
                trophies = sessionData.extra.trophyCount;
                lastCoins = sessionData.extra.levelFlow;
                nextCoins = sessionData.extra.nextLevelFlow;
                currCoins = sessionData.extra.accumFlow;
            }
        }
    }

    protected Label _name, _level, _trophies;
    protected ProgressBar _levelProgressBar;
    protected Session.Observer _observer;
    protected List<FlashEventListener> _listeners = Lists.newArrayList();
    // bah, we have to use a static here because the title bar keeps getting re-created but
    // didLogon is only called the first time
    // TODO: some day sort this out 
    protected static Data _data;
    protected static final ShellMessages _msgs = GWT.create(ShellMessages.class); 
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
