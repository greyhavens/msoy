//
// $Id$

package client.me;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MeServiceAsync;
import com.threerings.msoy.web.gwt.Contest;

import client.ui.ContestBox;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Page displaying a list of official Whirled contests past and present.
 */
public class ContestsPanel extends FlowPanel
{
    public ContestsPanel ()
    {
        setStyleName("contestsPanel");
        _mesvc.loadContests(new MsoyCallback<List<Contest>>() {
            public void onSuccess (List<Contest> contests) {
                init(contests);
            }
        });
    }
    
    protected void init (List<Contest> contests)
    {
        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");
        header.add(new Image("/images/me/contests_title.png"), 30, 0);
        header.add(MsoyUI.createLabel(_msgs.contestsIntro(), "Intro"), 250, 10);
        header.add(new Image("/images/me/contests_tofu.png"), 550, 10);
        add(header);

        RoundBox currentContests = new RoundBox(RoundBox.MEDIUM_BLUE);
        add(currentContests);
        currentContests.addStyleName("CurrentContests");
        currentContests.add(MsoyUI.createLabel(_msgs.contestsCurrent(), "CurrentTitle"));

        FlowPanel pastContests = MsoyUI.createFlowPanel("PastContests");
        add(pastContests);
        pastContests.add(MsoyUI.createLabel(_msgs.contestsPast(), "PastTitle"));
        
        for (Contest contest : contests) {
            if (contest.ends.after(new Date())) {
                currentContests.add(new ContestBox(contest));
            } else {
                pastContests.add(new ContestBox(contest));
            }
        }

        add(WidgetUtil.makeShim(10, 10));
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MeServiceAsync _mesvc = (MeServiceAsync)
        ServiceUtil.bind(GWT.create(MeService.class), MeService.ENTRY_POINT);
}
