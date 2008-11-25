//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.WidgetUtil;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.RoundBox;

/**
 * Page displaying a list of official Whirled contests past and present.
 */
public class ContestsPanel extends FlowPanel
{
    public ContestsPanel ()
    {
        setStyleName("contestsPanel");

        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");
        header.add(new Image("/images/me/contests_title.png"), 30, 0);
        header.add(MsoyUI.createLabel(_msgs.contestsIntro(), "Intro"), 250, 10);
        header.add(new Image("/images/me/contests_tofu.png"), 550, 10);
        add(header);

        RoundBox currentContests = new RoundBox(RoundBox.MEDIUM_BLUE);
        add(currentContests);
        currentContests.addStyleName("CurrentContests");
        currentContests.add(MsoyUI.createLabel(_msgs.contestsCurrent(), "CurrentTitle"));

        for (String id : CONTESTS) {
            FloatPanel contest = new FloatPanel("Contest");
            currentContests.add(contest);

            // left is icon
            contest.add(new Image("/images/me/contests_icon_" + id + ".png"));

            // center is name, text, status
            FlowPanel contestInfo = MsoyUI.createFlowPanel("ContestInfo");
            contest.add(contestInfo);

            HTML contestName = MsoyUI.createHTML(_dmsgs.xlate("contestsTitle_" + id),
                "ContestName");
            MsoyUI.addTrackingListener(contestName, "contestsNameClicked", id);
            contestInfo.add(contestName);

            contestInfo.add(MsoyUI.createHTML(_dmsgs.xlate("contestsText_" + id), "ContestText"));
            contestInfo.add(MsoyUI.createHTML(_dmsgs.xlate("contestsStatus_" + id), "Status"));

            // right is prizes
            FlowPanel prizes = MsoyUI.createFlowPanel("Prizes");
            contest.add(prizes);
            prizes.add(MsoyUI.createLabel(_msgs.contestsPrizes(), "PrizesTitle"));
            prizes.add(MsoyUI.createHTML(_dmsgs.xlate("contestsPrizes_" + id), "PrizesText"));
        }

        // past contests
        FlowPanel pastContests = MsoyUI.createFlowPanel("PastContests");
        add(pastContests);
        pastContests.add(MsoyUI.createLabel(_msgs.contestsPast(), "PastTitle"));
        for (String id : CONTESTS_PAST) {
            pastContests.add(MsoyUI.createHTML(_dmsgs.xlate("contestsPast_" + id), "PastContest"));
        }

        add(WidgetUtil.makeShim(10, 10));
    }

    protected static final String[] CONTESTS = { "GameCreator", "HideAndGhost" };
    protected static final String[] CONTESTS_PAST = { "DesignYour" };

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
