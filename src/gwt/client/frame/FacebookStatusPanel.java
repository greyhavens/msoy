//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.msoy.web.gwt.SessionData;

import client.shell.Session;
import client.shell.ShellMessages;
import client.ui.MsoyUI;

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
    }

    protected void updateSession (SessionData data)
    {
        _name.setText(data.creds.name.toString());
        _level.setText(_msgs.fbstatusLevel(String.valueOf(data.level)));
        if (data.extra != null) {
            _trophies.setText("" + data.extra.trophyCount);
            int current = data.extra.accumFlow - data.extra.levelFlow;
            int total = data.extra.nextLevelFlow - data.extra.levelFlow;
            _levelProgressBar.set(current, total);
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

        public void set (int current, int total)
        {
            float percent = (float)current / total * 100;
            _detail.setText(_msgs.fbStatusProgress(""+current, ""+total));
            DOM.setStyleAttribute(_meter.getElement(), "width", (int)percent + "%");
        }

        protected Label _detail;
        protected FlowPanel _meter;
    }

    protected Label _name, _level, _trophies;
    protected ProgressBar _levelProgressBar;
    protected static final ShellMessages _msgs = GWT.create(ShellMessages.class); 
}
