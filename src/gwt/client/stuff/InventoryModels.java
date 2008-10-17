//
// $Id$

package client.stuff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.Predicate;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.stuff.gwt.StuffService.DetailOrIdent;
import com.threerings.msoy.web.gwt.Args;

import client.util.ServiceUtil;
import client.util.events.FlashEvents;
import client.util.events.ItemUsageEvent;
import client.util.events.ItemUsageListener;

/**
 * Maintains information on our member's inventory.
 */
public class InventoryModels
    implements ItemUsageListener
{
    public static class Stuff extends SimpleDataModel<Item>
    {
        public final byte type;
        public final int suiteId;
        public final String query;

        public Stuff (List<Item> items, Key key) {
            this(items, key.type, key.suiteId, key.query);
        }

        public Stuff (List<Item> items, byte type, int suiteId, String query) {
            super(items);
            this.type = type;
            this.suiteId = suiteId;
            this.query = query;
        }

        public boolean matches (Item item) {
            return item.getType() == type &&
                suiteId == (item instanceof SubItem ? ((SubItem)item).suiteId : 0);
        }

        public String makeArgs (int page) {
            return (query == null) ? Args.compose(type, page) : Args.compose(type, page, query);
        }

        public String toString () {
            return "[type=" + type + ", suiteId=" + suiteId + ", query=" + query + "]";
        }

        protected SimpleDataModel<Item> createFilteredModel (List<Item> items) {
            return new Stuff(items, type, suiteId, query);
        }
    }

    public void startup ()
    {
        FlashEvents.addListener(this);
    }

    public void shutdown ()
    {
        FlashEvents.removeListener(this);
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
     * Looks up the data model with the specified parameters. Returns null if we have none.
     */
    public SimpleDataModel<Item> getModel (byte type, String query)
    {
        return _models.get(new Key(type, 0, query));
    }

    /**
     * Loads a data model for items of the specified type and matching the optionally supplied
     * query.
     */
    public void loadModel (byte type, String query, AsyncCallback<DataModel<Item>> cb)
    {
        loadModel(new Key(type, 0, query), cb);
    }

    /**
     * Loads a data model for sub-items of the specified type with the specified parent.
     */
    public void loadSubModel (byte type, int suiteId, AsyncCallback<DataModel<Item>> cb)
    {
        loadModel(new Key(type, suiteId, null), cb);
    }

    /**
     * Used to try to find an item in the local data model/cache. If this returns null, then the
     * item will get loaded from the server.
     */
    public Item findItem (byte type, final int itemId)
    {
        Predicate<Item> itemP = new Predicate<Item>() {
            public boolean isMatch (Item item) {
                return item.itemId == itemId;
            }
        };
        for (Map.Entry<Key, Stuff> entry : _models.entrySet()) {
            Key key = entry.getKey();
            Stuff model = entry.getValue();
            if (key.type == type && model.query == null) {
                Item item = model.findItem(itemP);
                if (item != null) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * A callback to let the model know that the item was renamed, etc.
     */
    public void itemUpdated (Item item)
    {
        for (Stuff model : _models.values()) {
            if (model.matches(item)) {
                model.updateItem(item);
            }
        }
    }

    /**
     * Let the model know that the item was deleted.
     */
    public void itemDeleted (Item item)
    {
        for (Stuff model : _models.values()) {
            if (model.matches(item)) {
                model.removeItem(item);
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

    protected void loadModel (final Key key, final AsyncCallback<DataModel<Item>> cb)
    {
        Stuff model = _models.get(key);
        if (model != null) {
            cb.onSuccess(model);
            return;
        }

        AsyncCallback<List<Item>> callback = new AsyncCallback<List<Item>>() {
            public void onSuccess (List<Item> result) {
                Stuff model = new Stuff(result, key);
                _models.put(key, model);
                cb.onSuccess(model);
            }
            public void onFailure (Throwable caught) {
                cb.onFailure(caught);
            }
        };
        if (key.suiteId != 0) {
            _stuffsvc.loadSubInventory(key.type, key.suiteId, callback);
        } else {
            _stuffsvc.loadInventory(key.type, key.query, callback);
        }
    }

    protected static class Key {
        public final byte type;
        public final int suiteId;
        public final String query;

        public Key (byte type, int suiteId, String query) {
            this.type = type;
            this.suiteId = suiteId;
            this.query = (query != null && query.length() == 0) ? null : query;
        }

        public int hashCode() {
            return type ^ suiteId ^ (query == null ? 0 : query.hashCode());
        }

        public boolean equals (Object other) {
            Key okey = (Key)other;
            return type == okey.type && (suiteId == okey.suiteId) &&
                ((query != null && query.equals(okey.query)) || query == okey.query);
        }
    }

    protected Map<Key, Stuff> _models = new HashMap<Key, Stuff>();

    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
}
