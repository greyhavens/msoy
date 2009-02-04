//
// $Id: PromotionBox.java 13554 2008-11-26 21:49:32Z mdb $

package client.ui;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.threerings.gwt.ui.FloatPanel;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.Contest;

import client.shell.ShellMessages;
import client.util.MediaUtil;

/**
 * Displays a contest, present or past.
 */
public class ContestBox extends FloatPanel
{
    public ContestBox (Contest contest)
    {
        super("contestBox");

        // contest is still going on
        if (contest.ends.after(new Date())) {

            // left is icon
            FlowPanel icon = MsoyUI.createFlowPanel("Icon");
            add(icon);
            if (contest.icon != null) {
                icon.add(MediaUtil.createMediaView(contest.icon, MediaDesc.THUMBNAIL_SIZE));
            } else {
                icon.add(MsoyUI.createHTML("&nbsp;", null));
            }

            // center is name, text, status
            FlowPanel contestInfo = MsoyUI.createFlowPanel("ContestInfo");
            add(contestInfo);

            contestInfo.add(MsoyUI.createHTML(contest.name, "ContestName"));
            contestInfo.add(MsoyUI.createHTML(contest.blurb, "ContestText"));
            contestInfo.add(MsoyUI.createHTML(contest.status, "Status"));

            // right is prizes
            FlowPanel prizes = MsoyUI.createFlowPanel("Prizes");
            add(prizes);
            prizes.add(MsoyUI.createLabel(_cmsgs.contestPrizes(), "PrizesTitle"));
            prizes.add(MsoyUI.createHTML(contest.prizes, "PrizesText"));

        // contest has ended; display the pastBlurb only
        } else {
            addStyleDependentName("past");
            add(MsoyUI.createHTML(contest.pastBlurb, null));
        }
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
