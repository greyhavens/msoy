//
// $Id$

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.GameItem;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.item.BaseItemDetailPanel;
import client.item.ConfigButton;
import client.item.DoListItemPopup;
import client.item.ItemActivator;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.Link;
import client.util.NaviUtil;
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
        _models = models;

        // create our main interface
        init(detail);

        if (isRemixable()) {
            HorizontalPanel extras = new HorizontalPanel();
            extras.setStyleName("Extras");
            extras.add(new ConfigButton(false, _msgs.detailRemix(),
                NaviUtil.onRemixItem(_item.getType(), _item.itemId)));
            _indeets.add(extras);
        }

        // only add owner buttons for owners and support
        if (userOwnsItem() || CShell.isSupport()) {
            addOwnerButtons();
        }
    }

    // from DoListItemPopup.ListedListener
    public void itemListed (Item item, boolean updated)
    {
        // reload the page
        Link.replace(Pages.STUFF, "d", _item.getType(), _item.itemId);
    }

    // from interface ItemUsageListener
    public void itemUsageChanged (ItemUsageEvent event)
    {
        if ((event.getItemTypeByte() == _item.getType().toByte()) && (event.getItemId() == _item.itemId)) {
            adjustForUsage();
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

    /**
     * Returns true if the user created this specific item.
     */
    protected boolean userCreatedItem ()
    {
        return (_item.creatorId == CShell.getMemberId());
    }

    @Override // BaseItemDetailPanel
    protected boolean isRemixable ()
    {
        // for us to be able to save the remix, it needs to be....
        return super.isRemixable() &&
            // a clone            OR created by us     OR we rule
            ((_item.sourceId != 0) || userCreatedItem() || CShell.isSupport());
    }

    @Override // BaseItemDetailPanel
    protected void onUpClicked ()
    {
        Link.go(Pages.STUFF, _item.getType(), _item.ownerId);
    }

    protected void addOwnerButtons ()
    {
        // figure out a few pieces of common info
        boolean original = (_item.sourceId == 0);
        boolean listedOriginal = _item.isListedOriginal();
        boolean canEditAndList = userCreatedItem() || CShell.isSupport();
        boolean remixable = isRemixable();
        boolean used = _item.used.forAnything();
        boolean isRoomType = _item.getType().isRoomType();

        // add a button for deleting this item
        RowPanel buttons = new RowPanel();
        _deleteBtn = MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.detailDelete(), null);
        createDeleteCallback(_deleteBtn);
        buttons.add(_deleteBtn);

        // add a button for editing this item, if it's an original
        if (original) {
            if (canEditAndList) {
                buttons.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.detailEdit(),
                    NaviUtil.onEditItem(_item.getType(), _item.itemId)));
            }
        } else {
            // add a button for renaming
            PushButton rename = MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.detailRename(), null);
            buttons.add(rename);
            new RenameHandler(rename, _item, _models);
        }

        _details.add(WidgetUtil.makeShim(10, 10));
        _details.add(buttons);

        // if this item is in use, mention that
        if (used) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(MsoyUI.createHTML(getUsageMessage(), null));
        }

        // add an activator for this item (and tuck it up next to the in-use message if we have one)
        if (isRoomType && userOwnsItem() && FlashClients.clientExists()) {
            if (!used) {
                // if we don't already have a usage message, add a not-in-use message because that
                // nicely explains the button we're about to add
                _details.add(WidgetUtil.makeShim(10, 10));
                _details.add(MsoyUI.createHTML(_msgs.detailNotInUse(), null));
            }
            _details.add(WidgetUtil.makeShim(10, 5));
            _details.add(new ItemActivator(_item, true));
        }

        // if this item is listed in the catalog or listable, add a UI for that
        if (_item.getType().isShopType() && canEditAndList && (listedOriginal || _item.sourceId == 0)) {
            _details.add(WidgetUtil.makeShim(10, 10));

            // this handles both creating and updating of listings
            ClickHandler onDoList = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    // TODO RELISTING
                    DoListItemPopup.show(_item, null, ItemDetailPanel.this);
                }
            };

            // if the item is listed, add a biggish UI for updating the listing and pricing
            if (listedOriginal) {
                _details.add(createTipLink(_msgs.detailUplistTip(), _msgs.detailViewListing(),
                                           Pages.SHOP, "l", _item.getType(), _item.catalogId));

                // add a button for listing or updating the item
                buttons = new RowPanel();
                buttons.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.detailUplist(), onDoList));

                if (listedOriginal && _item.isSalable()) {
                    // add a button for repricing the listing
                    PushButton button =
                        MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.detailUpprice(), null);
                    new ClickCallback<CatalogListing>(button) {
                        @Override protected boolean callService () {
                            _catalogsvc.loadListing(_item.getType(), _item.catalogId, false, this);
                            return true;
                        }
                        @Override protected boolean gotResult (CatalogListing listing) {
                            DoListItemPopup.show(_item, listing, ItemDetailPanel.this);
                            return true;
                        }
                    };
                    buttons.add(button);
                }
                _details.add(WidgetUtil.makeShim(10, 5));
                _details.add(buttons);

            } else if (_item.isCatalogMaster()) {
                _details.add(createTipLink(_msgs.detailMasterTip(), _msgs.detailViewListing(),
                    Pages.SHOP, "l", _item.getType(), _item.catalogId));

            } else {
                // otherwise add a subtler UI letting them know the item can be listed
                _details.add(createTipLink(_msgs.detailListTip(), _msgs.detailList(), onDoList));
            }
        }

        // if remixable, add a button for that
        if (remixable) {
            _details.add(WidgetUtil.makeShim(10, 5));
            buttons = new RowPanel();
            // if it's a remixed clone, add a button for reverting
            if (!original) {
                boolean mixed = _item.isAttrSet(Item.ATTR_REMIXED_CLONE);
                boolean newOrigAvail = _item.isAttrSet(Item.ATTR_ORIGINAL_UPDATED);
                String lbl = newOrigAvail ? _msgs.detailUpdate() : _msgs.detailRevert();
                PushButton revert = MsoyUI.createButton(MsoyUI.LONG_THIN, lbl, null);
                if (!newOrigAvail && !mixed) { // if the item is up-to-date, disable the button
                    revert.setEnabled(false);
                    revert.setTitle(_msgs.detailRevertNotNeeded());
                } else {
                    _details.add(WidgetUtil.makeShim(10, 10));
                    _details.add(new Label(newOrigAvail ? _msgs.detailUpdateTip() :
                                           _msgs.detailRevertTip()));
                }
                createRevertCallback(revert);
                buttons.add(revert);
            }
            _details.add(buttons);
        }

        // if this item is giftable, add a UI for that
        if (!listedOriginal) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(_giftBits = new FlowPanel());
        }

        // if this item is a game item, provide a link to its game
        if (_item instanceof GameItem) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(createTipLink(_msgs.detailCanViewGame(), _msgs.detailViewGame(),
                                       Pages.GAMES, "d", ((GameItem)_item).gameId));
        }

        // if this was purchased from the shop, add a link to the original listing
        if (_item.isCatalogClone()) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(createTipLink(_msgs.detailCanViewShop(), _msgs.detailViewShop(),
                                       Pages.SHOP, "l", _item.getType(), _item.catalogId));
        }

        adjustForUsage();
    }

    protected void adjustForUsage ()
    {
        boolean unused = !_item.used.forAnything();
        _deleteBtn.setEnabled(unused);
        if (_giftBits != null) {
            _giftBits.clear();
            if (unused) {
                _giftBits.add(createTipLink(_msgs.detailGiftTip(), _msgs.detailGift(),
                                            Pages.MAIL, "w", "i", _item.getType(), _item.itemId));
            } else {
                _giftBits.add(new Label(_msgs.detailGiftDisabled()));
            }
        }
    }

    /**
     * Get the usage message for an item, possibly detailing where it's being used.
     */
    protected String getUsageMessage ()
    {
        switch (_item.used) {
        case FURNITURE:
        case PET:
        case BACKGROUND:
            return _msgs.detailInUseInRoom("" + _item.location, _detail.useLocation);

        default:
            return _msgs.detailInUse();
        }
    }

    /**
     * Create a click callback for deleting the item.
     */
    protected void createDeleteCallback (HasClickHandlers trigger)
    {
        new ClickCallback<Void>(trigger, _msgs.detailConfirmDelete()) {
            @Override protected boolean callService () {
                _stuffsvc.deleteItem(_item.getIdent(), this);
                return true;
            }
            @Override protected boolean gotResult (Void result) {
                // remove the item from our data model
                _models.itemDeleted(_item);

                MsoyUI.info(_msgs.msgItemDeleted());
                History.back(); // back up to the page that contained the item
                return false;
            }
        };
    }

    /**
     * Create a click callback for reverting the item.
     */
    protected void createRevertCallback (HasClickHandlers trigger)
    {
        new ClickCallback<Item>(trigger, _msgs.detailConfirmRevert()) {
            @Override protected boolean callService () {
                _stuffsvc.revertRemixedClone(_item.getIdent(), this);
                return true;
            }
            @Override protected boolean gotResult (Item item) {
                _models.itemUpdated(item);
                _item = item;
                _detail.item = item;

                // redisplay the item detail with the reverted version.
                Link.replace(Pages.STUFF, "d", item.getType(), item.itemId, "revert");
                return false;
            }
        };
    }

    /**
     * Creates a line of text followed inline by a link.
     */
    protected FlowPanel createTipLink (String tip, String link, ClickHandler onClick)
    {
        FlowPanel row = new FlowPanel();
        row.add(new InlineLabel(tip, true, false, true));
        row.add(MsoyUI.createActionLabel(link, "inline", onClick));
        return row;
    }

    /**
     * Creates a line of text followed inline by a link.
     */
    protected FlowPanel createTipLink (String tip, String link, Pages page, Object... args)
    {
        FlowPanel row = new FlowPanel();
        row.add(new InlineLabel(tip, true, false, true));
        row.add(Link.create(link, page, args));
        return row;
    }

    protected InventoryModels _models;
    protected PushButton _deleteBtn;
    protected FlowPanel _giftBits;

    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = GWT.create(CatalogService.class);
    protected static final StuffServiceAsync _stuffsvc = GWT.create(StuffService.class);
}
