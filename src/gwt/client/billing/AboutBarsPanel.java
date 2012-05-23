//
// $Id$

package client.billing;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.WidgetUtil;

import client.ui.MsoyUI;
import client.ui.StretchButton;
import client.util.BillingUtil;

/**
 * Guests and players whose accounts are not billing-ready are shown this blurb about why they
 * should buy bars instead of being sent to the billing site.
 */
public class AboutBarsPanel extends BillingPanel
{
    public AboutBarsPanel ()
    {
        super("/images/billing/buy_some_bars.png", _msgs.aboutBarsIntro());
        addStyleName("aboutBars");

        add(MsoyUI.createHTML(_msgs.aboutBarsMethods(), "Methods"));

        // show some sample payment methods, link the whole thing to billing
        FloatPanel methodButtons = new FloatPanel("MethodButtons");
        methodButtons.add(new Image(_images.cc_default()));
        methodButtons.add(new Image(_images.paypal_default()));
        methodButtons.add(new Image(_images.ooo_card_default()));
        methodButtons.add(new Image(_images.sms_default()));
        FocusPanel methodButtonsWrapper = new FocusPanel(methodButtons);
        methodButtonsWrapper.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                BillingUtil.openBillingPage(BillingPage.IFRAME);
            }});
        add(methodButtonsWrapper);

        add(StretchButton.makeOrange(_msgs.aboutBarsGetBars(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                BillingUtil.openBillingPage(BillingPage.IFRAME);
            }
        }));

        add(WidgetUtil.makeShim(20, 20));
    }
}
