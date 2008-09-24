//
// $Id$

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;

import client.item.BaseItemDetailPanel;
import client.item.DoListItemPopup;
import client.item.ItemActivator;
import client.item.RemixableLabel;
import client.shell.Args;
import client.shell.CShell;
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.Link;
import client.util.NaviUtil;
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
    public ItemDetailPanel (ItemDataModel model, ItemDetail detail)
    {
        super(detail);
        _model = model;

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
                        new SubItemPanel(_model, types[ii].getType(), _item), false);
        }
    }

    // from DoListItemPopup.ListedListener
    public void itemListed (Item item, boolean updated)
    {
        // reload the page
        Link.replace(Pages.STUFF, Args.compose("d", _item.getType(), _item.itemId));
    }

    // from interface ItemUsageListener
    public void itemUsageChanged (ItemUsageEvent event)
    {
        if ((event.getItemType() == _item.getType()) && (event.getItemId() == _item.itemId)) {
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
            new RenameHandler(rename, _item, _model);
        }

        _details.add(WidgetUtil.makeShim(10, 10));
        _details.add(buttons);

        // if this item is in use, mention that
        if (used) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(MsoyUI.createHTML(getUsageMessage(), null));
        }

        // add an activator for this item (and tuck it up next to the in-use message if we have one)
        if (_item.ownerId == memberId && FlashClients.clientExists()) {
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
        if (canEditAndList && (catalogOriginal || _item.sourceId == 0)) {
            _details.add(WidgetUtil.makeShim(10, 10));

            // this handles both creating and updating of listings
            ClickListener onDoList = new ClickListener() {
                public void onClick (Widget sender) {
                    DoListItemPopup.show(_item, null, ItemDetailPanel.this);
                }
            };

            // if the item is listed, add a biggish UI for updating the listing and pricing
            if (catalogOriginal) {
                String args = Args.compose("l", _item.getType(), _item.catalogId);
                _details.add(createTipLink(_msgs.detailUplistTip(), _msgs.detailViewListing(),
                                           Link.createListener(Pages.SHOP, args)));

                // add a button for listing or updating the item
                buttons = new RowPanel();
                buttons.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.detailUplist(), onDoList));

                boolean salable = (!(_item instanceof SubItem) || ((SubItem)_item).isSalable());
                if (catalogOriginal && salable) {
                    // add a button for repricing the listing
                    PushButton button =
                        MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.detailUpprice(), null);
                    new ClickCallback<CatalogListing>(button) {
                        public boolean callService () {
                            _catalogsvc.loadListing(_item.getType(), _item.catalogId, this);
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

            } else {
                // otherwise add a subtler UI letting them know the item can be listed
                _details.add(createTipLink(_msgs.detailListTip(), _msgs.detailList(), onDoList));
            }
        }

        // if remixable, add a button for that
        if (remixable) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(new RemixableLabel());
            _details.add(WidgetUtil.makeShim(10, 5));

            buttons = new RowPanel();
            buttons.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.detailRemix(),
                                            NaviUtil.onRemixItem(_item.getType(), _item.itemId)));

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
        if (!catalogOriginal) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(_giftBits = new FlowPanel());
        }

        adjustForUsage();
    }

    protected void adjustForUsage ()
    {
        boolean unused = (_item.used == Item.UNUSED);
        _deleteBtn.setEnabled(unused);
        if (_giftBits != null) {
            _giftBits.clear();
            if (unused) {
                String args = Args.compose("w", "i", _item.getType(), _item.itemId);
                ClickListener onClick = Link.createListener(Pages.MAIL, args);
                _giftBits.add(createTipLink(_msgs.detailGiftTip(), _msgs.detailGift(), onClick));
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
        case Item.USED_AS_FURNITURE:
        case Item.USED_AS_PET:
        case Item.USED_AS_BACKGROUND:
            return _msgs.detailInUseInRoom("" + _item.location, _detail.useLocation);

        default:
            return _msgs.detailInUse();
        }
    }

    /**
     * Create a click callback for deleting the item.
     */
    protected void createDeleteCallback (SourcesClickEvents trigger)
    {
        new ClickCallback<Void>(trigger, _msgs.detailConfirmDelete()) {
            public boolean callService () {
                _stuffsvc.deleteItem(_item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Void result) {
                // remove the item from our data model
                _model.itemDeleted(_item);

                MsoyUI.info(_msgs.msgItemDeleted());
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
        new ClickCallback<Item>(trigger, _msgs.detailConfirmRevert()) {
            public boolean callService () {
                _stuffsvc.revertRemixedClone(_item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Item item) {
                _model.itemUpdated(item);
                _item = item;
                _detail.item = item;

                // redisplay the item detail with the reverted version.
                Link.replace(Pages.STUFF, Args.compose("d", item.getType(), item.itemId, "revert"));
                return false;
            }
        };
    }

    /**
     * Creates a line of text followed inline by a link.
     */
    protected FlowPanel createTipLink (String tip, String link, ClickListener onClick)
    {
        FlowPanel row = new FlowPanel();
        row.add(new InlineLabel(tip, true, false, true));
        row.add(MsoyUI.createActionLabel(link, "inline", onClick));
        return row;
    }

    protected ItemDataModel _model;
    protected PushButton _deleteBtn;
    protected FlowPanel _giftBits;

    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
}
