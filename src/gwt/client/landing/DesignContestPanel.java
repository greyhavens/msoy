//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Landing page for the design your whirled room design contest.
 */
public class DesignContestPanel extends FlowPanel
{
    public DesignContestPanel ()
    {
        setStyleName("designContestPanel");
        addStyleName("BlueLandingPage");
        FlowPanel content = MsoyUI.createFlowPanel("Content");
        add(content);

        // big bg image with a couple positioned buttons
        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");
        content.add(header);
        header.add(MsoyUI.createActionImage("/images/landing/blue_landing_whirled_logo.png",
            Link.createListener(Pages.LANDING, "")), 20, 10);
        header.add(MsoyUI.createActionImage("/images/landing/blue_landing_join_now.png",
            Link.createListener(Pages.ACCOUNT, "create")), 750, 0);

        // instructions etc in white boxes
        content.add(
            new WideContentBox(_msgs.designconIntroTitle(), _msgs.designconIntroText(), false));
        content.add(
            new WideContentBox(_msgs.designconPrizesTitle(), _msgs.designconPrizesText(), true));
        content.add(
            new WideContentBox(_msgs.designconEnterTitle(), _msgs.designconEnterText(), true));
        content.add(
            new WideContentBox(_msgs.designconRulesTitle(), _msgs.designconRulesText(), false));

        // footer stretches full width, contains copyright info
        add(MsoyUI.createSimplePanel(new LandingCopyright(), "Footer"));
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
