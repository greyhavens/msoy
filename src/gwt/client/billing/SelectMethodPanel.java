//
// $Id$

package client.billing;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.gwt.ui.SmartTable;

import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.BillingUtil;

/**
 * Displays the UI for selecting a payment method.
 */
public class SelectMethodPanel extends BillingPanel
{
    public SelectMethodPanel ()
    {
        super("/images/billing/buy_some_bars.png", _msgs.selectIntro());
        addStyleName("selectMethod");

        SmartTable methods = new SmartTable("Methods", 0, 0);
        RoundBox box = new RoundBox(RoundBox.DARK_BLUE);
        FlowPanel balloons = MsoyUI.createFlowPanel("Balloons");
        for (final Method method : METHODS) {
            PushButton button = new PushButton(
                method.normal.createImage(), method.down.createImage(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        BillingUtil.popBillingPage(method.target);
                    }
                });
            button.getUpHoveringFace().setImage(method.hover.createImage());
            box.add(button);
            balloons.add(MsoyUI.createHTML(method.tip, "Tip"));
        }
        methods.setWidget(1, 0, box);
        methods.setWidget(1, 1, balloons);
        add(methods);
    }

    protected static class Method
    {
        public AbstractImagePrototype normal;
        public AbstractImagePrototype hover;
        public AbstractImagePrototype down;
        public String tip;
        public String target;

        public Method (AbstractImagePrototype normal, AbstractImagePrototype hover,
                       AbstractImagePrototype down, String tip, String target) {
            this.normal = normal;
            this.hover = hover;
            this.down = down;
            this.tip = tip;
            this.target = target;
        }
    }

    protected static final Method[] METHODS = {
        new Method(_images.cc_default(), _images.cc_over(), _images.cc_down(),
                   _msgs.selectCCTip(), "buy_coins.wm"),
        new Method(_images.paypal_default(), _images.paypal_over(), _images.paypal_down(),
                   _msgs.selectPayPalTip(), "paypal/choosecoins.jspx"),
        new Method(_images.ooo_card_default(), _images.ooo_card_over(), _images.ooo_card_down(),
                   _msgs.selectOOOCardTip(), "threeringscard/check.jspx?mode=coins"),
        new Method(_images.sms_default(), _images.sms_over(), _images.sms_down(),
                   _msgs.selectSMSTip(), "mobill/choosecoins.jspx"),
        new Method(_images.paysafe_default(), _images.paysafe_over(), _images.paysafe_down(),
                   _msgs.selectPaysafeTip(), "paysafecard/choosecoins.jspx"),
        new Method(_images.other_default(), _images.other_over(), _images.other_down(),
                   _msgs.selectOtherTip(), "otheroptions.wm?choice=coins"),
    };
}
