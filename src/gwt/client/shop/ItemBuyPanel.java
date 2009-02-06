//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.PurchaseResult;

import client.item.ItemActivator;
import client.money.BuyPanel;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * An interface for buying a CatalogListing. Doesn't display anything but functional buy
 * buttons.
 */
public class ItemBuyPanel extends BuyPanel<CatalogService.ItemPurchaseResult>
{
    /**
     * @param callback optional. Notified only on success.
     */
    public ItemBuyPanel (CatalogListing listing, AsyncCallback<Item> callback)
    {
        super(listing.quote);
        _listing = listing;
        _callback = callback;
    }

    @Override
    protected void makePurchase (
        Currency currency, int amount, AsyncCallback<CatalogService.ItemPurchaseResult> listener)
    {
        if (CShell.isGuest()) {
            MsoyUI.infoAction(_msgs.msgMustRegister(), _msgs.msgRegister(),
                Link.createListener(Pages.ACCOUNT, "create"));
        } else {
            _catalogsvc.purchaseItem(_listing.detail.item.getType(), _listing.catalogId,
                currency, amount, listener);
        }
    }

    @Override
    protected void addPurchasedUI (CatalogService.ItemPurchaseResult result, Currency currency)
    {
        Item item = result.item;
        byte itype = item.getType();

        if (_callback != null) {
            _callback.onSuccess(item);
        }

        // change the buy button into a "you bought it" display
        String type = _dmsgs.xlate("itemType" + itype);
        add(MsoyUI.createLabel(_msgs.boughtTitle(type), "Title"));

        if (FlashClients.clientExists()) {
            if (item instanceof SubItem) {
                add(WidgetUtil.makeShim(10, 10));
                add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.boughtBackTo(),
                    new ClickListener() {
                    public void onClick (Widget sender) {
                        CShell.frame.closeContent();
                    }
                }));
            } else {
                add(new ItemActivator(item, true));
                add(new Label(getUsageMessage(itype)));
            }

        } else {
            add(new Label(_msgs.boughtViewStuff(type)));
            String ptype = _dmsgs.xlate("pItemType" + itype);
            add(Link.create(_msgs.boughtGoNow(ptype), Pages.STUFF, ""+itype));
        }
    }

    protected static String getUsageMessage (byte itemType)
    {
        if (itemType == Item.AVATAR) {
            return _msgs.boughtAvatarUsage();
        } else if (itemType == Item.DECOR) {
            return _msgs.boughtDecorUsage();
        } else if (itemType == Item.AUDIO) {
            return _msgs.boughtAudioUsage();
        } else if (itemType == Item.PET) {
            return _msgs.boughtPetUsage();
        } else {
            return _msgs.boughtOtherUsage();
        }
    }

    protected CatalogListing _listing;

    protected AsyncCallback<Item> _callback;
    
    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
