//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;

import client.editem.ItemEditor;
import client.item.BaseItemDetailPopup;
import client.util.ClickCallback;
import client.util.ItemUtil;

/**
 * Displays a popup detail view of an item from the user's inventory.
 */
public class ItemDetailPopup extends BaseItemDetailPopup
{
    public ItemDetailPopup (Item item, ItemPanel parent)
    {
        super(item);
        _parent = parent;
    }

    // @Override // BaseItemDetailPopup
    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        super.createInterface(details, controls);

        ItemUtil.addItemSpecificControls(_item, controls, this);

        // we'll need this now so that we can pass it to our click callbacks
        _status = new Label("");
        _status.setStyleName("Status");

        Button button;
        if (_item.parentId == 0) {
            button = new Button(CInventory.msgs.detailList(), new ClickListener() {
                public void onClick (Widget sender) {
                    new DoListItemPopup(_item).show();
                    hide();
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
                    hide();
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
                hide();
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
                    hide();
                }
            });
            controls.add(button);
        }

        controls.add(_status);
    }

    protected ItemPanel _parent;
    protected Label _status;
}
