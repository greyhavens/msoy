//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.WidgetUtil;

import client.ui.MsoyUI;
import client.ui.RoundBox;
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

        FlowPanel joinButton = MsoyUI.createFlowPanel("JoinButton", MsoyUI.createActionImage(
            "/images/landing/blue_landing_join_now.png", NaviUtil.onSignUp()));

        RoundBox about = new RoundBox(RoundBox.WHITE);
        about.add(MsoyUI.createLabel(_msgs.contestOctAboutTitle(), "Title"));
        about.add(MsoyUI.createHTML(_msgs.contestOctAboutText(), null));

        RoundBox prizes = new RoundBox(RoundBox.WHITE);
        prizes.add(MsoyUI.createLabel(_msgs.contestOctPrizesTitle(), "TitleAlt"));
        prizes.add(MsoyUI.createHTML(_msgs.contestOctPrizesText(), null));

        RoundBox enter = new RoundBox(RoundBox.WHITE);
        enter.add(MsoyUI.createLabel(_msgs.contestOctEnterTitle(), "Title"));
        enter.add(MsoyUI.createHTML(_msgs.contestOctEnterText(), null));
        enter.add(MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.contestOctSignup(),
            NaviUtil.onSignUp()));

        RoundBox rules = new RoundBox(RoundBox.WHITE);
        rules.add(MsoyUI.createLabel(_msgs.contestOctRulesTitle(), "TitleAlt"));
        rules.add(MsoyUI.createHTML(_msgs.contestOctRulesText(), null));

        add(MsoyUI.createFlowPanel("Content", joinButton, about, prizes, enter, rules));

        add(WidgetUtil.makeShim(20, 20));
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
