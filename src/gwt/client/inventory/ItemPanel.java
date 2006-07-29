//
// $Id$

package client.inventory;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.item.data.Item;

import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.WebCreds;

/**
 * Displays all items of a particular type in a player's inventory.
 */
public class ItemPanel extends FlowPanel
{
    public ItemPanel (WebCreds creds, ItemServiceAsync itemsvc, String type)
    {
        // setStyleName("inventory_item");
        _creds = creds;
        _itemsvc = itemsvc;
        _type = type;
    }

    protected void onLoad ()
    {
        // load the users inventory if we have no already
        if (_items == null) {
            _itemsvc.loadInventory(_creds, _type, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _items = (ArrayList)result;
                    if (_items == null || _items.size() == 0) {
                        add(new Label("You have no " + _type + " items."));
                    } else {
                        for (int ii = 0; ii < _items.size(); ii++) {
                            add(new ItemThumbnail((Item)_items.get(ii)));
                        }
                    }
                }
                public void onFailure (Throwable caught) {
                    // TODO: if ServiceException, translate
                    add(new Label("Failed to load inventory."));
                }
            });
        }
    }

    protected WebCreds _creds;
    protected ItemServiceAsync _itemsvc;
    protected String _type;
    protected ArrayList _items;
}
