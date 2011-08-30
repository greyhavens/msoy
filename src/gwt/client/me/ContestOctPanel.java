//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.WidgetUtil;

import client.ui.MsoyUI;
import client.ui.RoundBox;

/**
 * Page displaying a list of official Whirled contests past and present.
 */
public class ContestOctPanel extends FlowPanel
{
    public ContestOctPanel ()
    {
        setStyleName("contestOctPanel");

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

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
