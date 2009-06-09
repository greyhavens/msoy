//
// $Id$

package client.billing;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.gwt.ui.SmartTable;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.BillingUtil;

/**
 * Displays an interface explaining subscription and linking to the billing system.
 */
public class SubscribePanel extends BillingPanel
{
    public SubscribePanel ()
    {
        super("/images/billing/join_club_whirled.png", _msgs.subscribeIntro());
        addStyleName("subscribe");

        RoundBox box = new RoundBox(RoundBox.DARK_BLUE);
        add(box);

        // if we're already a subscriber, just show a thank you and some useful links
        if (CShell.isSubscriber()) {
            box.add(MsoyUI.createHTML(_msgs.subscribeThanks(BillingUtil.getAccountStatusPage()),
                                      "Features"));
            return;
        }

        box.add(MsoyUI.createHTML(_msgs.subscribeFeatures(), "Features"));

        PushButton gobuy = new PushButton(
            _images.cc_default().createImage(), _images.cc_down().createImage(),
            new ClickHandler() {
                public void onClick (ClickEvent event) {
                    BillingUtil.popBillingPage("subscribe.wm");
                }
            });
        gobuy.getUpHoveringFace().setImage(_images.cc_over().createImage());

        SmartTable gotable = new SmartTable(0, 0);
        gotable.setHTML(0, 0, _msgs.subscribeDoItNow(), 1, "GoBuy");
        gotable.setWidget(0, 1, gobuy);
        box.add(gotable);
    }
}
