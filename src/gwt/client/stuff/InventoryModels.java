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

import client.shell.Args;
import client.util.ServiceUtil;
import client.util.events.FlashEvents;
import client.util.events.ItemUsageEvent;
import client.util.events.ItemUsageListener;

/**
 * Maintains information on our member's inventory.
 */
public class InventoryModels
    implements ItemUsageListener, ItemDataModel
{
    public static class Stuff extends SimpleDataModel<Item>
    {
        public final byte type;
        public final int suiteId;
        public final String query;

        public Stuff (List<Item> items, byte type, int suiteId, String query) {
            super(items);
            this.type = type;
            this.suiteId = suiteId;
            this.query = (query != null && query.length() == 0) ? null : query;
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
    public SimpleDataModel<Item> getModel (byte type, int suiteId, String query)
    {
        return _models.get(new Key(type, suiteId, query));
    }

    // from interface ItemDataModel
    public void loadModel (final byte type, final int suiteId, final String query,
                           final AsyncCallback<DataModel<Item>> cb)
    {
        final Key key = new Key(type, suiteId, query);
        Stuff model = _models.get(key);
        if (model != null) {
            cb.onSuccess(model);
            return;
        }

        _stuffsvc.loadInventory(type, suiteId, query, new AsyncCallback<List<Item>>() {
            public void onSuccess (List<Item> result) {
                Stuff model = new Stuff(result, type, suiteId, query);
                _models.put(key, model);
                cb.onSuccess(model);
            }
            public void onFailure (Throwable caught) {
                cb.onFailure(caught);
            }
        });
    }

    // from interface ItemDataModel
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
        for (Stuff model : _models.values()) {
            if (model.matches(item)) {
                model.updateItem(item);
            }
        }
    }

    // from interface ItemDataModel
    public void itemDeleted (Item item)
    {
        for (Stuff model : _models.values()) {
            if (model.matches(item)) {
                model.removeItem(item);
            }
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
            return type == okey.type && suiteId == okey.suiteId &&
                ((query != null && query.equals(okey.query)) || query == okey.query);
        }
    }

    protected Map<Key, Stuff> _models = new HashMap<Key, Stuff>();

    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
}
