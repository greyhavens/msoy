package client.stuff;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.SimpleDataModel;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListQuery;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.stuff.gwt.StuffService.DetailOrIdent;
import com.threerings.msoy.stuff.gwt.StuffService.ItemListResult;

import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * Provides a data model for a PagedGrid backed by a persistent item list. The item list is accessed
 * via the StuffService.
 *
 * @author mjensen
 */
public class ItemListDataModel extends ServiceBackedDataModel<Item, ItemListResult>
    implements ItemDataModel
{
    public ItemListDataModel ()
    {
        // by default, show all item types
        this(Item.NOT_A_TYPE);
    }

    public ItemListDataModel (byte itemType)
    {
        _query = new ItemListQuery();
        _query.itemType = itemType;
    }

    public void setListId (int listId)
    {
        // we can't display a list unless/until the list id is set
        _initialized = listId >= 0;

        _query.listId = listId;

        // make sure that the model reloads next time a fetch is called
        reset();
    }

    public void setItemType (byte itemType)
    {
        if (_query.itemType != itemType ) {
            _query.itemType = itemType;

            // make sure that the model reloads next time a fetch is called
            reset();
        }
    }

    /**
     * Sets whether the list should be sorted in descending order.
     */
    public void setDescending (boolean descending)
    {
        _query.descending = descending;
    }

    /**
     * Looks for the given item in the currently loaded items.
     */
    // from ItemDataModel
    public Item findItem (byte itemType, int itemId)
    {
        for (Item item : _pageItems) {
            if (item.itemId == itemId && item.getType() == itemType) {
                return item;
            }
        }
        return null;
    }

    /**
     * Gets the item type filter used by this model.
     */
    // from ItemDataModel
    public byte getDefaultItemType ()
    {
        return _query.itemType;
    }

    /**
     * This is called when the user has deleted an item.
     */
    // from ItemDataModel
    public void itemDeleted (Item item)
    {
        _pageItems.remove(item);
    }

    /**
     * This is called when the user has modified an item.
     */
    // from ItemDataModel
    public void itemUpdated (Item item)
    {
        for (int i = 0; i < _pageItems.size(); i++) {
            Item pageItem = _pageItems.get(i);
            if (pageItem.itemId == item.itemId && pageItem.getType() == item.getType()) {
                // replace the item
                _pageItems.set(i, item);
                return;
            }
        }
    }

    /**
     * This callback method is used to get the item details for a particular item.
     */
    // from ItemDataModel
    public void loadItemDetail (ItemIdent ident, AsyncCallback<DetailOrIdent> resultCallback)
    {
        _stuffsvc.loadItemDetail(ident, resultCallback);
    }

    /**
     * Used to load subtypes for
     */
    // from ItemDataModel
    public void loadModel (byte itemType, int suiteId, final AsyncCallback<DataModel<Item>> resultCallback)
    {
        _stuffsvc.loadInventory(itemType, suiteId, new AsyncCallback<List<Item>>() {
            public void onSuccess (List<Item> result) {
                SimpleDataModel<Item> model = new SimpleDataModel<Item>(result);
                resultCallback.onSuccess(model);
            }
            public void onFailure (Throwable caught) {
                resultCallback.onFailure(caught);
            }
        });
    }

    /**
     * Sets the item type and returns this model.
     */
    // from ItemDataModel
    public DataModel<Item> getGridModel (byte itemType)
    {
        setItemType(itemType);
        return this;
    }

    /**
     * Sends an item list query to the service using the given callback to collect the results.
     */
    @Override // from ServiceBackedDataModel
    protected void callFetchService (int start, int count, boolean needsCount, AsyncCallback<ItemListResult> callback)
    {
        if (!_initialized) {
            return;
        }
        _query.offset = start;
        _query.count = count;
        _query.needsCount = needsCount;
        _stuffsvc.loadItemList(_query, callback);
    }

    /**
     * Returns the total number of possible items returned (asynchronously) by the last service
     * fetch.
     *
     * @see client.util.ServiceBackedDataModel#getCount(java.lang.Object)
     */
    @Override
    protected int getCount (ItemListResult result)
    {
        return result.totalCount;
    }

    /**
     * Returns the list of item results from the last service fetch.
     *
     * @see client.util.ServiceBackedDataModel#getRows(java.lang.Object)
     */
    @Override
    protected List<Item> getRows (ItemListResult result)
    {
        return result.items;
    }

    protected ItemListQuery _query;

    protected boolean _initialized;

    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync) ServiceUtil.bind(GWT
        .create(StuffService.class), StuffService.ENTRY_POINT);
}
