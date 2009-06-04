//
// $Id$

package client.billing;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;

import client.ui.MsoyUI;
import client.util.BillingUtil;

/**
 * Displays an interface explaining subscription and linking to the billing system.
 */
public class SubscribePanel extends FlowPanel
{
    public SubscribePanel ()
    {
        setStyleName("subscribe");

        add(MsoyUI.createLabel("Subscribe now! It's frawesome!", "Title"));

        add(MsoyUI.createActionLabel("Subscribe now!", new ClickHandler() {
            public void onClick (ClickEvent event) {
                BillingUtil.popBillingPage("subscribe.wm");
            }
        }));
    }
}
