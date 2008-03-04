//
// $Id$

package client.stuff;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
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
    public ItemDetailPanel (InventoryModels models, ItemDetail detail, ItemPanel panel)
    {
        super(detail);
        _models = models;
        _panel = panel;

// TODO
//         ItemUtil.addItemSpecificButtons(_item, _buttons);

        // only add owner buttons for owners and admins
        if (_item.ownerId == CShell.getMemberId() || CShell.isAdmin()) {
            addOwnerButtons();
        }

        // TODO: When catalog browsing is fully URL-friendly, browsing catalog by creator from here
        // will be straightforward
        /*_creator.setMember(_detail.creator, new PopupMenu() {
            protected void addMenuItems () {
                this.addMenuItem(CStuff.imsgs.viewProfile(), new Command() {
                    public void execute () {
                        Application.go(Page.PEOPLE, "" + _detail.creator.getMemberId());
                    }
                });
                this.addMenuItem(CStuff.imsgs.browseCatalogFor(), new Command() {
                    public void execute () {
                        // TODO
                    }
                });
            }
        });*/

        // if this item supports sub-items, add a tab for those item types
        SubItem[] types = _item.getSubTypes();
        for (int ii = 0; ii < types.length; ii++) {
            // if this is not an original item, only show salable subtypes
            if (_item.sourceId != 0 && !types[ii].isSalable()) {
                continue;
            }
            addTabBelow(CStuff.dmsgs.getString("pItemType" + types[ii].getType()),
                        new SubItemPanel(models, types[ii].getType(), _item, panel), false);
        }
    }

    /**
     * Returns true if we're displaying the specified item.
     */
    public boolean isShowing (ItemIdent ident)
    {
        return (_item != null) && _item.getIdent().equals(ident);
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
        Button button;

        // don't show delete to anyone but the owner
        if (_item.ownerId == CShell.getMemberId()) {
            button = new Button(CStuff.msgs.detailDelete());
            new ClickCallback(button, CStuff.msgs.detailConfirmDelete()) {
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
// TODO
//             _buttons.add(button);
        }

        if (_item.sourceId == 0) {
            button = new Button(CStuff.msgs.detailEdit());
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    CStuff.editItem(_item.getType(), _item.itemId);
                }
            });
// TODO
//             _buttons.add(button);
        }

        if (_item.ownerId == CShell.getMemberId() && FlashClients.clientExists()) {
            _details.add(WidgetUtil.makeShim(1, 10));
            _details.add(new ItemActivator(_item));
        }

        if (_item.catalogId != 0 || _item.sourceId == 0) {
            String tip, butlbl;
            if (_item.catalogId != 0) {
                tip = CStuff.msgs.detailUplistTip();
                butlbl = CStuff.msgs.detailUplist();
            } else {
                tip = CStuff.msgs.detailListTip();
                butlbl = CStuff.msgs.detailList();
            }
            _details.add(WidgetUtil.makeShim(1, 10));
            _details.add(_listTip = new Label(tip));

            // add a button for listing or updating the item
            RowPanel buttons = new RowPanel();
            buttons.add(_listBtn = new Button(butlbl, new ClickListener() {
                public void onClick (Widget sender) {
                    new DoListItemPopup(_item, null, ItemDetailPanel.this).show();
                }
            }));

            boolean salable = (!(_item instanceof SubItem) || ((SubItem)_item).isSalable());
            if (_item.catalogId != 0 && salable) {
                // add a button for repricing the listing
                buttons.add(new Button(CStuff.msgs.detailUpprice(), new ClickListener() {
                    public void onClick (Widget sender) {
                        CStuff.catalogsvc.loadListing(
                            CStuff.ident, _item.getType(), _item.catalogId, new MsoyCallback() {
                            public void onSuccess (Object result) {
                                new DoListItemPopup(
                                    _item, (CatalogListing)result, ItemDetailPanel.this).show();
                            }
                        });
                    }
                }));
            }
            _details.add(buttons);
        }


        // TODO: enable remixing for everyone
        boolean remixable = (_item.getFurniMedia().mimeType == MediaDesc.APPLICATION_ZIP) &&
            CShell.isSupport();
        if (remixable) {
            _details.add(WidgetUtil.makeShim(1, 10));
            _details.add(new Label(CStuff.msgs.detailRemixTip()));
            button = new Button(CStuff.msgs.detailRemix());
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    CStuff.remixItem(_item.getType(), _item.itemId);
                }
            });
//            new ClickCallback(button) {
//                public boolean callService () {
//                    CStuff.itemsvc.remixItem(CStuff.ident, _item.getIdent(), this);
//                    return true;
//                }
//                public boolean gotResult (Object result) {
//                    MsoyUI.info(CStuff.msgs.msgItemRemixed());
//                    _panel.itemRemixed(_item, (Item) result);
//                    History.back();
//                    return false;
//                }
//            };
            _details.add(button);
        }
    }

    protected InventoryModels _models;
    protected ItemPanel _panel;
    protected Label _listTip;
    protected Button _listBtn;
}
