//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.IdentGameItem;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ItemActivator;
import client.item.ItemUtil;
import client.money.BuyPanel;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.FlashClients;
import client.util.Link;
import client.util.NaviUtil;

/**
 * An interface for buying a CatalogListing. Doesn't display anything but functional buy buttons.
 */
public class ItemBuyPanel extends BuyPanel<Item>
{
    /**
     * @param callback optional. Notified only on success.
     */
    public ItemBuyPanel (CatalogListing listing, AsyncCallback<Item> callback)
    {
        _listing = listing;
        init(listing.quote, callback);
    }

    @Override
    protected boolean makePurchase (
        Currency currency, int amount, AsyncCallback<PurchaseResult<Item>> listener)
    {
        if (CShell.isGuest()) {
            MsoyUI.infoAction(_msgs.msgMustRegister(), _msgs.msgRegister(),
                              NaviUtil.onMustRegister());
            return false;

        } else {
            _catalogsvc.purchaseItem(_listing.detail.item.getType(), _listing.catalogId,
                currency, amount, ItemUtil.getMemories(), listener);
            return true;
        }
    }

    @Override
    protected void addPurchasedUI (Item item, FlowPanel boughtPanel)
    {
        MsoyItemType itype = item.getType();

        // change the buy button into a "you bought it" display
        String type = _dmsgs.xlateItemType(itype);
        boughtPanel.add(MsoyUI.createLabel(_msgs.boughtTitle(type), "Title"));

        if (FlashClients.clientExists()) {
            if (item instanceof IdentGameItem) {
                boughtPanel.add(WidgetUtil.makeShim(10, 10));
                boughtPanel.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.boughtBackTo(),
                    new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        CShell.frame.closeContent();
                    }
                }));
            } else {
                boughtPanel.add(new ItemActivator(item, true));
                boughtPanel.add(new Label(getUsageMessage(itype)));
            }

        } else {
            boughtPanel.add(new Label(_msgs.boughtViewStuff(type)));
            String ptype = _dmsgs.xlateItemsType(itype);
            boughtPanel.add(Link.create(_msgs.boughtGoNow(ptype), Pages.STUFF, itype));
        }
    }

    protected static String getUsageMessage (MsoyItemType itemType)
    {
        if (itemType == MsoyItemType.AVATAR) {
            return _msgs.boughtAvatarUsage();
        } else if (itemType == MsoyItemType.DECOR) {
            return _msgs.boughtDecorUsage();
        } else if (itemType == MsoyItemType.AUDIO) {
            return _msgs.boughtAudioUsage();
        } else if (itemType == MsoyItemType.PET) {
            return _msgs.boughtPetUsage();
        } else {
            return _msgs.boughtOtherUsage();
        }
    }

    protected CatalogListing _listing;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = GWT.create(CatalogService.class);
}
