//
// $Id$

package client.billing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.money.BuyPanel;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.BillingUtil;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Displays an interface explaining subscription and linking to the billing system.
 */
@Deprecated
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
            new Image(_images.cc_default()), new Image(_images.cc_down()), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    BillingUtil.openBillingPage(BillingPage.IFRAME_SUBSCRIBE);
                }
            });
        gobuy.getUpHoveringFace().setImage(new Image(_images.cc_over()));

        SmartTable gotable = new SmartTable(0, 0);
        gotable.setHTML(0, 0, _msgs.subscribeDoItNow(), 1, "GoBuy");
        gotable.setWidget(0, 1, gobuy);
        box.add(gotable);

        add(WidgetUtil.makeShim(20,20));
        final Label barscribe = new Label(_msgs.orBarscribe());
        barscribe.addStyleName("actionLabel");
        barscribe.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                remove(barscribe);
                addBarscribing();
            }
        });
        add(barscribe);
    }

    protected void addBarscribing ()
    {
        final BuyPanel<WebCreds.Role> buyPanel = new BuyPanel<WebCreds.Role>() {
            @Override protected boolean makePurchase (
                Currency currency, int amount, AsyncCallback<PurchaseResult<WebCreds.Role>> cback)
            {
                _membersvc.barscribe(amount, cback);
                return true;
            }
        };
        _membersvc.getBarscriptionCost(new InfoCallback<PriceQuote>() {
            public void onSuccess (PriceQuote result) {
                buyPanel.init(result, new InfoCallback<WebCreds.Role>() {
                    public void onSuccess (WebCreds.Role role) {
                        CShell.creds.role = role;
                        Link.go(Pages.PEOPLE, CShell.getMemberId());
                    }
                });
            }
        });
        add(MsoyUI.createLabel(_msgs.barscribeUpsell(), null));
        add(buyPanel.createPromptHost(_msgs.barscribe()));
    }

    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);
}
