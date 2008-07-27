//
// $Id$

package client.stuff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import client.util.ServiceUtil;

import client.util.events.FlashEvents;
import client.util.events.ItemUsageEvent;
import client.util.events.ItemUsageListener;

import com.threerings.gwt.util.Predicate;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;

/**
 * Maintains information on our member's inventory.
 */
public class InventoryModels
    implements ItemUsageListener
{
    public void startup ()
    {
        FlashEvents.addListener(this);
    }

    public void shutdown ()
    {
        FlashEvents.removeListener(this);
    }

    public void loadModel (byte type, int suiteId, final AsyncCallback<SimpleDataModel<Item>> cb)
    {
        final Key key = new Key(type, suiteId);
        SimpleDataModel<Item> model = _models.get(key);
        if (model != null) {
            cb.onSuccess(model);
            return;
        }

        _itemsvc.loadInventory(CStuff.ident, type, suiteId, new AsyncCallback<List<Item>>() {
            public void onSuccess (List<Item> result) {
                SimpleDataModel<Item> model = new SimpleDataModel<Item>(result);
                _models.put(key, model);
                cb.onSuccess(model);
            }
            public void onFailure (Throwable caught) {
                cb.onFailure(caught);
            }
        });
    }

    public SimpleDataModel<Item> getModel (byte type, int suiteId)
    {
        return _models.get(new Key(type, suiteId));
    }

    public Item findItem (byte type, final int itemId)
    {
        Predicate<Item> itemP = new Predicate<Item>() {
            public boolean isMatch (Item item) {
                return item.itemId == itemId;
            }
        };
        for (Map.Entry<Key, SimpleDataModel<Item>> entry : _models.entrySet()) {
            Key key = entry.getKey();
            if (key.type == type) {
                SimpleDataModel<Item> model = entry.getValue();
                Item item = model.findItem(itemP);
                if (item != null) {
                    return item;
                }
            }
        }
        return null;
    }

    public void updateItem (Item item)
    {
        for (Map.Entry<Key, SimpleDataModel<Item>> entry : _models.entrySet()) {
            Key key = entry.getKey();
            if (key.matches(item)) {
                entry.getValue().updateItem(item);
            }
        }
    }

    // from interface ItemUsageListener
    public void itemUsageChanged (ItemUsageEvent event)
    {
        Item item = findItem(event.getItemType(), event.getItemId());
        if (item != null) {
            item.used = event.getUsage();
            item.location = event.getLocation();
            // TODO: update lastTouched time locally?

            // TODO: right now, the ItemActivators listen to the usageChangedEvent just
            // like we do, but perhaps this class should dispatch a more generic itemChanged
            // event, and have the ItemActivators respond to that.
        }
    }

    protected static class Key {
        public final byte type;
        public final int suiteId;

        public Key (byte type, int suiteId) {
            this.type = type;
            this.suiteId = suiteId;
        }

        public boolean matches (Item item) {
            return item.getType() == type &&
                suiteId == (item instanceof SubItem ? ((SubItem)item).suiteId : 0);
        }

        public int hashCode() {
            return type ^ suiteId;
        }

        public boolean equals (Object other) {
            Key okey = (Key)other;
            return type == okey.type && suiteId == okey.suiteId;
        }
    }

    protected Map<Key, SimpleDataModel<Item>> _models = new HashMap<Key, SimpleDataModel<Item>>();

    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync)
        ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
}
