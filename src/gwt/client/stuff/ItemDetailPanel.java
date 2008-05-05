//
// $Id$

package client.stuff;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.item.BaseItemDetailPanel;
import client.item.ItemActivator;
import client.shell.Application;
import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.ItemUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays a popup detail view of an item from the user's inventory.
 */
public class ItemDetailPanel extends BaseItemDetailPanel
    implements DoListItemPopup.ListedListener
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
            addTabBelow(CStuff.dmsgs.getString("pItemType" + types[ii].getType()),
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

    // @Override // BaseItemDetailPanel
    protected boolean allowAvatarScaleEditing ()
    {
        return (_item.ownerId == CShell.getMemberId());
    }

    // @Override // BaseItemDetailPanel
    protected void onUpClicked ()
    {
        CStuff.viewParent(_item);
    }

    protected void addOwnerButtons ()
    {
        if (_item.ownerId == CShell.getMemberId() && FlashClients.clientExists()) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(new ItemActivator(_item, false));
        }

        // figure out a few pieces of common info
        boolean original = (_item.sourceId == 0);
        boolean catalogOriginal = _item.isCatalogOriginal();
        MediaDesc primaryMedia = _item.getPrimaryMedia();
        boolean remixable = (primaryMedia != null) && primaryMedia.isRemixable();
        boolean used = (_item.used != Item.UNUSED);

        RowPanel buttons = new RowPanel();

        // if this item is in use, mention that
        if (used) {
            // tell the user that the item is in use, and maybe where
            String msg;
            switch (_item.used) {
            case Item.USED_AS_FURNITURE:
            case Item.USED_AS_PET:
            case Item.USED_AS_BACKGROUND:
                msg = CStuff.msgs.detailInUseInRoom("" + _item.location, _detail.useLocation);
                break;

            default:
                msg = CStuff.msgs.detailInUse();
                break;
            }
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(new HTML(msg));
        }

        // add a button for deleting this item
        PushButton delete = MsoyUI.createButton(
            MsoyUI.LONG_THIN, CStuff.msgs.detailDelete(), null);
        delete.setEnabled(!used);
        new ClickCallback(delete, CStuff.msgs.detailConfirmDelete()) {
            public boolean callService () {
                CStuff.itemsvc.deleteItem(CStuff.ident, _item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                // remove the item from our cached models
                int suiteId = (_item instanceof SubItem) ? ((SubItem)_item).suiteId : 0;
                DataModel model = _models.getModel(_item.getType(), suiteId);
                if (model != null) {
                    model.removeItem(_item);
                }
                MsoyUI.info(CStuff.msgs.msgItemDeleted());
                History.back(); // back up to the page that contained the item
                return false;
            }
        };
        buttons.add(delete);

        // add a button for editing this item, if it's an original
        if (original) {
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
            new ClickCallback(revert, CStuff.msgs.detailConfirmRevert()) {
                public boolean callService () {
                    CStuff.itemsvc.revertRemixedClone(CStuff.ident, _item.getIdent(), this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    Item item = (Item) result;
                    _models.updateItem(item);
                    _item = item;
                    _detail.item = item;

                    // redisplay the item detail with the reverted version.
                    Application.go(Page.STUFF, Args.compose(new String[]
                        { "d", "" + item.getType(), "" + item.itemId, "revert" }));
                    return false;
                }
            };
            buttons.add(revert);
        }

        // add our delete/edit buttons if we have them
        if (buttons.getWidgetCount() > 0) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(buttons);
        }

        // if this item is listed in the catalog or listable, add a UI for that
        if (catalogOriginal || _item.sourceId == 0) {
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
                buttons.add(MsoyUI.createButton(MsoyUI.LONG_THIN, butlbl, new ClickListener() {
                    public void onClick (Widget sender) {
                        CStuff.catalogsvc.loadListing(
                            CStuff.ident, _item.getType(), _item.catalogId, new MsoyCallback() {
                            public void onSuccess (Object result) {
                                DoListItemPopup.show(
                                    _item, (CatalogListing)result, ItemDetailPanel.this);
                            }
                        });
                    }
                }));
            }
            _details.add(WidgetUtil.makeShim(10, 5));
            _details.add(buttons);
        }

        // if this item is giftable, add a UI for that
        if (!catalogOriginal && !used) {
            _details.add(WidgetUtil.makeShim(10, 10));
            _details.add(new Label(CStuff.msgs.detailGiftTip()));
            _details.add(WidgetUtil.makeShim(10, 5));
            String[] args = new String[] { "w", "i", ""+_item.getType(), ""+_item.itemId };
            ClickListener onClick = Application.createLinkListener(Page.MAIL, Args.compose(args));
            _details.add(MsoyUI.createButton(MsoyUI.LONG_THIN, CStuff.msgs.detailGift(), onClick));
        }

        buttons = new RowPanel();

        // add a button for renaming
        if (!original) {
            PushButton rename = MsoyUI.createButton(MsoyUI.LONG_THIN, CStuff.msgs.detailRename(), null);
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
    }

    protected InventoryModels _models;
    protected Label _listTip;
    protected PushButton _listBtn;
}
