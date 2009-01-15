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
 * Landing page with an introduction to Whirled aimed at game and content developers.
 */
public class DeveloperIntroPanel extends FlowPanel
{
    public DeveloperIntroPanel ()
    {
        setStyleName("developerIntroPanel");
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

        content.add(new WideContentBox(_msgs.devintroWhatTitle(), _msgs.devintroWhatText(), 
            false));
        content.add(new WideContentBox(_msgs.devintroRevenueTitle(), _msgs.devintroRevenueText(),
            true));
        content.add(new WideContentBox(_msgs.devintroExamplesTitle(),
            _msgs.devintroExamplesText(), false));
        content.add(new WideContentBox(_msgs.devintroStartedTitle(), _msgs.devintroStartedText(),
            true));

        // footer stretches full width, contains copyright info
        add(MsoyUI.createSimplePanel(new LandingCopyright(), "Footer"));
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
