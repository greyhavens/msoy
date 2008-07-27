//
// $Id$

package client.account;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.ui.MsoyUI;
import client.ui.RoundBox;

/**
 * Displays a welcome message to newly registered users.
 */
public class WelcomePanel extends FlowPanel
{
    public WelcomePanel ()
    {
        setStyleName("welcome");

        RoundBox box = new RoundBox(RoundBox.DARK_BLUE);
        box.addStyleName("Box");
        box.add(MsoyUI.createLabel(CAccount.msgs.welcomeBoxTitle(), "Title"));
        box.add(MsoyUI.createHTML(CAccount.msgs.welcomeSubtitle(""+STARTER_FLOW), null));
        add(box);

        add(WidgetUtil.makeShim(20, 20));
        SmartTable bits = new SmartTable("Bits", 0, 0);
        bits.setText(0, 0, CAccount.msgs.welcomeWhatTitle(), 1, "Title");
        bits.setText(1, 0, CAccount.msgs.welcomeWhatExplain(), 1, "Explain");
        bits.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);

        bits.setText(0, 1, CAccount.msgs.welcomeHowMuchTitle(), 1, "Title");
        FlowPanel howMuch = new FlowPanel();
        howMuch.add(MsoyUI.createImage("/images/welcome/howmuch_arrow.png", "HowMuchArrow"));
        howMuch.add(MsoyUI.createImage("/images/welcome/howmuch.png", null));
        howMuch.add(MsoyUI.createLabel(CAccount.msgs.welcomeHowMuchExplain(), "Tip"));
        bits.setWidget(1, 1, howMuch);
        add(bits);

        add(WidgetUtil.makeShim(20, 20));
        add(MsoyUI.createImage("/images/welcome/whirled_cycle.png", null));

        add(WidgetUtil.makeShim(20, 20));
        add(MsoyUI.createHTML(CAccount.msgs.welcomeFree(), "FreeTitle"));

        add(WidgetUtil.makeShim(20, 20));
        SmartTable next = new SmartTable("Next", 0, 0);
        next.setWidget(0, 0, MsoyUI.createImage("/images/welcome/next_arrow.png", "NextArrow"));
        next.getFlexCellFormatter().setRowSpan(0, 0, 2);
        next.setHTML(0, 1, CAccount.msgs.welcomeNextTitle());
        next.getFlexCellFormatter().setStyleName(0, 1, "Title");
        next.setText(1, 0, CAccount.msgs.welcomeNextTip(), 1, "Tip");
        add(next);

        add(WidgetUtil.makeShim(20, 20));
        add(MsoyUI.createImage("/images/welcome/me_snapshot.png", null));
    }

    // this is defined in UserAction which we can't reference from GWT, when GWT supports 1.5
    // features, we can move it to MemberCodes or somewhere accessible to everyone
    protected static final int STARTER_FLOW = 1000;
}
