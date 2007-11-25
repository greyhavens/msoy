//
// $Id$

package client.inventory;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.editem.EditorHost;
import client.editem.ItemEditor;
import client.item.BaseItemDetailPanel;
import client.shell.Application;
import client.shell.CShell;
import client.shell.Page;
import client.util.ClickCallback;
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
    public ItemDetailPanel (ItemDetail detail, ItemPanel panel)
    {
        super(detail);
        _panel = panel;

        // if this item supports sub-items, add a tab for those item types
        byte[] types = _item.getSubTypes();
        if (types.length > 0) {
            for (int ii = 0; ii < types.length; ii++) {
                addTabBelow(CInventory.dmsgs.getString("pItemType" + types[ii]),
                            new SubItemPanel(types[ii], _item, panel), false);
            }
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
            _listTip.setText(CInventory.msgs.detailUplistTip());
            _listBtn.setText(CInventory.msgs.detailUplist());
        }
    }

    // @Override // BaseItemDetailPanel
    protected void createInterface (VerticalPanel details)
    {
        super.createInterface(details);

        ItemUtil.addItemSpecificButtons(_item, _buttons);

        // TODO: this may not be necessary if this panel is only shown to the owner, but currently
        // anyone can see it by viewing the item details.
        if (_item.ownerId == CShell.getMemberId()) {
            addOwnerButtons();
        }

        // TODO: When catalog browsing is fully URL-friendly, browsing catalog by creator from here
        // will be straightforward
        /*_creator.setMember(_detail.creator, new PopupMenu() {
            protected void addMenuItems () {
                this.addMenuItem(CInventory.imsgs.viewProfile(), new Command() {
                    public void execute () {
                        Application.go(Page.PROFILE, "" + _detail.creator.getMemberId());
                    }
                });
                this.addMenuItem(CInventory.imsgs.browseCatalogFor(), new Command() {
                    public void execute () {
                        // TODO
                    }
                });
            }
        });*/
    }

    // @Override // BaseItemDetailPanel
    protected boolean allowAvatarScaleEditing ()
    {
        return (_item.ownerId == CShell.getMemberId());
    }

    protected void addOwnerButtons ()
    {
        Button button = new Button(CInventory.msgs.detailDelete());
        new ClickCallback(button) {
            public boolean callService () {
                CInventory.itemsvc.deleteItem(CInventory.ident, _item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                _panel.itemDeleted(_item);
                MsoyUI.info(CInventory.msgs.msgItemDeleted());
                History.back(); // back up to the page that contained the item
                return false;
            }
        };
        _buttons.add(button);

        if (_item.sourceId == 0) {
            button = new Button(CInventory.msgs.detailEdit());
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor = ItemEditor.createItemEditor(_item.getType(), _panel);
                    editor.setItem(_item);
                    editor.show();
                }
            });
            _buttons.add(button);
        }

        if (_item.catalogId != 0 || _item.sourceId == 0) {
            String tip, butlbl;
            if (_item.catalogId != 0) {
                tip = CInventory.msgs.detailUplistTip();
                butlbl = CInventory.msgs.detailUplist();
            } else {
                tip = CInventory.msgs.detailListTip();
                butlbl = CInventory.msgs.detailList();
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
                buttons.add(new Button(CInventory.msgs.detailUpprice(), new ClickListener() {
                    public void onClick (Widget sender) {
                        CInventory.catalogsvc.loadListing(
                            _item.getType(), _item.catalogId, false, new MsoyCallback() {
                            public void onSuccess (Object result) {
                                new DoListItemPopup(
                                    _item, (CatalogListing)result, ItemDetailPanel.this).show();
                            }
                        });
                    }
                }));
            }
            _details.add(buttons);

// TODO: we want to handle remixing in a more sophisticated way, most likely we'll link items to a
// project where the item's source files are available (even if they're not built by swiftly) and
// where collaborators can talk about the project, etc. and where the item's remixing policy can be
// more clearly detailed and generally things can be much more sophisticated than a random remix
// button in the middle of one's inventory
//
//         } else /* TODO: if (remixable) */ {
//             _details.add(WidgetUtil.makeShim(1, 10));
//             _details.add(new Label(CInventory.msgs.detailRemixTip()));
//             button = new Button(CInventory.msgs.detailRemix());
//             new ClickCallback(button) {
//                 public boolean callService () {
//                     CInventory.itemsvc.remixItem(CInventory.ident, _item.getIdent(), this);
//                     return true;
//                 }
//                 public boolean gotResult (Object result) {
//                     MsoyUI.info(CInventory.msgs.msgItemRemixed());
//                     _panel.itemRemixed(_item, (Item) result);
//                     History.back();
//                     return false;
//                 }
//             };
//             _details.add(button);
        }
    }

    protected ItemPanel _panel;
    protected Label _listTip;
    protected Button _listBtn;
}
