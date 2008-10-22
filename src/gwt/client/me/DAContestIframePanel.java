//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Similar to client.landing.DAContestPanel, but this contest information is desiged to be viewed
 * from within an iframe by a (probably) registered user. Images stored under /images/landing/ and
 * some are shared with client.landing.DAContestPanel.
 */
public class DAContestIframePanel extends AbsolutePanel
{
    public DAContestIframePanel ()
    {
        setStyleName("daContestIframePanel");

        // design your whirled header
        add(new Image("/images/landing/dacontest_header_iframe.png"));

        // how to enter and step panels are large images that include text
        AbsolutePanel howToEnter = MsoyUI.createAbsolutePanel("HowToEnter");
        add(howToEnter);
        howToEnter.add(MsoyUI.createButton(MsoyUI.LONG_THICK, "Join Whirled!",
            Link.createListener(Pages.ACCOUNT, "create")), 455, 345);

        AbsolutePanel step1 = MsoyUI.createAbsolutePanel("Step1");
        add(step1);
        step1.add(createInvisiLink(Link.createListener(Pages.SHOP, "3"), 50, 20), 560, 145);
        step1.add(createInvisiLink("http://wiki.whirled.com/", 90, 20), 490, 160);

        AbsolutePanel step2 = MsoyUI.createAbsolutePanel("Step2");
        add(step2);

        AbsolutePanel step3 = MsoyUI.createAbsolutePanel("Step3");
        add(step3);
        step3.add(createInvisiLink(
            "http://www.deviantart.com/#catpath=projects/contests/2008/whirled", 290, 20), 30,
            480);

        // Rules are in text
        AbsolutePanel rules = MsoyUI.createAbsolutePanel("Rules");
        add(rules);
        rules.add(MsoyUI.createHTML(_msgs.dacontestRules(), null), 35, 70);
        rules.add(MsoyUI.createHTML(_msgs.dacontestJudging(), "Judging"), 355, 75);

        // tools icon sits over everything
        add(new Image("/images/landing/dacontest_tools.png"), 150, 110);
    }

    /**
     * Helper function for creating an invisible area of a given size with a given offsite link
     * that will be opened in a new window.
     */
    protected Widget createInvisiLink (final String offsiteLinkPath, int width, int height)
    {
        return createInvisiLink(new ClickListener() {
            public void onClick (Widget sender) {
                Window.open(offsiteLinkPath, "_blank", null);
            }
        }, width, height);
    }

    /**
     * Helper function for creating an invisible area of a given size with a given click event,
     * used to create a sort of imagemap.
     */
    protected Widget createInvisiLink (ClickListener listener, int width, int height)
    {
        Image image = MsoyUI.createActionImage("/images/landing/dacontest_blank.png", listener);
        image.setWidth(width + "px");
        image.setHeight(height + "px");
        return image;
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
