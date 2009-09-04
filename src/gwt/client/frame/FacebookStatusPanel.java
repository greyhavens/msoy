//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.msoy.web.gwt.SessionData;

import client.shell.CShell;
import client.shell.Session;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.events.FlashEvents;
import client.util.events.StatusChangeEvent;
import client.util.events.StatusChangeListener;

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

        Session.addObserver(new Session.Observer() {
            @Override public void didLogon (SessionData data) {
                updateSession(data);
            }
            @Override public void didLogoff () {
            }
        });

        FlashEvents.addListener(new StatusChangeListener() {
            public void statusChanged (StatusChangeEvent event) {
                switch(event.getType()) {
                case StatusChangeEvent.LEVEL:
                    // this is a bit hacky, but we need to know whether the didLogon handler
                    // in StatusPanel is the sender. It always uses 0 as the old value. If we don't
                    // perform this check we go into an infinite loop, continuously validating the
                    // session
                    // TODO: better alternative, perhaps just request the SessionData without
                    // dispatching a didLogon... or maybe include the information with a new
                    // LevelChangedEvent
                    if (event.getOldValue() != 0) {
                        // revalidate since we need the last and next coin values
                        //CShell.log("Level change from flash");
                        //Session.validate();
                    }
                    break;

                case StatusChangeEvent.COINS:
                    _levelProgressBar.setCurrent(event.getValue());
                    break;
                }
            }
        });
    }

    protected void updateSession (SessionData data)
    {
        _name.setText(data.creds.name.toString());
        _level.setText(_msgs.fbstatusLevel(String.valueOf(data.level)));
        if (data.extra != null) {
            _trophies.setText("" + data.extra.trophyCount);
            _levelProgressBar.set(
                data.extra.levelFlow, data.extra.nextLevelFlow, data.extra.accumFlow);
        } else {
            _trophies.setText("");
            _levelProgressBar.setVisible(false);
        }
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

        public void setCurrent (int current)
        {
            _current = current;
            update();
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

    protected Label _name, _level, _trophies;
    protected ProgressBar _levelProgressBar;
    protected static final ShellMessages _msgs = GWT.create(ShellMessages.class); 
}
