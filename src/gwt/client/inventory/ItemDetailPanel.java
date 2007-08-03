//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.editem.ItemEditor;
import client.item.BaseItemDetailPanel;
import client.shell.CShell;
import client.util.ClickCallback;
import client.util.ItemUtil;

/**
 * Displays a popup detail view of an item from the user's inventory.
 */
public class ItemDetailPanel extends BaseItemDetailPanel
{
    public ItemDetailPanel (ItemDetail detail, ItemPanel parent)
    {
        super(detail);
        _parent = parent;
    }

    // @Override // BaseItemDetailPanel
    protected void createInterface (VerticalPanel details)
    {
        super.createInterface(details);

        ItemUtil.addItemSpecificButtons(_item, _buttons);

        // TODO: this may not be necessary if this panel is only shown to the owner, but currently
        // anyone can see it by viewing the item details.
        boolean userOwnsItem = (_item.ownerId == CShell.getMemberId());
        if (!userOwnsItem) {
            // don't add any more of the buttons
            return;
        }

        Button button = new Button(CInventory.msgs.detailDelete());
        new ClickCallback(button) {
            public boolean callService () {
                CInventory.itemsvc.deleteItem(CInventory.ident, _item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                _parent.itemDeleted(_item);
                return false;
            }
        };
        _buttons.add(button);

        if (_item.parentId == 0) {
            button = new Button(CInventory.msgs.detailEdit());
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor = ItemEditor.createItemEditor(_item.getType(), _parent);
                    editor.setItem(_item);
                    editor.show();
                }
            });
            _buttons.add(button);
        }

        if (_item.parentId == 0) {
            _details.add(WidgetUtil.makeShim(1, 10));
            _details.add(new Label(CInventory.msgs.detailListTip()));
            button = new Button(CInventory.msgs.detailList(), new ClickListener() {
                public void onClick (Widget sender) {
                    new DoListItemPopup(_item).show();
                }
            });
            _details.add(button);

        } else /* TODO: if (remixable) */ {
            _details.add(WidgetUtil.makeShim(1, 10));
            _details.add(new Label(CInventory.msgs.detailRemixTip()));
            button = new Button(CInventory.msgs.detailRemix());
            new ClickCallback(button) {
                public boolean callService () {
                    CInventory.itemsvc.remixItem(CInventory.ident, _item.getIdent(), this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    _parent.itemRemixed (_item, (Item) result);
                    return false;
                }
            };
            _details.add(button);
        }

        // TODO: When catalog browsing is fully URL-friendly, browsing catalog by creator from here
        // will be straightforward
        /*_creator.setMember(_detail.creator, new PopupMenu() {
            protected void addMenuItems () {
                this.addMenuItem(CInventory.imsgs.viewProfile(), new Command() {
                    public void execute () {
                        History.newItem(Application.createLinkToken("profile",
                            "" + _detail.creator.getMemberId()));
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
    protected void returnToList ()
    {
        _parent.requestClearDetail();
    }

    // @Override // BaseItemDetailPanel
    protected boolean allowAvatarScaleEditing ()
    {
        return (_item.ownerId == CShell.getMemberId());
    }

    protected ItemPanel _parent;
}
