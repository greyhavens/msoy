//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.data.gwt.CatalogListing;

import com.threerings.msoy.web.data.CostUpdatedException;

import client.item.BaseItemDetailPanel;
import client.item.ItemActivator;
import client.shell.Application;
import client.shell.Args;
import client.shell.CommentsPanel;
import client.shell.CShell;
import client.shell.Page;
import client.shell.ShellMessages;
import client.stuff.CStuff;
import client.stuff.DoListItemPopup;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.Link;
import client.util.MsoyUI;
import client.util.PopupMenu;
import client.util.ShopUtil;

/**
 * Displays a detail view of an item from the catalog.
 */
public class ListingDetailPanel extends BaseItemDetailPanel
{
    public ListingDetailPanel (CatalogModels models, CatalogListing listing)
    {
        super(listing.detail);
        addStyleName("listingDetailPanel");

        _models = models;
        _listing = listing;

// TODO
//         ItemUtil.addItemSpecificButtons(_item, _buttons);

        _indeets.add(WidgetUtil.makeShim(10, 10));
        _indeets.add(_priceLabel = new PriceLabel(_listing.flowCost, _listing.goldCost));

        _details.add(WidgetUtil.makeShim(10, 10));
        PushButton purchase =
            MsoyUI.createButton(MsoyUI.SHORT_THICK, CShop.msgs.listingBuy(), null);
        new ClickCallback<Item>(purchase) {
            public boolean callService () {
                if (CShop.isGuest()) {
                    MsoyUI.infoAction(CShop.msgs.msgMustRegister(), CShop.msgs.msgRegister(),
                                      Link.createListener(Page.ACCOUNT, "create"));
                } else {
                    CShop.catalogsvc.purchaseItem(
                        CShop.ident, _item.getType(), _listing.catalogId,
                        _listing.flowCost, _listing.goldCost, this);
                }
                return true;
            }
            public boolean gotResult (Item item) {
                itemPurchased(item);
                return false; // don't reenable buy button
            }

            public void onFailure (Throwable cause)
            {
                super.onFailure(cause);

                if (cause instanceof CostUpdatedException) {
                    CostUpdatedException cue = (CostUpdatedException) cause;
                    _listing.flowCost = cue.getFlowCost();
                    _listing.goldCost = cue.getGoldCost();
                    _priceLabel.updatePrice(cue.getFlowCost(), cue.getGoldCost());
                }
            }
        };
        _buyPanel = new FlowPanel();
        _buyPanel.add(purchase);

        if (!CShop.isGuest() && isRemixable()) {
            PushButton remix = MsoyUI.createButton(MsoyUI.SHORT_THICK, CStuff.msgs.detailRemix(),
                new ClickListener() {
                    public void onClick (Widget sender) {
                        CStuff.remixCatalogItem(
                            _item.getType(), _item.itemId, _listing.catalogId,
                            _listing.flowCost, _listing.goldCost);
                    }
                });
            _buyPanel.add(remix);
        }

        _details.add(_buyPanel);

        // create a table to display miscellaneous info and admin/owner actions
        SmartTable info = new SmartTable("Info", 0, 5);
        info.setText(0, 0, CShop.msgs.listingListed(), 1, "What");
        info.setText(0, 1, _lfmt.format(listing.listedDate));
        info.setText(1, 0, CShop.msgs.listingPurchases(), 1, "What");
        info.setText(1, 1, "" + listing.purchases);

        // if we are the creator (lister) of this item, allow us to delist it
        if (_detail.creator.getMemberId() == CShop.getMemberId() || CShop.isSupport()) {
            Label reprice = new Label(CShop.msgs.listingReprice());
            reprice.addStyleName("actionLabel");
            reprice.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    DoListItemPopup.show(_item, _listing, new DoListItemPopup.ListedListener() {
                        public void itemListed (Item item, boolean updated) {
                            Application.replace(Page.SHOP, Args.compose(new String[] {
                                "l", "" + _item.getType(), "" + _listing.catalogId,
                                "repriced_from_" + _listing.flowCost}));
                        }
                    });
                }
            });
            info.addWidget(reprice, 2, null);

            Label delist = new Label(CShop.msgs.listingDelist());
            new ClickCallback<Void>(delist, CShop.msgs.listingDelistConfirm()) {
                public boolean callService () {
                    CShop.catalogsvc.removeListing(
                        CShop.ident, _item.getType(), _listing.catalogId, this);
                    return true;
                }
                public boolean gotResult (Void result) {
                    MsoyUI.info(CShop.msgs.msgListingDelisted());
                    _models.itemDelisted(_listing);
                    History.back();
                    return false;
                }
            };
            info.addWidget(delist, 2, null);

            if (_listing.originalItemId != 0) {
                // also add a link to view the original
                String args = Args.compose("d", ""+_item.getType(), ""+_listing.originalItemId);
                info.addWidget(Link.create(CShop.msgs.listingViewOrig(),
                                                      Page.STUFF, args), 2, null);
            }
        }

        _details.add(WidgetUtil.makeShim(10, 10));
        _details.add(info);

        // display a comment interface below the listing details
        addTabBelow("Comments", new CommentsPanel(_item.getType(), listing.catalogId), true);

//         // if this item supports sub-items, add a tab for those item types
//         byte[] types = _item.getSalableSubTypes();
//         if (types.length > 0) {
//             for (int ii = 0; ii < types.length; ii++) {
//                 addTabBelow(CShop.dmsgs.getString("pItemType" + types[ii]), new Label("TBD"));
//             }
//         }
    }

    protected void itemPurchased (Item item)
    {
        byte itype = item.getType();

        // clear out the buy button
        _buyPanel.clear();
        _buyPanel.addStyleName("Bought");

        // report to the client that we generated a tutorial event
        if (itype == Item.DECOR) {
            FlashClients.tutorialEvent("decorBought");
        } else if (itype == Item.FURNITURE) {
            FlashClients.tutorialEvent("furniBought");
        } else if (itype == Item.PET) {
            FlashClients.tutorialEvent("petBought");
        } else if (itype == Item.AVATAR) {
            FlashClients.tutorialEvent("avatarBought");
        }

        // change the buy button into a "you bought it" display
        String type = CShop.dmsgs.getString("itemType" + itype);
        _buyPanel.add(MsoyUI.createLabel(CShop.msgs.boughtTitle(type), "Title"));

        if (FlashClients.clientExists() && !(item instanceof SubItem)) {
            _buyPanel.add(new ItemActivator(item, true));
            _buyPanel.add(new Label(getUsageMessage(itype)));
        } else {
            _buyPanel.add(new Label(CShop.msgs.boughtViewStuff(type)));
            String ptype = CShop.dmsgs.getString("pItemType" + itype);
            _buyPanel.add(Link.create(
                              CShop.msgs.boughtGoNow(ptype), Page.STUFF, ""+itype));
        }
    }

    protected static String getUsageMessage (byte itemType)
    {
        if (itemType == Item.AVATAR) {
            return CShop.msgs.boughtAvatarUsage();
        } else if (itemType == Item.DECOR) {
            return CShop.msgs.boughtDecorUsage();
        } else if (itemType == Item.AUDIO) {
            return CShop.msgs.boughtAudioUsage();
        } else if (itemType == Item.PET) {
            return CShop.msgs.boughtPetUsage();
        } else {
            return CShop.msgs.boughtOtherUsage();
        }
    }

    @Override
    protected void addTagMenuItems (final String tag, PopupMenu menu)
    {
        menu.addMenuItem(_cmsgs.tagSearch(), new Command() {
            public void execute() {
                Link.go(Page.SHOP, ShopUtil.composeArgs(_item.getType(), tag, null, 0));
            }
        });
    }

    protected CatalogModels _models;
    protected CatalogListing _listing;
    protected FlowPanel _buyPanel;
    protected PriceLabel _priceLabel;

    protected static final SimpleDateFormat _lfmt = new SimpleDateFormat("MMM dd, yyyy");
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
