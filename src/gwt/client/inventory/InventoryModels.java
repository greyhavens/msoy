//
// $Id$

package client.inventory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.Predicate;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;

/**
 * Maintains information on our member's inventory.
 */
public class InventoryModels
{
    public void loadModel (byte type, int suiteId, final AsyncCallback cb)
    {
        final Key key = new Key(type, suiteId);
        DataModel model = (DataModel)_models.get(key);
        if (model != null) {
            cb.onSuccess(model);
            return;
        }

        CInventory.membersvc.loadInventory(CInventory.ident, type, suiteId, new AsyncCallback() {
            public void onSuccess (Object result) {
                DataModel model = new SimpleDataModel((List)result);
                _models.put(key, model);
                cb.onSuccess(model);
            }
            public void onFailure (Throwable caught) {
                cb.onFailure(caught);
            }
        });
    }

    public Item findItem (byte type, final int itemId)
    {
        Predicate itemP = new Predicate() {
            public boolean isMatch (Object object) {
                return ((Item)object).itemId == itemId;
            }
        };
        for (Iterator iter = _models.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            Key key = (Key)entry.getKey();
            if (key.type == type) {
                SimpleDataModel model = (SimpleDataModel)entry.getValue();
                Item item = (Item)model.findItem(itemP);
                if (item != null) {
                    return item;
                }
            }
        }
        return null;
    }

    public DataModel getModel (byte type, int suiteId)
    {
        return (DataModel)_models.get(new Key(type, suiteId));
    }

    protected static class Key {
        public final byte type;
        public final int suiteId;

        public Key (byte type, int suiteId) {
            this.type = type;
            this.suiteId = suiteId;
        }

        public int hashCode() {
            return type ^ suiteId;
        }

        public boolean equals (Object other) {
            Key okey = (Key)other;
            return type == okey.type && suiteId == okey.suiteId;
        }
    }

    protected Map _models = new HashMap();
}
