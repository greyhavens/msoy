//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.FloatPanel;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Landing page listing winners of the deviant art contest
 */
public class DAContestWinnersPanel extends FlowPanel
{
    public DAContestWinnersPanel ()
    {
        setStyleName("daContestWinnersPanel");
        addStyleName("BlueLandingPage");
        FlowPanel content = MsoyUI.createFlowPanel("Content");
        add(content);

        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");
        header.add(MsoyUI.createActionImage(
            "/images/landing/contests/dawinners_whirled_logo.png", Link.createListener(
                Pages.LANDING, "")), 5, 5);
        header.add(new Image("/images/landing/contests/dawinners_ooo_presents.png"), 350, 0);
        header.add(MsoyUI.createHTML(_msgs.dawinnersIntro(), "Intro"), 20, 250);
        content.add(header);

        // first, second and third place
        FloatPanel winners = new FloatPanel(null);
        FlowPanel winner1 = MsoyUI.createFlowPanel("Winner1");
        winner1.add(MsoyUI.createActionImage("/images/landing/contests/dawinners_winner_1.png",
            Link.createListener(Pages.WORLD, "s" + WINNER_IDS[0])));
        winner1.add(MsoyUI.createHTML(_msgs.dawinnersWinner1(), null));
        winners.add(winner1);

        FlowPanel winner2 = MsoyUI.createFlowPanel("Winner2");
        winner2.add(MsoyUI.createActionImage("/images/landing/contests/dawinners_winner_2.png",
            Link.createListener(Pages.WORLD, "s" + WINNER_IDS[1])));
        winner2.add(MsoyUI.createHTML(_msgs.dawinnersWinner2(), null));
        winners.add(winner2);

        FlowPanel winner3 = MsoyUI.createFlowPanel("Winner3");
        winner3.add(MsoyUI.createActionImage("/images/landing/contests/dawinners_winner_3.png",
            Link.createListener(Pages.WORLD, "s" + WINNER_IDS[2])));
        winner3.add(MsoyUI.createHTML(_msgs.dawinnersWinner3(), null));
        winners.add(winner3);
        content.add(new WideContentBox(winners));

        content.add(MsoyUI.createImage("/images/landing/contests/dawinners_honorable_title.png",
            "SectionTitle"));

        // honorable mentions
        FlowPanel honorable = MsoyUI.createFlowPanel(null);
        FloatPanel honorableTop = new FloatPanel("HonorableTop");
        honorableTop.add(MsoyUI.createActionImage(
            "/images/landing/contests/dawinners_honorable_1.png", Link.createListener(
                Pages.WORLD, "s" + HONORABLE_IDS[0])));
        honorableTop.add(MsoyUI.createActionImage(
            "/images/landing/contests/dawinners_honorable_2.png", Link.createListener(
                Pages.WORLD, "s" + HONORABLE_IDS[1])));
        honorableTop.add(MsoyUI.createActionImage(
            "/images/landing/contests/dawinners_honorable_3.png", Link.createListener(
                Pages.WORLD, "s" + HONORABLE_IDS[2])));
        honorable.add(honorableTop);

        FloatPanel honorableBottom = new FloatPanel("HonorableBottom");
        honorableBottom.add(MsoyUI.createActionImage(
            "/images/landing/contests/dawinners_honorable_4.png", Link.createListener(
                Pages.WORLD, "s" + HONORABLE_IDS[3])));
        honorableBottom.add(MsoyUI.createActionImage(
            "/images/landing/contests/dawinners_honorable_5.png", Link.createListener(
                Pages.WORLD, "s" + HONORABLE_IDS[4])));
        honorable.add(honorableBottom);

        honorable.add(MsoyUI.createHTML(_msgs.dawinnersHonorable(), "HonorableText"));
        content.add(new WideContentBox(honorable));

        // too cool for school
        content.add(MsoyUI.createImage("/images/landing/contests/dawinners_other_title.png",
            "SectionTitle"));
        FlowPanel others = MsoyUI.createFlowPanel(null);
        others.add(MsoyUI.createImage("/images/landing/contests/dawinners_other_text.png",
            "OtherText"));
        FloatPanel othersColumns = new FloatPanel(null);
        othersColumns.add(MsoyUI.createHTML(_msgs.dawinnersOthersLeft(), "OthersColumn"));
        othersColumns.add(MsoyUI.createHTML(_msgs.dawinnersOthersRight(), "OthersColumn"));
        others.add(othersColumns);
        content.add(new WideContentBox(others));

        // footer stretches full width, contains copyright info
        add(MsoyUI.createSimplePanel(new LandingCopyright(), "Footer"));
    }

    // scene ids of the winning and honorable mention rooms that we show images for
    protected static final int[] WINNER_IDS = { 107217, 119439, 89294 };
    protected static final int[] HONORABLE_IDS = { 101315, 94819, 116771, 99355, 130834 };

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
