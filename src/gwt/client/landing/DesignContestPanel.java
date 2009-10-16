//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.Link;
import client.util.NaviUtil;

/**
 * Landing page for the design your whirled room design contest.
 */
public class DesignContestPanel extends FlowPanel
{
    public DesignContestPanel ()
    {
        setStyleName("designContestPanel");
        addStyleName("BlueLandingPage");

        RoundBox about = new RoundBox(RoundBox.WHITE);
        about.add(MsoyUI.createLabel(_msgs.contestOctAboutTitle(), "Title"));
        about.add(MsoyUI.createHTML(_msgs.contestOctAboutText(), null));

        RoundBox prizes = new RoundBox(RoundBox.WHITE);
        prizes.add(MsoyUI.createLabel(_msgs.contestOctPrizesTitle(), "TitleAlt"));
        prizes.add(MsoyUI.createHTML(_msgs.contestOctPrizesText(), null));

        RoundBox enter = new RoundBox(RoundBox.WHITE);
        enter.add(MsoyUI.createLabel(_msgs.contestOctEnterTitle(), "Title"));
        enter.add(MsoyUI.createHTML(_msgs.contestOctEnterText(), null));

        RoundBox rules = new RoundBox(RoundBox.WHITE);
        rules.add(MsoyUI.createLabel(_msgs.contestOctRulesTitle(), "TitleAlt"));
        rules.add(MsoyUI.createHTML(_msgs.contestOctRulesText(), null));

        add(MsoyUI.createFlowPanel("Content", about, prizes, enter, rules));

        add(WidgetUtil.makeShim(20, 20));
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
