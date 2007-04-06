//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;

import client.editem.ItemEditor;
import client.item.BaseItemDetailPanel;
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
    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        super.createInterface(details, controls);

        ItemUtil.addItemSpecificControls(_item, controls);

        // we'll need this now so that we can pass it to our click callbacks
        _status = new Label("");
        _status.setStyleName("Status");

        Button button;
        if (_item.parentId == 0) {
            button = new Button(CInventory.msgs.detailList(), new ClickListener() {
                public void onClick (Widget sender) {
                    new DoListItemPopup(_item).show();
                }
            });

        } else {
            button = new Button(CInventory.msgs.detailRemix());
            new ClickCallback(button) {
                public boolean callService () {
                    CInventory.itemsvc.remixItem(CInventory.creds, _item.getIdent(), this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    _parent.itemRemixed (_item, (Item) result);
                    return false;
                }
            };
        }
        controls.add(button);

        button = new Button(CInventory.msgs.detailDelete());
        new ClickCallback(button) {
            public boolean callService () {
                CInventory.itemsvc.deleteItem(CInventory.creds, _item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                _parent.itemDeleted(_item);
                return false;
            }
        };
        controls.add(button);

        if (_item.parentId == 0) {
            button = new Button(CInventory.msgs.detailEdit());
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor = ItemEditor.createItemEditor(_item.getType(), _parent);
                    editor.setItem(_item);
                    editor.show();
                }
            });
            controls.add(button);
        }

        controls.add(_status);
    }

    // @Override // BaseItemDetailPanel
    protected void returnToList ()
    {
        _parent.requestClearDetail();
    }

    protected ItemPanel _parent;
    protected Label _status;
}
