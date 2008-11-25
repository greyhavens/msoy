//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Landing page for the Flash Game Developer Challenge. This is a full-width page.
 */
public class GameContestPanel extends FlowPanel
{
    public GameContestPanel ()
    {
        setStyleName("gameContestPanel");
        FlowPanel content = MsoyUI.createFlowPanel("Content");
        add(content);

        // big bg image with a couple positioned buttons
        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");
        content.add(header);
        header.add(MsoyUI.createActionImage("/images/landing/contests/gamecon_whirled_logo.png",
            Link.createListener(Pages.LANDING, "")), 20, 10);
        header.add(MsoyUI.createActionImage("/images/landing/contests/gamecon_join_now.png",
            Link.createListener(Pages.ACCOUNT, "create")), 750, 0);

        content.add(new WideContentBox(
            _msgs.gameconOverviewTitle(), _msgs.gameconOverviewText(), false));
        content.add(new WideContentBox(
            _msgs.gameconPrizesTitle(), _msgs.gameconPrizesText(), true));
        content.add(new WideContentBox(
            _msgs.gameconDatesTitle(), _msgs.gameconDatesText(), false));
        content.add(new WideContentBox(
            _msgs.gameconConditionsTitle(), _msgs.gameconConditionsText(), true));
        content.add(new WideContentBox(
            _msgs.gameconEnterTitle(), _msgs.gameconEnterText(), false));
        content.add(new WideContentBox(
            _msgs.gameconJudgingTitle(), _msgs.gameconJudgingText(), true));
        content.add(new WideContentBox(
            _msgs.gameconRulesTitle(), _msgs.gameconRulesText(), false));

        content.add(WidgetUtil.makeShim(10, 10));

        AbsolutePanel footerContent = MsoyUI.createAbsolutePanel("FooterContent");
        SimplePanel footer = MsoyUI.createSimplePanel(footerContent, "Footer");
        add(footer);
        footerContent.add(MsoyUI.createActionImage(
            "/images/landing/contests/gamecon_three_rings_logo.png",
            Link.createListener(Pages.LANDING, "")), 20, 5);
        footerContent.add(MsoyUI.createHTML(_msgs.gameconFooterText(), "FooterText"), 0, 15);
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
