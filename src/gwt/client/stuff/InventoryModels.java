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

import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.Predicate;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.stuff.gwt.StuffService.DetailOrIdent;

/**
 * Maintains information on our member's inventory.
 */
public class InventoryModels
    implements ItemUsageListener, ItemDataModel
{
    public void startup ()
    {
        FlashEvents.addListener(this);
    }

    public void shutdown ()
    {
        FlashEvents.removeListener(this);
    }

    // from interface ItemDataModel
    public void loadModel (byte type, int suiteId, final AsyncCallback<DataModel<Item>> cb)
    {
        final Key key = new Key(type, suiteId);
        SimpleDataModel<Item> model = _models.get(key);
        if (model != null) {
            cb.onSuccess(model);
            return;
        }

        _stuffsvc.loadInventory(type, suiteId, new AsyncCallback<List<Item>>() {
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

    // from interface ItemDataModel
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

    // from interface ItemDataModel
    public void itemUpdated (Item item)
    {
        for (Map.Entry<Key, SimpleDataModel<Item>> entry : _models.entrySet()) {
            Key key = entry.getKey();
            if (key.matches(item)) {
                entry.getValue().updateItem(item);
            }
        }
    }

    // from interface ItemDataModel
    public void itemDeleted (Item item)
    {
        int suiteId = (item instanceof SubItem) ? ((SubItem)item).suiteId : 0;
        DataModel<Item> model = getModel(item.getType(), suiteId);
        if (model != null) {
            model.removeItem(item);
        }
        // we may have a non-suite-specific model for subitems in addition to our per-suite models
        if (suiteId != 0) {
            model = getModel(item.getType(), 0);
            if (model != null) {
                model.removeItem(item);
            }
        }
    }

    /**
     * Gets the default item type to use in the case that one wasn't specified.
     */
    public byte getDefaultItemType ()
    {
        return Item.AVATAR;
    }

    /**
     * Loads the item detail from the service and feeds the results to the given callback.
     */
    public void loadItemDetail (ItemIdent ident, AsyncCallback<DetailOrIdent> resultCallback)
    {
        _stuffsvc.loadItemDetail(ident, resultCallback);
    }

    /**
     * Returns the correct model for a PagedGrid based off the given item type.
     */
    // from interface ItemDataModel
    public DataModel<Item> getGridModel (byte itemType)
    {
        return getModel(itemType, 0);
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

    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
}
