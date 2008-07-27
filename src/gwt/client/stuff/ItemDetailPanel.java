//
// $Id$

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.client.CatalogServiceAsync;
import com.threerings.msoy.web.client.ItemService;
import com.threerings.msoy.web.client.ItemServiceAsync;

import client.item.BaseItemDetailPanel;
import client.item.ItemActivator;
import client.shell.Application;
import client.shell.Args;
import client.shell.CShell;
import client.shell.DynamicMessages;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.Link;
import client.util.MsoyUI;
import client.util.RowPanel;
import client.util.ServiceUtil;

import client.util.events.FlashEvents;
import client.util.events.ItemUsageEvent;
import client.util.events.ItemUsageListener;

/**
 * Displays a popup detail view of an item from the user's inventory.
 */
public class ItemDetailPanel extends BaseItemDetailPanel
    implements ItemUsageListener, DoListItemPopup.ListedListener
{
    public ItemDetailPanel (InventoryModels models, ItemDetail detail)
    {
        super(detail);
        _models = models;

// TODO
//         ItemUtil.addItemSpecificButtons(_item, _buttons);

        // only add owner buttons for owners and support
        if (_item.ownerId == CShell.getMemberId() || CShell.isSupport()) {
            addOwnerButtons();
        }

        // if this item supports sub-items, add a tab for those item types
        SubItem[] types = _item.getSubTypes();
        for (int ii = 0; ii < types.length; ii++) {
            // if this is not an original item, only show salable subtypes
            if (_item.sourceId != 0 && !types[ii].isSalable()) {
                continue;
            }
            addTabBelow(_dmsgs.getString("pItemType" + types[ii].getType()),
                        new SubItemPanel(models, types[ii].getType(), _item), false);
        }
    }

    // from DoListItemPopup.ListedListener
    public void itemListed (Item item, boolean updated)
    {
        // if this was a first time listing, change "List..." to "Update listing..."
        if (!updated) {
            _listTip.setText(CStuff.msgs.detailUplistTip());
            _listBtn.setText(CStuff.msgs.detailUplist());
        }
    }

    // from interface ItemUsageListener
    public void itemUsageChanged (ItemUsageEvent event)
    {
        if ((event.getItemType() == _item.getType()) && (event.getItemId() == _item.itemId)) {
            // make any changes
            adjustButtonsBasedOnUsage();
        }
    }

    @Override
    protected void onAttach ()
    {
        super.onAttach();

        FlashEvents.addListener(this);
    }

    @Override
    protected void onDetach ()
    {
        super.onDetach();

        FlashEvents.removeListener(this);
    }

    @Override // BaseItemDetailPanel
    protected boolean userOwnsItem ()
    {
        return (_item.ownerId == CShell.getMemberId());
    }

    @Override // BaseItemDetailPanel
    protected void onUpClicked ()
    {
        CStuff.viewParent(_item);
    }

    protected void addOwnerButtons ()
    {
        // figure out a few pieces of common info
        int memberId = CShell.getMemberId();
        boolean original = (_item.sourceId == 0);
        boolean catalogOriginal = _item.isCatalogOriginal();
        boolean canEditAndList = memberId == _item.creatorId || CShell.isSupport();
        boolean remixable = isRemixable();
        boolean used = (_item.used != Item.UNUSED);

        if (_item.ownerId == memberId && FlashClients.clientExists()) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(new ItemActivator(_item, false));
        }

        // if this item is in use, mention that
        if (used) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(new HTML(getUsageMessage()));
        }

        RowPanel buttons = new RowPanel();
        // add a button for deleting this item
        _deleteBtn = MsoyUI.createButton(MsoyUI.LONG_THIN, CStuff.msgs.detailDelete(), null);
        createDeleteCallback(_deleteBtn);
        buttons.add(_deleteBtn);

        // add a button for editing this item, if it's an original
        if (original && canEditAndList) {
            String butlbl = CStuff.msgs.detailEdit();
            buttons.add(MsoyUI.createButton(MsoyUI.LONG_THIN, butlbl, new ClickListener() {
                public void onClick (Widget sender) {
                    CStuff.editItem(_item.getType(), _item.itemId);
                }
            }));

        } else if (!original && remixable) {
            // if it's a remixed clone, add a button for reverting
            boolean mixed = _item.isAttrSet(Item.ATTR_REMIXED_CLONE);
            boolean newOrigAvail = _item.isAttrSet(Item.ATTR_ORIGINAL_UPDATED);
            PushButton revert = MsoyUI.createButton(MsoyUI.LONG_THIN,
                newOrigAvail ? CStuff.msgs.detailUpdate() : CStuff.msgs.detailRevert(), null);
            if (!newOrigAvail && !mixed) { // if the item is up-to-date, disable the button
                revert.setEnabled(false);
                revert.setTitle(CStuff.msgs.detailRevertNotNeeded());
            } else {
                _details.add(WidgetUtil.makeShim(10, 10));
                _details.add(new Label(newOrigAvail ? CStuff.msgs.detailUpdateTip()
                    : CStuff.msgs.detailRevertTip()));
            }
            createRevertCallback(revert);
            buttons.add(revert);
        }

        // add our delete/edit buttons if we have them
        if (buttons.getWidgetCount() > 0) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(buttons);
        }

        // if this item is listed in the catalog or listable, add a UI for that
        if (canEditAndList && (catalogOriginal || _item.sourceId == 0)) {
            String tip, butlbl;
            if (catalogOriginal) {
                tip = CStuff.msgs.detailUplistTip();
                butlbl = CStuff.msgs.detailUplist();
            } else {
                tip = CStuff.msgs.detailListTip();
                butlbl = CStuff.msgs.detailList();
            }
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(_listTip = new Label(tip));

            // add a button for listing or updating the item
            buttons = new RowPanel();
            ClickListener onClick = new ClickListener() {
                public void onClick (Widget sender) {
                    DoListItemPopup.show(_item, null, ItemDetailPanel.this);
                }
            };
            buttons.add(_listBtn = MsoyUI.createButton(MsoyUI.LONG_THIN, butlbl, onClick));

            boolean salable = (!(_item instanceof SubItem) || ((SubItem)_item).isSalable());
            if (catalogOriginal && salable) {
                // add a button for repricing the listing
                butlbl = CStuff.msgs.detailUpprice();
                PushButton button = MsoyUI.createButton(MsoyUI.LONG_THIN, butlbl, null);
                new ClickCallback<CatalogListing>(button) {
                    public boolean callService () {
                        _catalogsvc.loadListing(
                            CStuff.ident, _item.getType(), _item.catalogId, this);
                        return true;
                    }
                    public boolean gotResult (CatalogListing listing) {
                        DoListItemPopup.show(_item, listing, ItemDetailPanel.this);
                        return true;
                    }
                };
                buttons.add(button);
            }
            _details.add(WidgetUtil.makeShim(10, 5));
            _details.add(buttons);
        }

        // if this item is giftable, add a UI for that
        if (!catalogOriginal) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(new Label(CStuff.msgs.detailGiftTip()));
            _details.add(WidgetUtil.makeShim(10, 5));
            String[] args = new String[] { "w", "i", ""+_item.getType(), ""+_item.itemId };
            ClickListener onClick = Link.createListener(Page.MAIL, Args.compose(args));
            _giftBtn = MsoyUI.createButton(MsoyUI.LONG_THIN, CStuff.msgs.detailGift(), onClick);
            _details.add(_giftBtn);
        }

        buttons = new RowPanel();

        // add a button for renaming
        if (!original) {
            PushButton rename = MsoyUI.createButton(MsoyUI.LONG_THIN, CStuff.msgs.detailRename(),
                null);
            buttons.add(rename);
            new RenameHandler(rename, _item, _models);
        }

        // if remixable, add a button for that.
        if (remixable) {
            buttons.add(MsoyUI.createButton(MsoyUI.LONG_THIN, CStuff.msgs.detailRemix(),
                new ClickListener() {
                    public void onClick (Widget sender) {
                        CStuff.remixItem(_item.getType(), _item.itemId);
                    }
                }));
        }

        if (buttons.getWidgetCount() > 0) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(buttons);
        }

        adjustButtonsBasedOnUsage();
    }

    protected void adjustButtonsBasedOnUsage ()
    {
        boolean unused = (_item.used == Item.UNUSED);
        _deleteBtn.setEnabled(unused);
        if (_giftBtn != null) {
            _giftBtn.setEnabled(unused);
        }
    }

    /**
     * Get the usage message for an item, possibly detailing where it's being used.
     */
    protected String getUsageMessage ()
    {
        switch (_item.used) {
        case Item.USED_AS_FURNITURE:
        case Item.USED_AS_PET:
        case Item.USED_AS_BACKGROUND:
            return CStuff.msgs.detailInUseInRoom("" + _item.location, _detail.useLocation);

        default:
            return CStuff.msgs.detailInUse();
        }
    }

    /**
     * Create a click callback for deleting the item.
     */
    protected void createDeleteCallback (SourcesClickEvents trigger)
    {
        new ClickCallback<Void>(trigger, CStuff.msgs.detailConfirmDelete()) {
            public boolean callService () {
                _itemsvc.deleteItem(CStuff.ident, _item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Void result) {
                // remove the item from our cached models
                int suiteId = (_item instanceof SubItem) ? ((SubItem)_item).suiteId : 0;
                DataModel<Item> model = _models.getModel(_item.getType(), suiteId);
                if (model != null) {
                    model.removeItem(_item);
                }
                MsoyUI.info(CStuff.msgs.msgItemDeleted());
                History.back(); // back up to the page that contained the item
                return false;
            }
        };
    }

    /**
     * Create a click callback for reverting the item.
     */
    protected void createRevertCallback (SourcesClickEvents trigger)
    {
        new ClickCallback<Item>(trigger, CStuff.msgs.detailConfirmRevert()) {
            public boolean callService () {
                _itemsvc.revertRemixedClone(CStuff.ident, _item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Item item) {
                _models.updateItem(item);
                _item = item;
                _detail.item = item;

                // redisplay the item detail with the reverted version.
                Application.replace(Page.STUFF, Args.compose(new String[]
                    { "d", "" + item.getType(), "" + item.itemId, "revert" }));
                return false;
            }
        };
    }

    protected InventoryModels _models;
    protected Label _listTip;
    protected PushButton _deleteBtn;
    protected PushButton _listBtn;
    protected PushButton _giftBtn;

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync)
        ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
}
